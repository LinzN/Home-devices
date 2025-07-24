/*
 * Copyright (C) 2021. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.homeDevices.devices.other;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.events.records.MQTTPrintEndEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.informationModule.InformationBlock;
import de.stem.stemSystem.modules.informationModule.InformationIntent;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public class AnkermakePrinter extends MqttDevice {

    public Date lastPrinterData;
    private JSONObject printData;
    private InformationBlock informationBlock;
    private String last_success_task_id = null;

    public AnkermakePrinter(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "ankermake2mqtt/printers/" + deviceProfile.getDeviceHardAddress() + "/data");
    }

    @Override
    protected void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.ANKERMAKE_PRINTER.name() + ") is not supported!");
    }


    @Override
    public void requestHealthCheck() {
    }

    @Override
    public boolean healthCheckStatus() {
        return true;
    }

    @Override
    public boolean hasData() {
        return true;
    }

    public String getPrinterStatus() {
        if (this.printData != null && this.lastPrinterData.toInstant().plus(15, ChronoUnit.SECONDS).toEpochMilli() >= new Date().toInstant().toEpochMilli()) {
            if (this.printData.has("1001")) {
                return "PRINTING";
            } else {
                return "IDLE";
            }
        } else {
            return "OFFLINE";
        }
    }


    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lastPrinterData", this.lastPrinterData);
        jsonObject.put("printerStatus", this.getPrinterStatus());
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
    public void mqttMessageEvent(MqttMessage mqttMessage) {

        String payload = new String(mqttMessage.getPayload());
        JSONArray jsonPayload = new JSONArray(payload);
        this.lastPrinterData = new Date();
        JSONObject printData = new JSONObject();
        for (int i = 0; i < jsonPayload.length(); i++) {
            JSONObject object = jsonPayload.getJSONObject(i);
            if (object.has("commandType")) {
                int commandType = object.getInt("commandType");
                switch (commandType) {
                    case 1000 -> {
                        printData.put("1000", true);
                        printData.put("event_type", object.getInt("subType"));
                        printData.put("event_value", object.getInt("value"));
                    }
                    case 1003 -> {
                        printData.put("1003", true);
                        printData.put("nozzle_temp_current", object.getInt("currentTemp"));
                        printData.put("nozzle_temp_target", object.getInt("currentTemp"));
                    }
                    case 1004 -> {
                        printData.put("1004", true);
                        printData.put("hotbed_temp_current", object.getInt("currentTemp"));
                        printData.put("hotbed_temp_target", object.getInt("currentTemp"));
                    }
                    case 1001 -> {
                        printData.put("1001", true);
                        if (object.has("task_id")) {
                            printData.put("task_id", object.getString("task_id"));
                        }
                        if (object.has("progress")) {
                            printData.put("print_progress", object.getInt("progress"));
                        } else {
                            printData.put("print_progress", -1);
                        }
                        if (object.has("name")) {
                            printData.put("print_name", object.getString("name"));
                        } else {
                            printData.put("print_name", "unknown");
                        }
                        if (object.has("realSpeed")) {
                            printData.put("print_speed", object.getInt("realSpeed"));
                        } else {
                            printData.put("print_speed", -1);
                        }
                    }
                }
            }
        }
        this.printData = printData;
        if (this.printData.has("1000")) {
            //STEMSystemApp.LOGGER.CORE("EVENT_TYPE: " + this.printData.getInt("event_type") + " EVENT_VALUE: " + this.printData.getInt("event_value"));
            if (this.printData.getInt("event_type") == 1 && this.printData.getInt("event_value") == 4) {
                if (this.last_success_task_id == null || !this.last_success_task_id.equalsIgnoreCase(this.printData.getString("task_id"))) {

                    this.last_success_task_id = this.printData.getString("task_id");

                    final MQTTPrintEndEvent mqttPrintEndEvent = new MQTTPrintEndEvent(this, this.printData.getInt("event_value"), this.printData.getString("print_name"));
                    STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(mqttPrintEndEvent);

                    if (this.informationBlock != null && this.informationBlock.isActive()) {
                        this.informationBlock.expire();
                        this.informationBlock = null;
                    }

                    this.informationBlock = new InformationBlock("3D Printer", "3D print job done for " + this.printData.getString("print_name"), HomeDevicesPlugin.homeDevicesPlugin, "3D print done for print job " + this.printData.getString("print_name") + " with exit value " + this.printData.getInt("event_value"));
                    this.informationBlock.setIcon("USV");
                    this.informationBlock.setExpireTime(TimeAdapter.getTimeInstant().plus(20, ChronoUnit.MINUTES));
                    this.informationBlock.addIntent(InformationIntent.NOTIFY_USER);
                    this.informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
                    STEMSystemApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
                }
            }
        }
    }

}
