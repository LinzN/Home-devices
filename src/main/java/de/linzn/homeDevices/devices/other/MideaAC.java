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
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class MideaAC extends MqttDevice {

    public Date lastACData;
    private Date healthSwitchDateRequest;
    private JSONObject acData;

    public MideaAC(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "midea2mqtt/" + deviceProfile.getDeviceHardAddress() + "/state");
        this.mqttModule.subscribe("midea2mqtt/" + deviceProfile.getDeviceHardAddress() + "/capabilities", this);
    }

    private void requestData(){
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        this.mqttModule.publish("midea2mqtt/" + this.getDeviceHardAddress() + "/get", mqttMessage);
    }

    private void writeData(JSONObject data){
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        mqttMessage.setPayload(data.toString().getBytes());
        this.mqttModule.publish("midea2mqtt/" + this.getDeviceHardAddress() + "/set", mqttMessage);
    }

    @Override
    protected void request_initial_status() {
        int counter = 0;
        while (!this.hasData() && counter <= 30) {
            this.requestData();
            STEMApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.MIDEA_AC.name() + ", " + this.getDeviceTechnology().name() + ")");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            counter++;
        }
        if (counter > 30) {
            STEMApp.LOGGER.WARNING("Seems device " + this.getDeviceHardAddress() + " is disconnected!");
        }
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
        if (this.lastACData != null) {
            return this.lastACData.getTime() >= this.healthSwitchDateRequest.getTime();
        } else {
            return false;
        }
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
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        if(jsonInput.has("power")) {
            this.writeData(jsonObject);
            jsonObject.put("status", "OK");
        } else {
            jsonObject.put("status", "ERROR");
        }
        return jsonObject;
    }

    @Override
    public void mqttMessageEvent(String topic, MqttMessage mqttMessage) {

        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        if(topic.endsWith("/state")){
            this.lastACData = new Date();
            this.acData = jsonPayload;
        } else if(topic.endsWith("/capabilities")){

        }
    }
}
