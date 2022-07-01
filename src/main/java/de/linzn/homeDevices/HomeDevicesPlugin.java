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


import de.linzn.homeDevices.devices.other.DoorRingDevice;
import de.linzn.homeDevices.devices.other.ZigbeeThermostatDevice;
import de.linzn.homeDevices.devices.sensors.MqttSensor;
import de.linzn.homeDevices.devices.sensors.ZigbeeEnvironmentSensor;
import de.linzn.homeDevices.devices.switches.SwitchableMQTTDevice;
import de.linzn.homeDevices.devices.switches.TasmotaSwitchDevice;
import de.linzn.homeDevices.devices.switches.ZigbeeSwitchDevice;
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

    private Map<String, SwitchableMQTTDevice> switchableMQTTDeviceMap;
    private Map<String, MqttSensor> mqttSensorMap;
    private Map<String, ZigbeeThermostatDevice> zigbeeThermostatDeviceMap;

    private Map<DeviceCategory, Boolean> activeCategoryAutoModes;

    private DoorRingDevice doorRingDevice;

    public HomeDevicesPlugin() {
        homeDevicesPlugin = this;
    }

    @Override
    public void onEnable() {
        this.switchableMQTTDeviceMap = new HashMap<>();
        this.mqttSensorMap = new HashMap<>();
        this.zigbeeThermostatDeviceMap = new HashMap<>();
        this.activeCategoryAutoModes = new HashMap<>();
        setUpConfig();
        loadCategoryAutoModes();
        loadSwitchableDevices();
        loadSensors();
        loadThermostats();
        loadDoorRing();
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

    public SwitchableMQTTDevice getSwitchableMQTTDevice(String deviceName) {
        return this.switchableMQTTDeviceMap.get(deviceName.toLowerCase());
    }

    public MqttSensor getMqttSensor(String configName) {
        return this.mqttSensorMap.get(configName.toLowerCase());
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

    private void loadSwitchableDevices() {
        HashMap<String, List> hashMap = (HashMap) this.getDefaultConfig().get("switches", new HashMap<String, List>());
        for (String configName : hashMap.keySet()) {
            String deviceHardAddress = this.getDefaultConfig().getString("switches." + configName + ".deviceHardAddress", configName.toLowerCase());
            DeviceCategory deviceCategory = DeviceCategory.valueOf(this.getDefaultConfig().getString("switches." + configName + ".category", DeviceCategory.OTHER.name()));
            String description = this.getDefaultConfig().getString("switches." + configName + ".description", "No description");
            DeviceBrand deviceBrand = DeviceBrand.valueOf(this.getDefaultConfig().getString("switches." + configName + ".deviceBrand"));

            SwitchableMQTTDevice switchableMQTTDevice = null;

            if (deviceBrand == DeviceBrand.TASMOTA) {
                switchableMQTTDevice = new TasmotaSwitchDevice(homeDevicesPlugin, configName, deviceHardAddress, deviceCategory, description);
            } else if (deviceBrand == DeviceBrand.ZIGBEE) {
                String zigbeeGatewayMqttName = this.getDefaultConfig().getString("switches." + configName + ".zigbeeGatewayMqttName", "sonoff_switch");
                switchableMQTTDevice = new ZigbeeSwitchDevice(homeDevicesPlugin, configName, deviceHardAddress, deviceCategory, description, zigbeeGatewayMqttName);
            }

            if (switchableMQTTDevice != null) {
                this.switchableMQTTDeviceMap.put(switchableMQTTDevice.getConfigName(), switchableMQTTDevice);
            }
        }
    }

    private void loadSensors() {
        HashMap<String, List> hashMap = (HashMap) this.getDefaultConfig().get("sensors", new HashMap<String, List>());
        for (String configName : hashMap.keySet()) {
            String deviceHardAddress = this.getDefaultConfig().getString("sensors." + configName + ".deviceHardAddress", configName.toLowerCase());
            SensorCategory sensorCategory = SensorCategory.valueOf(this.getDefaultConfig().getString("sensors." + configName + ".category", SensorCategory.OTHER.name()));
            String description = this.getDefaultConfig().getString("sensors." + configName + ".description", "No description");
            DeviceBrand deviceBrand = DeviceBrand.valueOf(this.getDefaultConfig().getString("sensors." + configName + ".deviceBrand"));
            MqttSensor mqttSensor = null;

            if (deviceBrand == DeviceBrand.TASMOTA) {
                //do nothing at the moment - no such devices
            } else if (deviceBrand == DeviceBrand.ZIGBEE) {
                String zigbeeGatewayMqttName = this.getDefaultConfig().getString("sensors." + configName + ".zigbeeGatewayMqttName", "sonoff_switch");

                if (sensorCategory == SensorCategory.ENVIRONMENT) {
                    mqttSensor = new ZigbeeEnvironmentSensor(homeDevicesPlugin, deviceHardAddress, description, configName, zigbeeGatewayMqttName);
                } else if (sensorCategory == SensorCategory.OTHER) {
                    //do nothing at the moment - no such devices
                }
            }

            if (mqttSensor != null) {
                this.mqttSensorMap.put(mqttSensor.getConfigName(), mqttSensor);
            }
        }
    }

    private void loadThermostats() {
        HashMap<String, List> hashMap = (HashMap) this.getDefaultConfig().get("thermostats", new HashMap<String, List>());
        for (String configName : hashMap.keySet()) {
            String deviceHardAddress = this.getDefaultConfig().getString("thermostats." + configName + ".deviceHardAddress", configName.toLowerCase());
            String description = this.getDefaultConfig().getString("thermostats." + configName + ".description", "No description");
            DeviceBrand deviceBrand = DeviceBrand.valueOf(this.getDefaultConfig().getString("thermostats." + configName + ".deviceBrand"));
            ZigbeeThermostatDevice zigbeeThermostatDevice = null;

            if (deviceBrand == DeviceBrand.ZIGBEE) {
                String zigbeeGatewayMqttName = this.getDefaultConfig().getString("thermostats." + configName + ".zigbeeGatewayMqttName", "sonoff_switch");
                zigbeeThermostatDevice = new ZigbeeThermostatDevice(homeDevicesPlugin, deviceHardAddress, description, configName, zigbeeGatewayMqttName);

            }

            if (zigbeeThermostatDevice != null) {
                this.zigbeeThermostatDeviceMap.put(zigbeeThermostatDevice.getConfigName(), zigbeeThermostatDevice);
            }
        }
    }

    private void loadDoorRing() {
        this.doorRingDevice = new DoorRingDevice(this);
    }
}
