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
import java.util.concurrent.atomic.AtomicBoolean;

public class DoorRingDevice extends MqttDevice {

    public AtomicBoolean deviceLock = new AtomicBoolean(false);

    public DoorRingDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/RESULT");
    }

    @Override
    protected void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.DOORRING.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        boolean status = jsonPayload.getString("POWER").equalsIgnoreCase("ON");

        if (!this.deviceLock.get() && status) {
            STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getDeviceProfile().getName() + " DeviceHardAddress: " + getDeviceProfile().getDeviceHardAddress());
            final MQTTDoorRingEvent mqttDoorRingEvent = new MQTTDoorRingEvent(this);
            STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(mqttDoorRingEvent);
            STEMSystemApp.LOGGER.INFO("DATA: [doorring:" + status + "]");

            InformationBlock informationBlock = new InformationBlock("Door", "Door Ring activated", HomeDevicesPlugin.homeDevicesPlugin);
            Instant expireDate = TimeAdapter.getTimeInstant().plus(2, ChronoUnit.HOURS);
            informationBlock.setExpireTime(expireDate);
            informationBlock.setIcon("DOOR");
            STEMSystemApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
        }
        this.deviceLock.set(status);
    }

    @Override
    public void requestHealthCheck() {
        //not supported
    }

    @Override
    public boolean healthCheckStatus() {
        return true;
    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

}
