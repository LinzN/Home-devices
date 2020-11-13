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
import de.linzn.homeDevices.restfulapi.get.GET_AutoMode;
import de.linzn.homeDevices.restfulapi.get.GET_DeviceStatus;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeAutoMode;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeDevice;
import de.linzn.restfulapi.RestFulApiPlugin;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeDevicesPlugin extends STEMPlugin {

    public static HomeDevicesPlugin homeDevicesPlugin;

    private Map<String, TasmotaDevice> tasmotaDeviceMap;

    private Map<DeviceCategory, Boolean> autoMode;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {
        this.tasmotaDeviceMap = new HashMap<>();
        this.autoMode = new HashMap<>();
        setUpConfig();
        loadAutoMode();
        loadTasmotaDevices();
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_AutoMode(this));
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_DeviceStatus(this));

        RestFulApiPlugin.restFulApiPlugin.registerIPostJSONClass(new POST_ChangeAutoMode(this));
        RestFulApiPlugin.restFulApiPlugin.registerIPostJSONClass(new POST_ChangeDevice(this));
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

    public boolean isAutoMode(DeviceCategory deviceCategory) {
        return this.autoMode.get(deviceCategory);
    }

    public boolean setAutoMode(DeviceCategory deviceCategory, boolean value) {
        this.autoMode.put(deviceCategory, value);
        return isAutoMode(deviceCategory);
    }

    private void loadAutoMode() {
        for (DeviceCategory deviceCategory : DeviceCategory.values()) {
            boolean value = this.getDefaultConfig().getBoolean("defaultAutomode." + deviceCategory.name(), false);
            STEMSystemApp.LOGGER.DEBUG("Found automode" + deviceCategory.name() + ":" + value);
            this.autoMode.put(deviceCategory, value);
        }
    }

    private void loadTasmotaDevices() {

        HashMap<String, List> hashMap = (HashMap) this.getDefaultConfig().get("tasmota");

        for (String name : hashMap.keySet()) {
            String deviceName = name;
            String hostName = this.getDefaultConfig().getString("tasmota." + deviceName + ".hostname");
            DeviceCategory deviceCategory = DeviceCategory.valueOf(this.getDefaultConfig().getString("tasmota." + deviceName + ".category", DeviceCategory.OTHER.name()));
            TasmotaDevice tasmotaDevice = new TasmotaDevice(deviceName, hostName, deviceCategory);
            STEMSystemApp.LOGGER.DEBUG("Found tasmota device " + deviceName + ":" + hostName + " category: " + deviceCategory.name());
            this.tasmotaDeviceMap.put(tasmotaDevice.getDeviceName(), tasmotaDevice);

            if (this.getDefaultConfig().getBoolean("tasmota." + deviceName + ".timed", false)) {
                String timedStart = this.getDefaultConfig().getString("tasmota." + deviceName + ".timedStart");
                String timedStop = this.getDefaultConfig().getString("tasmota." + deviceName + ".timedStop");
                int timedOffsetMinutes = this.getDefaultConfig().getInt("tasmota." + deviceName + ".timedOffsetMinutes");
                tasmotaDevice.setTimed(timedStart, timedStop, timedOffsetMinutes);
                STEMSystemApp.LOGGER.DEBUG("Timer enabled to tasmota device " + deviceName + ":" + hostName);
                STEMSystemApp.LOGGER.DEBUG("Between " + timedStart + " - " + timedStop + " offset " + timedOffsetMinutes);
            }
        }
    }
}
