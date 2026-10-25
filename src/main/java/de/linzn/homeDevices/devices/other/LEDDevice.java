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

import java.time.Duration;
import java.util.Date;

public class LEDDevice extends MqttDevice {

    public volatile Date lastData;
    private volatile Date healthSwitchDateRequest;
    /* mode and color are only changed together, guarded by this */
    private int mode = 0;
    private int r = 0;
    private int g = 0;
    private int b = 0;

    public LEDDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/data");
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.LED.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        this.lastData = new Date();
        JSONObject jsonPayload = new JSONObject(payload);
        if (!jsonPayload.has("currentMode")) {
            return;
        }
        int currentMode = jsonPayload.getInt("currentMode");
        synchronized (this) {
            if (currentMode != this.mode) {
                /* the device lost its state (e.g. after a reboot), send it again */
                this.updateLED();
            }
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastData, this.healthSwitchDateRequest, Duration.ofMinutes(1));
    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public synchronized JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mode", this.mode);
        jsonObject.put("r", this.r);
        jsonObject.put("g", this.g);
        jsonObject.put("b", this.b);
        return jsonObject;
    }

    public synchronized void setLEDMode(int mode, int r, int g, int b) {
        this.mode = mode;
        this.r = clampColor(r);
        this.g = clampColor(g);
        this.b = clampColor(b);
        this.updateLED();
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /* caller holds the lock */
    private void updateLED() {
        JSONObject setting = new JSONObject();
        setting.put("mode", this.mode);
        setting.put("r", this.r);
        setting.put("g", this.g);
        setting.put("b", this.b);
        this.publish("cmnd/" + getDeviceHardAddress() + "/data", setting.toString());
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        if (!jsonInput.has("mode")) {
            jsonObject.put("status", "error");
            jsonObject.put("error", "mode is missing");
            return jsonObject;
        }
        this.setLEDMode(jsonInput.getInt("mode"), jsonInput.optInt("r", 0), jsonInput.optInt("g", 0), jsonInput.optInt("b", 0));
        jsonObject.put("status", "ok");
        return jsonObject;
    }
}
