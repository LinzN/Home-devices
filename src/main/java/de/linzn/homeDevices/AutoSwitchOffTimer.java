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

package de.linzn.homeDevices;

import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.AutoSwitchOffTimerEvent;
import de.stem.stemSystem.STEMSystemApp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class AutoSwitchOffTimer implements Runnable {

    private final MqttSwitch mqttSwitch;
    private final long autoSwitchOffTimer;
    private final boolean autoSwitchEnabled;

    private final LocalTime startTime;
    private final LocalTime stopTime;

    public AutoSwitchOffTimer(MqttSwitch mqttSwitch) {
        this.mqttSwitch = mqttSwitch;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm");

        String configPath = "mqttDevices." + mqttSwitch.getConfigName() + ".options";
        if (HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().contains(configPath + ".autoModeSwitchOffSettings")) {
            this.autoSwitchEnabled = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean(configPath + ".autoModeSwitchOffSettings.autoSwitchOffEnabled");
            this.autoSwitchOffTimer = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getInt(configPath + ".autoModeSwitchOffSettings.autoSwitchOffAfterSeconds") * 1000L;
            this.startTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString(configPath + ".autoModeSwitchOffSettings.startTime"), dateTimeFormatter);
            this.stopTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString(configPath + ".autoModeSwitchOffSettings.stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.CONFIG("Load specific autoSwitchOff settings for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else {
            this.autoSwitchEnabled = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean("category." + mqttSwitch.getSwitchCategory().name() + ".autoSwitchOffEnabled");
            this.autoSwitchOffTimer = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getInt("category." + mqttSwitch.getSwitchCategory().name() + ".autoSwitchOffAfterSeconds") * 1000L;
            this.startTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("category." + mqttSwitch.getSwitchCategory().name() + ".startTime"), dateTimeFormatter);
            this.stopTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("category." + mqttSwitch.getSwitchCategory().name() + ".stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.CONFIG("No autoSwitchOff settings found for hardId " + mqttSwitch.getDeviceHardAddress() + " configName  " + mqttSwitch.getConfigName());
            STEMSystemApp.LOGGER.CONFIG("Load default settings from category " + mqttSwitch.getSwitchCategory().name());
        }

        if (this.autoSwitchEnabled && this.startTime.equals(this.stopTime)) {
            STEMSystemApp.LOGGER.ERROR("Start and stop are the same LocalTime! This is useless!");
        }
    }

    private boolean canSwitchOff(long lastSwitch) {
        if (this.autoSwitchEnabled) {
            return isInTimeRange() && (lastSwitch + autoSwitchOffTimer < new Date().getTime());
        } else {
            return false;
        }
    }

    private boolean isInTimeRange() {
        if (this.stopTime.isBefore(this.startTime)) {
            return this.startTime.isBefore(LocalTime.now()) || this.stopTime.isAfter(LocalTime.now());
        } else {
            return this.startTime.isBefore(LocalTime.now()) && this.stopTime.isAfter(LocalTime.now());
        }
    }

    private int getAutoSwitchOffTimerInSeconds() {
        return (int) (autoSwitchOffTimer / 1000);
    }

    @Override
    public void run() {
        if (HomeDevicesPlugin.homeDevicesPlugin.isCategoryInAutoSwitchOffMode(this.mqttSwitch.getSwitchCategory())) {
            if (this.mqttSwitch.deviceStatus != null && this.mqttSwitch.deviceStatus.get()) {
                if (this.canSwitchOff(this.mqttSwitch.lastSwitch.getTime())) {
                    AutoSwitchOffTimerEvent autoSwitchOffTimerEvent = new AutoSwitchOffTimerEvent(this.mqttSwitch, startTime, stopTime, this.mqttSwitch.lastSwitch);
                    STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(autoSwitchOffTimerEvent);
                    if (!autoSwitchOffTimerEvent.isCanceled()) {
                        STEMSystemApp.LOGGER.INFO("Auto-switch off hardId: " + this.mqttSwitch.deviceHardAddress + " configName: " + this.mqttSwitch.configName + " after: " + this.getAutoSwitchOffTimerInSeconds() + " seconds!");
                        this.mqttSwitch.switchDevice(false);
                    }
                }
            }
        }
    }
}
