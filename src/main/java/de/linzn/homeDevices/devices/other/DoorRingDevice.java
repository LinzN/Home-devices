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
import de.linzn.homeDevices.events.MQTTDoorRingEvent;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import de.stem.stemSystem.modules.notificationModule.NotificationPriority;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class DoorRingDevice implements IMqttMessageListener {

    public final String deviceHardAddress;
    private final HomeDevicesPlugin homeDevicesPlugin;
    private final MqttModule mqttModule;
    public AtomicBoolean deviceLock = new AtomicBoolean(false);

    public DoorRingDevice(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
        this.deviceHardAddress = homeDevicesPlugin.getDefaultConfig().getString("doorRing.deviceHardAddress", "tasmota_xxxxxx");
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe("stat/" + this.deviceHardAddress + "/RESULT", this);
        STEMSystemApp.LOGGER.CONFIG("Register doorRing device with hardId: " + this.deviceHardAddress);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        boolean status = jsonPayload.getString("POWER").equalsIgnoreCase("ON");

        if (!this.deviceLock.get() && status) {
            final MQTTDoorRingEvent mqttDoorRingEvent = new MQTTDoorRingEvent(this);
            STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(mqttDoorRingEvent);
            STEMSystemApp.LOGGER.INFO("Door ring");
            STEMSystemApp.getInstance().getNotificationModule().pushNotification("Door Ring activated", NotificationPriority.ASAP);
        }
        this.deviceLock.set(status);
    }

}
