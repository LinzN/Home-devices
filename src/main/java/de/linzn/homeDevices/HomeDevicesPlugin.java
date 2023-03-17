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
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.healthcheck.HomeDeviceHealthCheck;
import de.linzn.homeDevices.restfulapi.get.GET_AutoMode;
import de.linzn.homeDevices.restfulapi.get.GET_DeviceStatus;
import de.linzn.homeDevices.restfulapi.get.GET_MqttDeviceData;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeAutoMode;
import de.linzn.homeDevices.restfulapi.push.POST_ChangeDevice;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.linzn.homeDevices.webApi.WebApiHandler;
import de.linzn.restfulapi.RestFulApiPlugin;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

public class HomeDevicesPlugin extends STEMPlugin {

    public static HomeDevicesPlugin homeDevicesPlugin;
    private DeviceManager deviceManager;
    private WebApiHandler webApiHandler;

    private SmartHomeProfile currentProfile;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {
        this.currentProfile = SmartHomeProfile.DEFAULT;
        this.deviceManager = new DeviceManager(this);
        this.webApiHandler = new WebApiHandler(this);
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_AutoMode(this));
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_DeviceStatus(this));
        RestFulApiPlugin.restFulApiPlugin.registerIGetJSONClass(new GET_MqttDeviceData(this));

        RestFulApiPlugin.restFulApiPlugin.registerIPostJSONClass(new POST_ChangeAutoMode(this));
        RestFulApiPlugin.restFulApiPlugin.registerIPostJSONClass(new POST_ChangeDevice(this));
        STEMSystemApp.getInstance().getStemLinkModule().getStemLinkServer().registerEvents(new DeviceWrapperListener(this));
        STEMSystemApp.getInstance().getHealthModule().registerHealthCheck(new HomeDeviceHealthCheck(this));
    }

    @Override
    public void onDisable() {

    }

    public DeviceManager getDeviceManager() {
        return this.deviceManager;
    }

    public SmartHomeProfile getCurrentProfile() {
        return currentProfile;
    }

    public boolean changeSmartHomeProfile(SmartHomeProfile smartHomeProfile) {
        this.currentProfile = smartHomeProfile;
        boolean success = true;
        STEMSystemApp.LOGGER.CONFIG("Changing SmartHomeProfile to " + smartHomeProfile.name());

        for (MqttDevice mqttDevice : this.getDeviceManager().getAllDevices()) {
            if (!mqttDevice.deviceProfile.changeSmartProfile()) {
                success = false;
                STEMSystemApp.LOGGER.ERROR("Error while changing SmartHomeProfile to " + smartHomeProfile.name());
            } else {
                STEMSystemApp.LOGGER.CONFIG("Success changing SmartHomeProfile to " + smartHomeProfile.name());
            }
        }
        return success;
    }

}
