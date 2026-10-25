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
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.util.Date;

public class MideaAC extends MqttDevice {

    public volatile Date lastACData;
    private volatile Date healthSwitchDateRequest;
    private volatile JSONObject acData;
    private volatile JSONObject capabilities;

    public MideaAC(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "midea2mqtt/" + deviceProfile.getDeviceHardAddress() + "/state");
        this.addTopic("midea2mqtt/" + deviceProfile.getDeviceHardAddress() + "/capabilities");
    }

    private void requestData() {
        this.publish("midea2mqtt/" + this.getDeviceHardAddress() + "/get", "");
    }

    private void writeData(JSONObject data) {
        this.publish("midea2mqtt/" + this.getDeviceHardAddress() + "/set", data.toString());
    }

    @Override
    protected void request_initial_status() {
        this.requestUntilData(this::requestData, MqttDeviceCategory.MIDEA_AC.name());
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
        if (this.acData != null) {
            this.requestData();
        }
    }

    @Override
    public boolean healthCheckStatus() {
        Date last = this.lastACData;
        Date request = this.healthSwitchDateRequest;
        return last != null && request != null && last.getTime() >= request.getTime();
    }

    @Override
    public boolean hasData() {
        return this.acData != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lastACData", this.lastACData);
        jsonObject.put("acData", this.acData);
        if (this.capabilities != null) {
            jsonObject.put("capabilities", this.capabilities);
        }
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        JSONObject commands = jsonInput.optJSONObject("commands");
        if (commands != null) {
            this.writeData(commands);
            jsonObject.put("status", "OK");
        } else {
            jsonObject.put("status", "ERROR");
        }
        return jsonObject;
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        if (topic.endsWith("/state")) {
            this.lastACData = new Date();
            JSONObject previous = this.acData;
            if (previous != null && previous.optBoolean("power") != jsonPayload.optBoolean("power")) {
                this.getLogger().info("DeviceSwitchUpdate - ConfigName: " + this.getConfigName() + " DeviceHardAddress: " + this.getDeviceHardAddress());
            }
            this.acData = jsonPayload;
        } else if (topic.endsWith("/capabilities")) {
            this.capabilities = jsonPayload;
        }
    }
}
