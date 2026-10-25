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

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.events.records.MQTTDoorRingEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.stem.modules.informationModule.InformationBlock;
import de.linzn.stem.modules.informationModule.InformationIntent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DoorRingDevice extends MqttDevice {

    private final String rf433MQTT;
    private final String rf433CodeWord;
    public volatile Date lastData;
    private volatile Date lastEvent;
    private volatile Date healthSwitchDateRequest;

    public DoorRingDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/nanoCLR/" + deviceProfile.getDeviceHardAddress() + "/data");
        this.rf433MQTT = this.deviceProfile.getLoadedConfig().getString("custom.rf433MQTT", null);
        this.rf433CodeWord = this.deviceProfile.getLoadedConfig().getString("custom.codeWord", null);
    }

    private void sendRF433MQTT() {
        if (this.rf433MQTT != null && this.rf433CodeWord != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", this.rf433CodeWord);
            this.publish(this.rf433MQTT, jsonObject.toString());
        }
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.DOORRING.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        /* Update heartbeat date */
        if (jsonPayload.has("device")) {
            this.lastData = new Date();
        }
        /* Trigger if event fired */
        if (jsonPayload.has("event")) {
            this.lastEvent = new Date();
            this.getLogger().info("DeviceUpdate - ConfigName: " + getDeviceProfile().getName() + " DeviceHardAddress: " + getDeviceProfile().getDeviceHardAddress());
            this.fireEvent(new MQTTDoorRingEvent(this));
            this.sendRF433MQTT();
            InformationBlock informationBlock = new InformationBlock("DOOR", "Door Ring sensor", this.getHomeDevicesPlugin(), "Door Ring pushed");
            informationBlock.setExpireTime(TimeAdapter.getTimeInstant().plus(2, ChronoUnit.HOURS));
            informationBlock.setIcon("DOOR");
            informationBlock.addIntent(InformationIntent.NOTIFY_USER);
            informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
            this.getHomeDevicesPlugin().getStemApi().information().queue(informationBlock);
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastData, this.healthSwitchDateRequest, Duration.ofSeconds(30));
    }

    @Override
    public boolean hasData() {
        return this.lastData != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        Date event = this.lastEvent;
        jsonObject.put("lastEvent", event != null ? event.getTime() : -1);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        return notSupported();
    }
}
