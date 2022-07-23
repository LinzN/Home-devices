package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.other.DoorRingDevice;
import de.linzn.homeDevices.devices.other.ZigbeeThermostatDevice;
import de.linzn.homeDevices.devices.sensors.ZigbeeEnvironmentSensor;
import de.linzn.homeDevices.devices.switches.TasmotaSwitchDevice;
import de.linzn.homeDevices.devices.switches.ZigbeeSwitchDevice;
import de.linzn.homeDevices.listener.RequestRestartListener;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceManager {

    private final STEMPlugin stemPlugin;
    private final Map<String, MqttDevice> mqttDevices;

    private String randomZigbeeGateway;

    public DeviceManager(STEMPlugin stemPlugin) {
        this.stemPlugin = stemPlugin;
        this.mqttDevices = new HashMap<>();
        this.randomZigbeeGateway = null;
        this.loadMqttDevices();
        if (randomZigbeeGateway != null) {
            STEMSystemApp.getInstance().getEventModule().getStemEventBus().register(new RequestRestartListener(randomZigbeeGateway));
        }
    }

    private void loadMqttDevices() {
        HashMap<String, List> hashMap = (HashMap) this.stemPlugin.getDefaultConfig().get("mqttDevices", new HashMap<String, List>());
        for (String configName : hashMap.keySet()) {

            String configPath = "mqttDevices." + configName;

            String deviceHardAddress = this.stemPlugin.getDefaultConfig().getString(configPath + ".deviceHardAddress", configName.toLowerCase());
            String description = this.stemPlugin.getDefaultConfig().getString(configPath + ".description", "No description");
            DeviceTechnology deviceTechnology = DeviceTechnology.valueOf(this.stemPlugin.getDefaultConfig().getString(configPath + ".deviceTechnology"));
            MqttDeviceCategory mqttDeviceCategory = MqttDeviceCategory.valueOf(this.stemPlugin.getDefaultConfig().getString(configPath + ".mqttDeviceCategory"));
            MqttDevice mqttDevice = null;

            if (mqttDeviceCategory == MqttDeviceCategory.SWITCH) {
                SwitchCategory switchCategory = SwitchCategory.valueOf(this.stemPlugin.getDefaultConfig().getString(configPath + ".options.category", SwitchCategory.OTHER.name()));
                if (deviceTechnology == DeviceTechnology.TASMOTA) {
                    mqttDevice = new TasmotaSwitchDevice(this.stemPlugin, configName, deviceHardAddress, switchCategory, description);
                } else if (deviceTechnology == DeviceTechnology.ZIGBEE) {
                    String zigbeeGateway = this.stemPlugin.getDefaultConfig().getString(configPath + ".options.zigbeeGateway", "zigbee2mqtt");
                    this.randomZigbeeGateway = zigbeeGateway;
                    mqttDevice = new ZigbeeSwitchDevice(this.stemPlugin, configName, deviceHardAddress, switchCategory, description, zigbeeGateway);
                }
            } else if (mqttDeviceCategory == MqttDeviceCategory.SENSOR) {
                SensorCategory sensorCategory = SensorCategory.valueOf(this.stemPlugin.getDefaultConfig().getString(configPath + ".options.category", SensorCategory.OTHER.name()));
                if (deviceTechnology == DeviceTechnology.TASMOTA) {
                    //do nothing at the moment - no such devices
                } else if (deviceTechnology == DeviceTechnology.ZIGBEE) {
                    String zigbeeGateway = this.stemPlugin.getDefaultConfig().getString(configPath + ".options.zigbeeGateway", "zigbee2mqtt");
                    this.randomZigbeeGateway = zigbeeGateway;
                    if (sensorCategory == SensorCategory.ENVIRONMENT) {
                        mqttDevice = new ZigbeeEnvironmentSensor(this.stemPlugin, deviceHardAddress, description, configName, zigbeeGateway);
                    } else if (sensorCategory == SensorCategory.OTHER) {
                        //do nothing at the moment - no such devices
                    }
                }
            } else if (mqttDeviceCategory == MqttDeviceCategory.THERMOSTAT) {
                if (deviceTechnology == DeviceTechnology.TASMOTA) {
                    //do nothing at the moment - no such devices
                } else if (deviceTechnology == DeviceTechnology.ZIGBEE) {
                    String zigbeeGateway = this.stemPlugin.getDefaultConfig().getString(configPath + ".options.zigbeeGateway", "zigbee2mqtt");
                    this.randomZigbeeGateway = zigbeeGateway;
                    mqttDevice = new ZigbeeThermostatDevice(this.stemPlugin, deviceHardAddress, description, configName, zigbeeGateway);
                }
            } else if (mqttDeviceCategory == MqttDeviceCategory.DOORRING) {
                mqttDevice = new DoorRingDevice(this.stemPlugin, configName, deviceHardAddress, description);
            }

            if (mqttDevice != null) {
                this.mqttDevices.put(mqttDevice.configName.toLowerCase(), mqttDevice);
            }
        }
    }

    public MqttDevice getMqttDevice(String configName) {
        return this.mqttDevices.get(configName.toLowerCase());
    }
}
