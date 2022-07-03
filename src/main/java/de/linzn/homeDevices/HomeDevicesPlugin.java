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


import de.linzn.homeDevices.devices.DeviceManager;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.other.DoorRingDevice;
import de.linzn.homeDevices.restfulapi.get.GET_AutoMode;
import de.linzn.homeDevices.restfulapi.get.GET_DeviceStatus;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeAutoMode;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeDevice;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.linzn.restfulapi.RestFulApiPlugin;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.util.HashMap;
import java.util.Map;

public class HomeDevicesPlugin extends STEMPlugin {

    public static HomeDevicesPlugin homeDevicesPlugin;
    private DeviceManager deviceManager;


    private Map<SwitchCategory, Boolean> activeCategoryAutoModes;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {

        this.activeCategoryAutoModes = new HashMap<>();
        setUpConfig();
        loadCategoryAutoModes();
        this.deviceManager = new DeviceManager(this);
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

    public DeviceManager getDeviceManager() {
        return this.deviceManager;
    }

    public boolean isCategoryInAutoSwitchOffMode(SwitchCategory switchCategory) {
        return this.activeCategoryAutoModes.get(switchCategory);
    }

    public boolean setCategoryInAutoMode(SwitchCategory switchCategory, boolean value) {
        this.activeCategoryAutoModes.put(switchCategory, value);
        STEMSystemApp.LOGGER.INFO("Update DeviceCategory autoMode: " + switchCategory + " status: " + value);
        return isCategoryInAutoSwitchOffMode(switchCategory);
    }

    private void loadCategoryAutoModes() {
        for (SwitchCategory switchCategory : SwitchCategory.values()) {
            boolean value = this.getDefaultConfig().getBoolean("category." + switchCategory.name() + ".autoSwitchOffEnabled");
            STEMSystemApp.LOGGER.CONFIG("Load categoryAutoMode for " + switchCategory.name() + ":" + value);
            this.activeCategoryAutoModes.put(switchCategory, value);
        }
    }

}
