/*
 * Copyright (c) 2026 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */

package de.linzn.homeDevices.devices.other;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.events.records.MQTTSnapmakerPrintEndEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.informationModule.InformationBlock;
import de.linzn.stem.modules.informationModule.InformationIntent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Snapmaker U1 connected through snapmaker2mqtt (https://github.com/LinzN/snapmaker2mqtt).
 *
 * <p>The device's {@code hardwareAddress} is the printer name from the bridge's config.yml.
 * Topics used, with {@code <name>} being that printer name:
 * <pre>
 * snapmaker2mqtt/bridge/status             online / offline of the bridge (health check)
 * snapmaker2mqtt/&lt;name&gt;/availability     online / offline of the printer
 * snapmaker2mqtt/&lt;name&gt;/state            full printer state as JSON
 * snapmaker2mqtt/&lt;name&gt;/event/print_*    job end events
 * </pre>
 * Read only, like {@link AnkermakePrinter}: nothing is ever published to the bridge, so the
 * printer cannot be controlled from STEM.
 */
public class SnapmakerPrinter extends MqttDevice {

    private static final String BASE_TOPIC = "snapmaker2mqtt";

    public Date lastPrinterData;
    private JSONObject printData;
    private boolean printerOnline;
    private boolean bridgeOnline;
    private InformationBlock informationBlock;
    private String lastEndEventTimestamp;

    public SnapmakerPrinter(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, BASE_TOPIC + "/" + deviceProfile.getDeviceHardAddress() + "/state");
        String printerTopic = BASE_TOPIC + "/" + deviceProfile.getDeviceHardAddress();
        this.mqttModule.subscribe(printerTopic + "/availability", this);
        this.mqttModule.subscribe(printerTopic + "/event/print_finished", this);
        this.mqttModule.subscribe(printerTopic + "/event/print_failed", this);
        this.mqttModule.subscribe(printerTopic + "/event/print_cancelled", this);
        this.mqttModule.subscribe(BASE_TOPIC + "/bridge/status", this);
    }

    @Override
    protected void request_initial_status() {
        // read only: state and availability are retained by the bridge and arrive with the subscription
        STEMApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SNAPMAKER_PRINTER.name() + ") is not supported, waiting for retained data");
    }

    @Override
    public void requestHealthCheck() {
    }

    /**
     * The printer itself is often switched off, so only the bridge is checked, from its retained
     * {@code bridge/status} topic (its MQTT last will).
     */
    @Override
    public boolean healthCheckStatus() {
        return this.bridgeOnline;
    }

    @Override
    public boolean hasData() {
        return this.printData != null;
    }

    /**
     * @return PRINTING, PAUSED, IDLE or OFFLINE
     */
    public String getPrinterStatus() {
        if (!this.printerOnline || this.printData == null) {
            return "OFFLINE";
        }
        return switch (this.printData.optString("printState", "standby")) {
            case "printing" -> "PRINTING";
            case "paused" -> "PAUSED";
            default -> "IDLE";
        };
    }

    /**
     * @return progress of the current job in percent, -1 if unknown
     */
    public double getPrintProgress() {
        return this.printData == null ? -1 : this.printData.optDouble("progress", -1);
    }

    public String getPrintName() {
        return this.printData == null ? "unknown" : this.printData.optString("filename", "unknown");
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lastPrinterData", this.lastPrinterData);
        jsonObject.put("printerStatus", this.getPrinterStatus());
        jsonObject.put("printerOnline", this.printerOnline);
        jsonObject.put("bridgeOnline", this.bridgeOnline);
        jsonObject.put("printerData", this.printData);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

    @Override
    public void mqttMessageEvent(String topic, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());

        if (topic.endsWith("/bridge/status")) {
            this.bridgeOnline = payload.equalsIgnoreCase("online");
        } else if (topic.endsWith("/availability")) {
            boolean online = payload.equalsIgnoreCase("online");
            if (online != this.printerOnline) {
                STEMApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + this.getConfigName() + " DeviceHardAddress: " + this.getDeviceHardAddress() + " printer " + (online ? "online" : "offline"));
            }
            this.printerOnline = online;
        } else if (topic.endsWith("/state")) {
            JSONObject state = new JSONObject(payload);
            this.lastPrinterData = new Date();
            this.printerOnline = state.optBoolean("available", true);
            this.printData = state;
        } else if (topic.contains("/event/print_")) {
            this.onPrintEnd(new JSONObject(payload));
        }
    }

    private void onPrintEnd(JSONObject event) {
        // events are not retained, but guard against the same event delivered twice
        String timestamp = event.optString("timestamp", null);
        if (timestamp != null && timestamp.equals(this.lastEndEventTimestamp)) {
            return;
        }
        this.lastEndEventTimestamp = timestamp;

        String eventType = event.optString("event");
        String printName = event.optString("filename", "unknown");
        long duration = event.optLong("printDuration", 0);

        final MQTTSnapmakerPrintEndEvent printEndEvent = new MQTTSnapmakerPrintEndEvent(this, eventType, printName, duration);
        STEMApp.getInstance().getEventModule().getStemEventBus().fireEvent(printEndEvent);

        String title;
        String text;
        switch (eventType) {
            case "print_finished" -> {
                title = "3D print job done for " + printName;
                text = "3D print done for print job " + printName + " after " + formatDuration(duration);
            }
            case "print_failed" -> {
                String message = event.optString("message", "unknown error");
                title = "3D print job failed for " + printName;
                text = "3D print job " + printName + " failed: " + message;
            }
            default -> {
                // cancelled by the user, the event is enough
                return;
            }
        }

        if (this.informationBlock != null && this.informationBlock.isActive()) {
            this.informationBlock.expire();
            this.informationBlock = null;
        }

        this.informationBlock = new InformationBlock("3D Printer", title, HomeDevicesPlugin.homeDevicesPlugin, text);
        this.informationBlock.setIcon("USV");
        this.informationBlock.setExpireTime(TimeAdapter.getTimeInstant().plus(20, ChronoUnit.MINUTES));
        this.informationBlock.addIntent(InformationIntent.NOTIFY_USER);
        this.informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
        STEMApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours > 0 ? hours + " h " + minutes + " min" : minutes + " min";
    }
}
