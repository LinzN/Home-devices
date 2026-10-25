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
import de.linzn.homeDevices.healthcheck.HomeDeviceHealthCheck;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.linzn.homeDevices.webApi.WebApiHandler;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;


public class HomeDevicesPlugin extends STEMPlugin {

    public static HomeDevicesPlugin homeDevicesPlugin;

    private volatile ProfileController profileController;
    private volatile DeviceManager deviceManager;
    private WebApiHandler webApiHandler;
    private DeviceWrapperListener deviceWrapperListener;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {
        this.profileController = new ProfileController(this);
        this.deviceManager = new DeviceManager(this);
        this.deviceManager.loadMqttDevices();
        this.webApiHandler = new WebApiHandler(this);
        /* StemLink is a core only feature, there is no API for it yet */
        this.deviceWrapperListener = new DeviceWrapperListener(this);
        STEMApp.getInstance().getStemLinkModule().getStemLinkServer().registerEvents(this.deviceWrapperListener);
        /* registered through the API, so it is removed again on disable */
        this.getStemApi().health().register(new HomeDeviceHealthCheck(this));
    }

    @Override
    public void onDisable() {
        if (this.deviceWrapperListener != null) {
            try {
                STEMApp.getInstance().getStemLinkModule().getStemLinkServer().unregisterEvents(this.deviceWrapperListener);
            } catch (Exception e) {
                this.getPluginLogger().error("Unable to unregister StemLink listener", e);
            }
            this.deviceWrapperListener = null;
        }
        if (this.webApiHandler != null) {
            this.webApiHandler.disable();
            this.webApiHandler = null;
        }
        if (this.deviceManager != null) {
            this.deviceManager.shutdown();
        }
        /* scheduler tasks, MQTT subscriptions, events and health checks of this plugin are removed by STEM */
    }

    public DeviceManager getDeviceManager() {
        return this.deviceManager;
    }

    public ProfileController getProfileController() {
        return profileController;
    }
}
