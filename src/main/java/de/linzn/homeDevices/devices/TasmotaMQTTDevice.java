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

package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.AutoStartStopTimer;
import de.linzn.homeDevices.AutoSwitchOffTimer;
import de.linzn.homeDevices.DeviceCategory;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.events.TasmotaMQTTUpdateEvent;
import de.linzn.homeDevices.events.TasmotaSwitchEvent;
import de.linzn.homeDevices.events.TasmotaToggleEvent;
import de.linzn.homeDevices.events.TasmotaUpdateEvent;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TasmotaMQTTDevice implements IMqttMessageListener {

    public final String configName;
    public final String deviceHardAddress;
    public final String description;
    public final DeviceCategory deviceCategory;
    private final HomeDevicesPlugin homeDevicesPlugin;
    private final MqttModule mqttModule;
    private final AutoSwitchOffTimer autoSwitchOffTimer;
    private final AutoStartStopTimer autoStartStopTimer;
    public AtomicBoolean deviceStatus;
    public Date lastSwitch;

    public TasmotaMQTTDevice(HomeDevicesPlugin homeDevicesPlugin, String configName) {
        this.homeDevicesPlugin = homeDevicesPlugin;
        this.configName = configName.toLowerCase();
        this.deviceHardAddress = homeDevicesPlugin.getDefaultConfig().getString("tasmota." + configName + ".deviceHardAddress", configName.toLowerCase());
        this.deviceCategory = DeviceCategory.valueOf(homeDevicesPlugin.getDefaultConfig().getString("tasmota." + configName + ".category", DeviceCategory.OTHER.name()));
        this.description = homeDevicesPlugin.getDefaultConfig().getString("tasmota." + configName + ".description", "No description");
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe("stat/" + this.deviceHardAddress + "/RESULT", this);
        this.autoSwitchOffTimer = new AutoSwitchOffTimer(this);
        this.autoStartStopTimer = new AutoStartStopTimer(this);
        STEMSystemApp.LOGGER.CONFIG("Register new mqtt tasmota device with configName: " + this.configName + ", hardId: " + this.deviceHardAddress + " and category: " + this.deviceCategory.name());
        STEMSystemApp.LOGGER.CONFIG("Description: " + description);
        STEMSystemApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, autoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, autoStartStopTimer, 2, TimeUnit.SECONDS);
    }

    public String getConfigName() {
        return configName;
    }

    public String getDeviceHardAddress() {
        return this.deviceHardAddress;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean getDeviceStatus() {
        return this.deviceStatus.get();
    }

    public DeviceCategory getDeviceCategory() {
        return deviceCategory;
    }

    public void switchDevice(boolean status) {
        TasmotaSwitchEvent tasmotaSwitchEvent = new TasmotaSwitchEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(tasmotaSwitchEvent);

        if (!tasmotaSwitchEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            if (status) {
                mqttMessage.setPayload("ON".getBytes());
            } else {
                mqttMessage.setPayload("OFF".getBytes());
            }
            this.mqttModule.publish("cmnd/" + this.deviceHardAddress + "/Power", mqttMessage);
        }
    }

    public void toggleDevice() {
        TasmotaToggleEvent tasmotaToggleEvent = new TasmotaToggleEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(tasmotaToggleEvent);

        if (!tasmotaToggleEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            mqttMessage.setPayload("TOGGLE".getBytes());
            this.mqttModule.publish("cmnd/" + this.deviceHardAddress + "/Power", mqttMessage);
        }
    }

    private void update_status(boolean newStatus) {
        if (this.deviceStatus == null) {
            this.deviceStatus = new AtomicBoolean(newStatus);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " status: " + this.deviceStatus);
        } else {
            this.deviceStatus.set(newStatus);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " status: " + this.deviceStatus);
        }
        this.lastSwitch = new Date();
        TasmotaUpdateEvent tasmotaUpdateEvent = new TasmotaUpdateEvent(this, newStatus);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(tasmotaUpdateEvent);

        DeviceWrapperListener.updateStatus(this.configName, this.deviceStatus.get());
    }

    private void request_initial_status() {
        while (this.deviceStatus == null) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + this.deviceHardAddress + "/Power", mqttMessage);
            STEMSystemApp.LOGGER.INFO("MQTT initialization request for hardId: " + this.deviceHardAddress + " configName: " + this.configName);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }


    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        boolean status = jsonPayload.getString("POWER").equalsIgnoreCase("ON");
        final TasmotaMQTTUpdateEvent tasmotaMQTTUpdateEvent = new TasmotaMQTTUpdateEvent(this, status);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(tasmotaMQTTUpdateEvent);
        this.update_status(status);
    }

}
