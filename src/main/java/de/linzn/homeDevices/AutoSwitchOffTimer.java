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

import de.linzn.homeDevices.devices.switches.SwitchableMQTTDevice;
import de.linzn.homeDevices.events.AutoSwitchOffTimerEvent;
import de.stem.stemSystem.STEMSystemApp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class AutoSwitchOffTimer implements Runnable {

    private final SwitchableMQTTDevice switchableMQTTDevice;
    private final long autoSwitchOffTimer;
    private final boolean autoSwitchEnabled;

    private final LocalTime startTime;
    private final LocalTime stopTime;

    public AutoSwitchOffTimer(SwitchableMQTTDevice switchableMQTTDevice) {
        this.switchableMQTTDevice = switchableMQTTDevice;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm");

        if (HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().contains("switches." + switchableMQTTDevice.getConfigName() + ".autoModeSwitchOffSettings")) {
            this.autoSwitchEnabled = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean("switches." + switchableMQTTDevice.getConfigName() + ".autoModeSwitchOffSettings.autoSwitchOffEnabled");
            this.autoSwitchOffTimer = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getInt("switches." + switchableMQTTDevice.getConfigName() + ".autoModeSwitchOffSettings.autoSwitchOffAfterSeconds") * 1000L;
            this.startTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("switches." + switchableMQTTDevice.getConfigName() + ".autoModeSwitchOffSettings.startTime"), dateTimeFormatter);
            this.stopTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("switches." + switchableMQTTDevice.getConfigName() + ".autoModeSwitchOffSettings.stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.CONFIG("Load specific autoSwitchOff settings for hardId " + switchableMQTTDevice.getDeviceHardAddress() + " configName " + switchableMQTTDevice.getConfigName());
        } else {
            this.autoSwitchEnabled = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean("category." + switchableMQTTDevice.getDeviceCategory().name() + ".autoSwitchOffEnabled");
            this.autoSwitchOffTimer = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getInt("category." + switchableMQTTDevice.getDeviceCategory().name() + ".autoSwitchOffAfterSeconds") * 1000L;
            this.startTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("category." + switchableMQTTDevice.getDeviceCategory().name() + ".startTime"), dateTimeFormatter);
            this.stopTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("category." + switchableMQTTDevice.getDeviceCategory().name() + ".stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.CONFIG("No autoSwitchOff settings found for hardId " + switchableMQTTDevice.getDeviceHardAddress() + " configName  " + switchableMQTTDevice.getConfigName());
            STEMSystemApp.LOGGER.CONFIG("Load default settings from category " + switchableMQTTDevice.getDeviceCategory().name());
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
        if (HomeDevicesPlugin.homeDevicesPlugin.isCategoryInAutoSwitchOffMode(this.switchableMQTTDevice.getDeviceCategory())) {
            if (this.switchableMQTTDevice.deviceStatus != null && this.switchableMQTTDevice.deviceStatus.get()) {
                if (this.canSwitchOff(this.switchableMQTTDevice.lastSwitch.getTime())) {
                    AutoSwitchOffTimerEvent autoSwitchOffTimerEvent = new AutoSwitchOffTimerEvent(this.switchableMQTTDevice, startTime, stopTime, this.switchableMQTTDevice.lastSwitch);
                    STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(autoSwitchOffTimerEvent);
                    if (!autoSwitchOffTimerEvent.isCanceled()) {
                        STEMSystemApp.LOGGER.INFO("Auto-switch off hardId: " + this.switchableMQTTDevice.deviceHardAddress + " configName: " + this.switchableMQTTDevice.configName + " after: " + this.getAutoSwitchOffTimerInSeconds() + " seconds!");
                        this.switchableMQTTDevice.switchDevice(false);
                    }
                }
            }
        }
    }
}
