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

package de.linzn.homeDevices.devices.other.nanoCLR;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class KeyBoardDevice extends MqttDevice implements Runnable {
    public Date lastData;

    public Date lastModeSwitch;
    private Date healthSwitchDateRequest;
    private int mode = -1;
    private boolean hasPendingStandbyRequest;

    public KeyBoardDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/nanoCLR/" + deviceProfile.getDeviceHardAddress() + "/data");
        this.hasPendingStandbyRequest = false;
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(stemPlugin, this, 30, 3, TimeUnit.SECONDS);
    }

    @Override
    protected void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.LED.name() + ") is not supported!");
        this.sendMqttModeUpdate("startUp");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        this.lastData = new Date();
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        int value = jsonPayload.getBoolean("value") ? 1 : 0;

        if (this.mode != value) {
            this.lastModeSwitch = new Date();
            this.mode = value;
            if (this.mode == 0) {
                this.hasPendingStandbyRequest = true;
                this.sendMqttModeUpdate("penningLogout");
            } else {
                this.hasPendingStandbyRequest = false;
                HomeDevicesPlugin.homeDevicesPlugin.getProfileController().requestProfileChange(SmartHomeProfile.DEFAULT);
                this.sendMqttModeUpdate("loggedIn");
            }
        }
    }

    private void sendMqttModeUpdate(String modeName) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject data = new JSONObject();
        data.put("command", "led");
        data.put("mode", modeName);
        mqttMessage.setPayload(data.toString().getBytes());
        this.mqttModule.publish("cmnd/nanoCLR/" + getDeviceHardAddress() + "/data", mqttMessage);
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(1, ChronoUnit.MINUTES).toEpochMilli() <= this.lastData.getTime();
    }

    @Override
    public boolean hasData() {
        return this.mode != -1;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

    @Override
    public void run() {
        if (this.hasPendingStandbyRequest) {
            if (this.lastModeSwitch != null) {
                if (this.lastModeSwitch.toInstant().plus(5, ChronoUnit.MINUTES).toEpochMilli() <= new Date().getTime()) {
                    this.hasPendingStandbyRequest = false;
                    HomeDevicesPlugin.homeDevicesPlugin.getProfileController().requestProfileChange(SmartHomeProfile.STANDBY);
                    sendMqttModeUpdate("loggedOut");
                }
            }
        }
    }
}
