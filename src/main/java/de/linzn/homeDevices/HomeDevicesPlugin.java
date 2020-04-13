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

package de.linzn.homeDevices;


import de.linzn.homeDevices.devices.TasmotaDevice;
import de.stem.stemSystem.AppLogger;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import de.stem.stemSystem.utils.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeDevicesPlugin extends STEMPlugin {

    public static HomeDevicesPlugin homeDevicesPlugin;

    private Map<String, TasmotaDevice> tasmotaDeviceMap;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {
        this.tasmotaDeviceMap = new HashMap<>();
        setUpConfig();
        loadTasmotaDevices();
    }

    @Override
    public void onDisable() {

    }

    private void setUpConfig() {
        this.getDefaultConfig().save();
    }

    public TasmotaDevice getTasmotaDevice(String deviceName) {
        return this.tasmotaDeviceMap.get(deviceName.toLowerCase());
    }

    private void loadTasmotaDevices() {
        HashMap<String, List> hashMap = (HashMap) this.getDefaultConfig().get("tasmota");

        for (String name : hashMap.keySet()) {
            String deviceName = name;
            String hostName = this.getDefaultConfig().getString("tasmota." + deviceName + ".hostname");
            TasmotaDevice tasmotaDevice = new TasmotaDevice(deviceName, hostName);
            AppLogger.debug(Color.GREEN + "Found tasmota device " + deviceName + ":" + hostName);
            this.tasmotaDeviceMap.put(tasmotaDevice.getDeviceName(), tasmotaDevice);

            if (this.getDefaultConfig().getBoolean("tasmota." + deviceName + ".timed", false)) {
                String timedStart = this.getDefaultConfig().getString("tasmota." + deviceName + ".timedStart");
                String timedStop = this.getDefaultConfig().getString("tasmota." + deviceName + ".timedStop");
                int timedOffsetMinutes = this.getDefaultConfig().getInt("tasmota." + deviceName + ".timedOffsetMinutes");
                tasmotaDevice.setTimed(timedStart, timedStop, timedOffsetMinutes);
                AppLogger.debug(Color.GREEN + "Timer enabled to tasmota device " + deviceName + ":" + hostName);
                AppLogger.debug(Color.GREEN + "Between " + timedStart + " - " + timedStop + " offset " + timedOffsetMinutes);
            }
        }
    }
}
