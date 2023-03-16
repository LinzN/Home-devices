package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.other.*;
import de.linzn.homeDevices.devices.sensors.ZigbeeEnvironmentSensor;
import de.linzn.homeDevices.devices.switches.TasmotaSwitchDevice;
import de.linzn.homeDevices.devices.switches.ZigbeeSwitchDevice;
import de.linzn.homeDevices.listener.RequestRestartListener;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.homeDevices.profiles.EnvironmentSensorProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DeviceManager {

    private final STEMPlugin stemPlugin;

    private final Map<String, MqttDevice> mqttDevices;

    public DeviceManager(STEMPlugin stemPlugin) {
        this.stemPlugin = stemPlugin;
        this.mqttDevices = new HashMap<>();
        this.loadMqttDevices();
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().register(new RequestRestartListener());
    }

    private void loadMqttDevices() {
        File folder = new File(HomeDevicesPlugin.homeDevicesPlugin.getDataFolder(), "profiles");
        if (folder.exists()) {
            for (final File file : folder.listFiles()) {

                if (file.getName().equalsIgnoreCase("default.yml")) {
                    continue;
                }

                DeviceProfile deviceProfile = DeviceProfile.getDeviceProfile(file);

                MqttDevice mqttDevice = null;

                if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.SWITCH) {
                    if (deviceProfile.getDeviceTechnology() == DeviceTechnology.TASMOTA) {
                        mqttDevice = new TasmotaSwitchDevice(this.stemPlugin, deviceProfile);
                    } else if (deviceProfile.getDeviceTechnology() == DeviceTechnology.ZIGBEE) {
                        mqttDevice = new ZigbeeSwitchDevice(this.stemPlugin, deviceProfile);
                    }
                } else if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.SENSOR) {
                    if (deviceProfile.getDeviceTechnology() == DeviceTechnology.ZIGBEE) {
                        if (deviceProfile instanceof EnvironmentSensorProfile) {
                            mqttDevice = new ZigbeeEnvironmentSensor(this.stemPlugin, deviceProfile);
                        }
                    }
                } else if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.THERMOSTAT) {
                    if (deviceProfile.getDeviceTechnology() == DeviceTechnology.ZIGBEE) {
                        mqttDevice = new ZigbeeThermostatDevice(this.stemPlugin, deviceProfile);
                    }
                } else if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.DOORRING) {
                    mqttDevice = new DoorRingDevice(this.stemPlugin, deviceProfile);
                } else if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.POWERCONSUMPTION) {
                    mqttDevice = new PowerConsumption(this.stemPlugin, deviceProfile);
                } else if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.USV) {
                    mqttDevice = new USVDevice(this.stemPlugin, deviceProfile);
                } else if (deviceProfile.getMqttDeviceCategory() == MqttDeviceCategory.RF433) {
                    mqttDevice = new RF433Device(this.stemPlugin, deviceProfile);
                }

                if (mqttDevice != null) {
                    deviceProfile.setMqttDevice(mqttDevice);
                    deviceProfile.loadProfile();
                    deviceProfile.runProfile();
                    this.mqttDevices.put(mqttDevice.getConfigName().toLowerCase(), mqttDevice);
                }
            }
        }
    }

    public MqttDevice getMqttDevice(String configName) {
        return this.mqttDevices.get(configName.toLowerCase());
    }

    public Collection<MqttDevice> getAllDevices() {
        return this.mqttDevices.values();
    }

}
