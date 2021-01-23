/*
 * Copyright (C) 2020. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.DeviceCategory;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class TasmotaMQTTDevice implements IMqttMessageListener {

    private final String deviceId;
    private final DeviceCategory deviceCategory;
    private final MqttModule mqttModule;
    private AtomicBoolean deviceStatus;

    public TasmotaMQTTDevice(String deviceId, DeviceCategory deviceCategory) {
        this.deviceId = deviceId.toLowerCase();
        this.deviceCategory = deviceCategory;
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe("stat/" + this.deviceId + "/RESULT", this);
        STEMSystemApp.LOGGER.INFO("Register new mqtt tasmota device with id: " + this.deviceId + " and category: " + this.deviceCategory.name());
        this.request_initial_status();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean getDeviceStatus() {
        return this.deviceStatus.get();
    }

    public DeviceCategory getDeviceCategory() {
        return deviceCategory;
    }

    public void switchDevice(boolean status) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        if (status) {
            mqttMessage.setPayload("ON".getBytes());
        } else {
            mqttMessage.setPayload("OFF".getBytes());
        }
        this.mqttModule.publish("cmnd/" + this.deviceId + "/Power", mqttMessage);
    }

    public void toggleDevice() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        mqttMessage.setPayload("TOGGLE".getBytes());
        this.mqttModule.publish("cmnd/" + this.deviceId + "/Power", mqttMessage);
    }

    private void update_status(boolean newStatus) {
        if (this.deviceStatus == null) {
            this.deviceStatus = new AtomicBoolean(newStatus);
            STEMSystemApp.LOGGER.INFO("MQTT initialization device: " + this.deviceId + " status: " + this.deviceStatus);
        } else {
            this.deviceStatus.set(newStatus);
            STEMSystemApp.LOGGER.INFO("Update Device: " + this.deviceId + " status: " + this.deviceStatus);
        }
    }

    private void request_initial_status() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        this.mqttModule.publish("cmnd/" + this.deviceId + "/Power", mqttMessage);
        STEMSystemApp.LOGGER.INFO("MQTT initialization request for device: " + this.deviceId);
    }


    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        boolean status = jsonPayload.getString("POWER").equalsIgnoreCase("ON");
        this.update_status(status);
    }

}
