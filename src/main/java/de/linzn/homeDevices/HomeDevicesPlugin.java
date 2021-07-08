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
import de.linzn.homeDevices.restfulapi.get.GET_AutoMode;
import de.linzn.homeDevices.restfulapi.get.GET_DeviceStatus;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeAutoMode;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeDevice;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.linzn.restfulapi.RestFulApiPlugin;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeDevicesPlugin extends STEMPlugin {

    public static HomeDevicesPlugin homeDevicesPlugin;

    private Map<String, TasmotaMQTTDevice> tasmotaDeviceMap;

    private Map<DeviceCategory, Boolean> activeCategoryAutoModes;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {
        this.tasmotaDeviceMap = new HashMap<>();
        this.activeCategoryAutoModes = new HashMap<>();
        setUpConfig();
        loadCategoryAutoModes();
        loadTasmotaDevices();
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_AutoMode(this));
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_DeviceStatus(this));

        RestFulApiPlugin.restFulApiPlugin.registerIPostJSONClass(new POST_ChangeAutoMode(this));
        RestFulApiPlugin.restFulApiPlugin.registerIPostJSONClass(new POST_ChangeDevice(this));
        STEMSystemApp.getInstance().getStemLinkModule().getStemLinkServer().registerEvents(new DeviceWrapperListener(this));
    }

    @Override
    public void onDisable() {

    }

    private void setUpConfig() {
        this.getDefaultConfig().save();
    }

    public TasmotaMQTTDevice getTasmotaDevice(String deviceName) {
        return this.tasmotaDeviceMap.get(deviceName.toLowerCase());
    }

    public boolean isCategoryInAutoSwitchOffMode(DeviceCategory deviceCategory) {
        return this.activeCategoryAutoModes.get(deviceCategory);
    }

    public boolean setCategoryInAutoMode(DeviceCategory deviceCategory, boolean value) {
        this.activeCategoryAutoModes.put(deviceCategory, value);
        STEMSystemApp.LOGGER.INFO("Update DeviceCategory autoMode: " + deviceCategory + " status: " + value);
        return isCategoryInAutoSwitchOffMode(deviceCategory);
    }

    private void loadCategoryAutoModes() {
        for (DeviceCategory deviceCategory : DeviceCategory.values()) {
            boolean value = this.getDefaultConfig().getBoolean("category." + deviceCategory.name() + ".autoSwitchOffEnabled");
            STEMSystemApp.LOGGER.CONFIG("Load categoryAutoMode for " + deviceCategory.name() + ":" + value);
            this.activeCategoryAutoModes.put(deviceCategory, value);
        }
    }

    private void loadTasmotaDevices() {
        HashMap<String, List> hashMap = (HashMap) this.getDefaultConfig().get("tasmota");

        for (String configName : hashMap.keySet()) {
            TasmotaMQTTDevice tasmotaMQTTDevice = new TasmotaMQTTDevice(homeDevicesPlugin, configName);
            this.tasmotaDeviceMap.put(tasmotaMQTTDevice.getConfigName(), tasmotaMQTTDevice);
        }
    }
}
