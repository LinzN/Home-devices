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

import de.linzn.homeDevices.DeviceStatus;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.TasmotaDeviceUtils;
import de.stem.stemSystem.AppLogger;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.utils.Color;

import java.util.concurrent.TimeUnit;

public class TasmotaDevice {

    private String deviceName;
    private String hostname;
    private DeviceStatus deviceStatus;

    public TasmotaDevice(String deviceName, String hostname) {
        this.deviceName = deviceName.toLowerCase();
        this.hostname = hostname;
        this.deviceStatus = DeviceStatus.OFFLINE;
        this.statusTask();
    }

    private void statusTask() {
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, () -> {
            deviceStatus = TasmotaDeviceUtils.readDeviceStatus(hostname);
            AppLogger.debug(Color.GREEN + "Device status " + deviceName + " " + deviceStatus.name());
        }, 5, 2, TimeUnit.SECONDS);
    }


    public void toggleDevice() {
        TasmotaDeviceUtils.toggleDevice(hostname);
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
        TasmotaDeviceUtils.setDeviceStatus(hostname, status);
    }

}
