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
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RF433Device extends MqttDevice {

    private final AtomicBoolean isGarageTriggered;
    public Date lastData;

    public Date lastEventCallback;
    private Date healthSwitchDateRequest;
    private AtomicBoolean isGarageModuleConnected;
    private AtomicBoolean hasHeartbeat;
    private String rf433MQTT;

    public RF433Device(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/data");
        if (this.deviceProfile.getLoadedConfig().contains("custom.rf433MQTT")) {
            this.rf433MQTT = this.deviceProfile.getLoadedConfig().getString("custom.rf433MQTT");
        }
        this.isGarageTriggered = new AtomicBoolean(false);
        this.lastEventCallback = new Date(0);
    }

    @Override
    protected void request_initial_status() {
        STEMApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.DOORRING.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("heartbeat")) {
            this.hasHeartbeat = new AtomicBoolean(jsonPayload.getBoolean("heartbeat"));
        }

        if (jsonPayload.has("garageModuleConnected")) {
            this.isGarageModuleConnected = new AtomicBoolean(jsonPayload.getBoolean("garageModuleConnected"));
        }

        if (jsonPayload.has("garageEvent")) {
            this.lastEventCallback = new Date();
            STEMApp.LOGGER.INFO("Garage callback event received!");
            this.isGarageTriggered.set(true);
            STEMApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, () -> isGarageTriggered.set(false), 2, TimeUnit.SECONDS);
        }

        STEMApp.LOGGER.DEBUG("RF433 DATA: [heartbeat:" + this.hasHeartbeat.get() + ", garageModuleConnected:" + this.isGarageModuleConnected.get() + "]");
        this.lastData = new Date();
    }

    private boolean triggerGarageEvent() {
        if (this.rf433MQTT != null) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            JSONObject jsonObject = new JSONObject();
            mqttMessage.setPayload(jsonObject.toString().getBytes());
            this.mqttModule.publish(rf433MQTT, mqttMessage);
            return true;
        }
        return false;
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        if (!this.isGarageModuleConnected.get()) {
            return false;
        }
        return this.healthSwitchDateRequest.toInstant().minus(30, ChronoUnit.SECONDS).toEpochMilli() <= this.lastData.getTime();
    }

    @Override
    public boolean hasData() {
        return this.hasHeartbeat != null && this.isGarageModuleConnected != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("garageModuleConnected", this.isGarageModuleConnected.get());
        jsonObject.put("lastEventCallback", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(this.lastEventCallback));
        String status = "unknown";

        if (this.isGarageTriggered.get()) {
            status = "working";
        }
        jsonObject.put("garageModuleStatus", status);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        if (jsonInput.has("trigger_garage_event")) {
            if (this.triggerGarageEvent()) {
                jsonObject.put("status", "OK");
            } else {
                jsonObject.put("status", "ERROR");
            }
        }
        return jsonObject;
    }

}
