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
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.openJL.math.FloatingPoint;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.informationModule.InformationBlock;
import de.stem.stemSystem.modules.informationModule.InformationIntent;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class LEDDevice extends MqttDevice {
    public Date lastData;
    private Date healthSwitchDateRequest;
    private int mode = 0;
    private int r = 0;
    private int g = 0;
    private int b = 0;
    public LEDDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/data");
    }

    @Override
    protected void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.LED.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        this.lastData = new Date();
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        int currentMode = jsonPayload.getInt("currentMode");
        if(currentMode != this.mode){
            this.updateLED();
        }
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
        jsonObject.put("r", this.r);
        jsonObject.put("g", this.g);
        jsonObject.put("b", this.b);
        return jsonObject;
    }

    public void setLEDMode(int mode, int r, int g, int b){
        this.mode = mode;
        this.r = r;
        this.g = g;
        this.b = b;
        this.updateLED();
    }

    private void updateLED(){
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject setting = new JSONObject();

        setting.put("mode", this.mode);
        setting.put("r", this.r);
        setting.put("g", this.g);
        setting.put("b", this.b);
        mqttMessage.setPayload(setting.toString().getBytes());
        this.mqttModule.publish("cmnd/" + getDeviceHardAddress() + "/data", mqttMessage);
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        this.setLEDMode(jsonInput.getInt("mode"), jsonInput.getInt("r"), jsonInput.getInt("g"), jsonInput.getInt("b")) ;
        jsonObject.put("status", "ok");
        return jsonObject;
    }

}
