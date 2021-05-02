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

import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.stem.stemSystem.STEMSystemApp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class AutoTimer {
    private final long autoSwitchOffTimer;
    private final boolean autoSwitchEnabled;

    private final LocalTime startTime;
    private final LocalTime stopTime;

    public AutoTimer(TasmotaMQTTDevice tasmotaMQTTDevice) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm");

        if (HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().contains("tasmota." + tasmotaMQTTDevice.getConfigName() + ".autoModeSettings")) {
            this.autoSwitchEnabled = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean("tasmota." + tasmotaMQTTDevice.getConfigName() + ".autoModeSettings.autoSwitchEnabled");
            this.autoSwitchOffTimer = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getInt("tasmota." + tasmotaMQTTDevice.getConfigName() + ".autoModeSettings.autoSwitchOffAfterSeconds") * 1000L;
            this.startTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("tasmota." + tasmotaMQTTDevice.getConfigName() + ".autoModeSettings.startTime"), dateTimeFormatter);
            this.stopTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("tasmota." + tasmotaMQTTDevice.getConfigName() + ".autoModeSettings.stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.INFO("Load specific autoSwitch settings for hardId " + tasmotaMQTTDevice.getDeviceHardAddress() + " configName " + tasmotaMQTTDevice.getConfigName());
        } else {
            this.autoSwitchEnabled = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean("category." + tasmotaMQTTDevice.getDeviceCategory().name() + ".autoSwitchEnabled");
            this.autoSwitchOffTimer = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getInt("category." + tasmotaMQTTDevice.getDeviceCategory().name() + ".autoSwitchOffAfterSeconds") * 1000L;
            this.startTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("category." + tasmotaMQTTDevice.getDeviceCategory().name() + ".startTime"), dateTimeFormatter);
            this.stopTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("category." + tasmotaMQTTDevice.getDeviceCategory().name() + ".stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.INFO("No autoSwitch settings found for hardId " + tasmotaMQTTDevice.getDeviceHardAddress() + " configName  " + tasmotaMQTTDevice.getConfigName());
            STEMSystemApp.LOGGER.INFO("Load default settings from category " + tasmotaMQTTDevice.getDeviceCategory().name());
        }

        if (this.startTime.equals(this.stopTime)) {
            STEMSystemApp.LOGGER.INFO("Start and stop are the same LocalTime! This is useless!");
        }
    }

    public boolean canSwitchOff(long lastSwitch) {
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

    public int getAutoSwitchOffTimerInSeconds() {
        return (int) (autoSwitchOffTimer / 1000);
    }

    public long getAutoSwitchOffTimer() {
        return autoSwitchOffTimer;
    }

    public boolean isAutoSwitchEnabled() {
        return autoSwitchEnabled;
    }
}
