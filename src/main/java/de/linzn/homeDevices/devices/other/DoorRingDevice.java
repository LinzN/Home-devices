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
import de.linzn.homeDevices.events.MQTTDoorRingEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.informationModule.InformationBlock;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DoorRingDevice extends MqttDevice {

    public Date lastData;
    private Date lastEvent;
    private Date healthSwitchDateRequest;
    private String rf433MQTT;
    private String rf433CodeWord;

    public DoorRingDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/data");
        if (this.deviceProfile.getLoadedConfig().contains("custom.rf433MQTT")) {
            this.rf433MQTT = this.deviceProfile.getLoadedConfig().getString("custom.rf433MQTT");
        }
        if (this.deviceProfile.getLoadedConfig().contains("custom.codeWord")) {
            this.rf433CodeWord = this.deviceProfile.getLoadedConfig().getString("custom.codeWord");
        }
    }

    private void sendRF433MQTT() {
        if(this.rf433MQTT != null && this.rf433CodeWord != null) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", this.rf433CodeWord);

            mqttMessage.setPayload(jsonObject.toString().getBytes());
            this.mqttModule.publish(rf433MQTT, mqttMessage);
        }
    }

    @Override
    protected void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.DOORRING.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);

        /* Update heartbeat date */
        if (jsonPayload.has("heartbeat")) {
            this.lastData = new Date();
        }

        /* Trigger if event fired */
        if (jsonPayload.has("event")) {
            this.lastEvent = new Date();
            STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getDeviceProfile().getName() + " DeviceHardAddress: " + getDeviceProfile().getDeviceHardAddress());
            final MQTTDoorRingEvent mqttDoorRingEvent = new MQTTDoorRingEvent(this);
            STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(mqttDoorRingEvent);

            this.sendRF433MQTT();

            InformationBlock informationBlock = new InformationBlock("Door", "Door Ring activated", HomeDevicesPlugin.homeDevicesPlugin);
            Instant expireDate = TimeAdapter.getTimeInstant().plus(2, ChronoUnit.HOURS);
            informationBlock.setExpireTime(expireDate);
            informationBlock.setIcon("DOOR");
            STEMSystemApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(30, ChronoUnit.SECONDS).toEpochMilli() <= this.lastData.getTime();
    }

    @Override
    public boolean hasData() {
        return this.lastData != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lastEvent", lastEvent.getTime());
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

}
