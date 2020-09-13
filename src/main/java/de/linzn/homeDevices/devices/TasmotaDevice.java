/*
 * Copyright (C) 2020. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.DeviceCategory;
import de.linzn.homeDevices.DeviceStatus;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.TasmotaDeviceUtils;
import de.stem.stemSystem.STEMSystemApp;

import java.util.concurrent.TimeUnit;

public class TasmotaDevice {

    private final String deviceName;
    private final DeviceCategory deviceCategorie;
    private final String hostname;
    private DeviceStatus deviceStatus;
    private long lastEnabled;

    private boolean isTimed;
    private String timedStart;
    private String timedStop;
    private int timedOffsetMinutes;

    public TasmotaDevice(String deviceName, String hostname, DeviceCategory deviceCategorie) {
        this.deviceName = deviceName.toLowerCase();
        this.deviceCategorie = deviceCategorie;
        this.hostname = hostname;
        this.deviceStatus = null;
        this.lastEnabled = -1;
        this.isTimed = false;
        this.timedStart = "00:00";
        this.timedStop = "00:00";
        this.timedOffsetMinutes = 0;
        this.statusTask();
    }

    public void setTimed(String timedStart, String timedStop, int timedOffsetMinutes) {
        this.isTimed = true;
        this.timedStart = timedStart;
        this.timedStop = timedStop;
        this.timedOffsetMinutes = timedOffsetMinutes;
    }

    public void toggleDevice() {
        update_status(TasmotaDeviceUtils.toggleDevice(hostname));
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getHostname() {
        return hostname;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(boolean status) {
        update_status(TasmotaDeviceUtils.setDeviceStatus(hostname, status));
    }

    private void update_status(DeviceStatus newDeviceStatus) {
        if (this.deviceStatus != null) {
            if (this.deviceStatus == DeviceStatus.DISABLED && newDeviceStatus == DeviceStatus.ENABLED) {
                this.lastEnabled = System.currentTimeMillis();
            } else if (newDeviceStatus == DeviceStatus.DISABLED) {
                this.lastEnabled = -1;
            }
        } else {
            if (newDeviceStatus == DeviceStatus.ENABLED) {
                this.lastEnabled = System.currentTimeMillis();
            }
        }
        this.deviceStatus = newDeviceStatus;
    }


    private void statusTask() {
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, () -> {

            update_status(TasmotaDeviceUtils.readDeviceStatus(hostname));

            STEMSystemApp.LOGGER.DEBUG("Device status " + deviceName + " " + deviceStatus.name());
            if (HomeDevicesPlugin.homeDevicesPlugin.isAutoMode(this.deviceCategorie)) {
                if (isTimed) {
                    if (TasmotaDeviceUtils.checkLightShutdown(lastEnabled, timedStart, timedStop, timedOffsetMinutes)) {
                        if (deviceStatus == DeviceStatus.ENABLED) {
                            STEMSystemApp.LOGGER.DEBUG("AutoOFF " + this.deviceCategorie.name() + " " + deviceName);
                            setDeviceStatus(false);
                        }
                        lastEnabled = -1;
                    }
                }
            }
        }, 5, 2, TimeUnit.SECONDS);
    }

}
