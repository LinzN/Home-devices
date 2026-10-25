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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RF433Device extends MqttDevice {

    private final AtomicBoolean isGarageTriggered;
    private final String rf433MQTT;
    public volatile Date lastData;
    public volatile Date lastEventCallback;
    private volatile Date healthSwitchDateRequest;
    private volatile Boolean isGarageModuleConnected;
    private volatile Boolean hasHeartbeat;

    public RF433Device(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/data");
        this.rf433MQTT = this.deviceProfile.getLoadedConfig().getString("custom.rf433MQTT", null);
        this.isGarageTriggered = new AtomicBoolean(false);
        this.lastEventCallback = new Date(0);
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.RF433.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        if (jsonPayload.has("heartbeat")) {
            this.hasHeartbeat = jsonPayload.getBoolean("heartbeat");
        }
        if (jsonPayload.has("garageModuleConnected")) {
            this.isGarageModuleConnected = jsonPayload.getBoolean("garageModuleConnected");
        }
        if (jsonPayload.has("garageEvent")) {
            this.lastEventCallback = new Date();
            this.getLogger().info("Garage callback event received!");
            this.isGarageTriggered.set(true);
            this.getHomeDevicesPlugin().getStemApi().scheduler().runLater(() -> isGarageTriggered.set(false), 2, TimeUnit.SECONDS);
        }
        this.getLogger().debug("RF433 DATA: [heartbeat:" + this.hasHeartbeat + ", garageModuleConnected:" + this.isGarageModuleConnected + "]");
        this.lastData = new Date();
    }

    private boolean triggerGarageEvent() {
        if (this.rf433MQTT != null) {
            return this.publish(this.rf433MQTT, new JSONObject().toString());
        }
        return false;
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        if (!Boolean.TRUE.equals(this.isGarageModuleConnected)) {
            return false;
        }
        return receivedSince(this.lastData, this.healthSwitchDateRequest, Duration.ofSeconds(30));
    }

    @Override
    public boolean hasData() {
        return this.hasHeartbeat != null && this.isGarageModuleConnected != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("garageModuleConnected", Boolean.TRUE.equals(this.isGarageModuleConnected));
        jsonObject.put("lastEventCallback", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(this.lastEventCallback));
        jsonObject.put("garageModuleStatus", this.isGarageTriggered.get() ? "working" : "unknown");
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "ERROR");
        if (jsonInput.has("trigger_garage_event")) {
            jsonObject.put("status", this.triggerGarageEvent() ? "OK" : "ERROR");
        }
        return jsonObject;
    }
}
