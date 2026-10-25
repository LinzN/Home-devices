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

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.events.records.MQTTPrintEndEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.stem.modules.informationModule.InformationBlock;
import de.linzn.stem.modules.informationModule.InformationIntent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public class AnkermakePrinter extends MqttDevice {

    public volatile Date lastPrinterData;
    private volatile JSONObject printData;
    /* guarded by this */
    private InformationBlock informationBlock;
    private String last_success_task_id = null;
    /* the end event does not always carry the job data, so the last known job is remembered */
    private String lastTaskId = null;
    private String lastPrintName = "unknown";

    public AnkermakePrinter(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "ankermake2mqtt/printers/" + deviceProfile.getDeviceHardAddress() + "/data");
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.ANKERMAKE_PRINTER.name() + ") is not supported!");
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
        JSONObject data = this.printData;
        Date last = this.lastPrinterData;
        if (data != null && last != null && last.toInstant().plus(15, ChronoUnit.SECONDS).toEpochMilli() >= System.currentTimeMillis()) {
            return data.has("1001") ? "PRINTING" : "IDLE";
        }
        return "OFFLINE";
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
        return notSupported();
    }

    @Override
    public synchronized void mqttMessageEvent(String topic, String payload) {
        JSONArray jsonPayload = new JSONArray(payload);
        JSONObject data = new JSONObject();
        for (int i = 0; i < jsonPayload.length(); i++) {
            JSONObject object = jsonPayload.optJSONObject(i);
            if (object == null || !object.has("commandType")) {
                continue;
            }
            switch (object.getInt("commandType")) {
                case 1000 -> {
                    data.put("1000", true);
                    data.put("event_type", object.optInt("subType", -1));
                    data.put("event_value", object.optInt("value", -1));
                }
                case 1003 -> {
                    data.put("1003", true);
                    data.put("nozzle_temp_current", object.optInt("currentTemp"));
                    data.put("nozzle_temp_target", object.optInt("targetTemp", object.optInt("currentTemp")));
                }
                case 1004 -> {
                    data.put("1004", true);
                    data.put("hotbed_temp_current", object.optInt("currentTemp"));
                    data.put("hotbed_temp_target", object.optInt("targetTemp", object.optInt("currentTemp")));
                }
                case 1001 -> {
                    data.put("1001", true);
                    if (object.has("task_id")) {
                        this.lastTaskId = object.getString("task_id");
                        data.put("task_id", this.lastTaskId);
                    }
                    data.put("print_progress", object.optInt("progress", -1));
                    if (object.has("name")) {
                        this.lastPrintName = object.getString("name");
                    }
                    data.put("print_name", object.optString("name", "unknown"));
                    data.put("print_speed", object.optInt("realSpeed", -1));
                }
                default -> {
                }
            }
        }
        this.lastPrinterData = new Date();
        this.printData = data;

        if (data.has("1000") && data.getInt("event_type") == 1 && data.getInt("event_value") == 4) {
            String taskId = data.optString("task_id", this.lastTaskId);
            if (taskId != null && taskId.equalsIgnoreCase(this.last_success_task_id)) {
                return;
            }
            this.last_success_task_id = taskId;
            String printName = data.has("print_name") ? data.getString("print_name") : this.lastPrintName;
            int eventValue = data.getInt("event_value");
            this.fireEvent(new MQTTPrintEndEvent(this, eventValue, printName));
            if (this.informationBlock != null && this.informationBlock.isActive()) {
                this.informationBlock.expire();
            }
            this.informationBlock = new InformationBlock("3D Printer", "3D print job done for " + printName, this.getHomeDevicesPlugin(), "3D print done for print job " + printName + " with exit value " + eventValue);
            this.informationBlock.setIcon("USV");
            this.informationBlock.setExpireTime(TimeAdapter.getTimeInstant().plus(20, ChronoUnit.MINUTES));
            this.informationBlock.addIntent(InformationIntent.NOTIFY_USER);
            this.informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
            this.getHomeDevicesPlugin().getStemApi().information().queue(this.informationBlock);
        }
    }
}
