package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.other.*;
import de.linzn.homeDevices.devices.other.nanoCLR.DoorRingDevice;
import de.linzn.homeDevices.devices.other.nanoCLR.KeyBoardDevice;
import de.linzn.homeDevices.devices.sensors.DoorSensor;
import de.linzn.homeDevices.devices.sensors.ZigbeeEnvironmentSensor;
import de.linzn.homeDevices.devices.switches.TasmotaSwitchDevice;
import de.linzn.homeDevices.devices.switches.ZigbeeSwitchDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.homeDevices.profiles.DoorSensorProfile;
import de.linzn.homeDevices.profiles.EnvironmentSensorProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {

    private final STEMPlugin stemPlugin;
    private final Map<String, MqttDevice> mqttDevices;

    public DeviceManager(STEMPlugin stemPlugin) {
        this.stemPlugin = stemPlugin;
        this.mqttDevices = new ConcurrentHashMap<>();
    }

    /**
     * Loads every device profile from the profiles folder. A broken profile is logged and skipped,
     * it does not stop the other devices from loading.
     */
    public void loadMqttDevices() {
        File folder = new File(this.stemPlugin.getDataFolder(), "profiles");
        File[] files = folder.listFiles(file -> file.isFile() && !file.getName().startsWith("."));
        if (files == null) {
            this.stemPlugin.getPluginLogger().warning("No device profile folder found at " + folder.getAbsolutePath());
            return;
        }
        Arrays.sort(files);
        for (final File file : files) {
            if (file.getName().equalsIgnoreCase("default.yml")) {
                continue;
            }
            try {
                this.loadDevice(file);
            } catch (Exception e) {
                this.stemPlugin.getPluginLogger().error("Unable to load device profile " + file.getName(), e);
            }
        }
        this.stemPlugin.getPluginLogger().info("Loaded " + this.mqttDevices.size() + " home devices");
    }

    private void loadDevice(File file) {
        DeviceProfile deviceProfile = DeviceProfile.getDeviceProfile(file);
        MqttDevice mqttDevice = this.createDevice(deviceProfile);
        if (mqttDevice == null) {
            this.stemPlugin.getPluginLogger().warning("No device implementation for " + file.getName() + " (" + deviceProfile.getMqttDeviceCategory() + ", " + deviceProfile.getDeviceTechnology() + ")");
            return;
        }
        String key = mqttDevice.getConfigName().toLowerCase();
        if (this.mqttDevices.containsKey(key)) {
            this.stemPlugin.getPluginLogger().error("Duplicate device name " + mqttDevice.getConfigName() + " in " + file.getName() + ", device skipped");
            return;
        }
        deviceProfile.setMqttDevice(mqttDevice);
        deviceProfile.loadProfile();
        this.mqttDevices.put(key, mqttDevice);
        /* subscribe only after the device and its profile are fully initialized */
        mqttDevice.start();
        deviceProfile.runProfile();
    }

    private MqttDevice createDevice(DeviceProfile deviceProfile) {
        MqttDeviceCategory category = deviceProfile.getMqttDeviceCategory();
        DeviceTechnology technology = deviceProfile.getDeviceTechnology();
        return switch (category) {
            case SWITCH -> switch (technology) {
                case TASMOTA -> new TasmotaSwitchDevice(this.stemPlugin, deviceProfile);
                case ZIGBEE -> new ZigbeeSwitchDevice(this.stemPlugin, deviceProfile);
                default -> null;
            };
            case SENSOR -> {
                if (technology != DeviceTechnology.ZIGBEE) {
                    yield null;
                }
                if (deviceProfile instanceof EnvironmentSensorProfile) {
                    yield new ZigbeeEnvironmentSensor(this.stemPlugin, deviceProfile);
                } else if (deviceProfile instanceof DoorSensorProfile) {
                    yield new DoorSensor(this.stemPlugin, deviceProfile);
                }
                yield null;
            }
            case THERMOSTAT -> technology == DeviceTechnology.ZIGBEE ? new ZigbeeThermostatDevice(this.stemPlugin, deviceProfile) : null;
            case DOORRING -> new DoorRingDevice(this.stemPlugin, deviceProfile);
            case POWERCONSUMPTION -> new PowerConsumption(this.stemPlugin, deviceProfile);
            case USV -> new USVDevice(this.stemPlugin, deviceProfile);
            case RF433 -> new RF433Device(this.stemPlugin, deviceProfile);
            case LED -> new LEDDevice(this.stemPlugin, deviceProfile);
            case NANOCLR_KEYBOARD -> new KeyBoardDevice(this.stemPlugin, deviceProfile);
            case ANKERMAKE_PRINTER -> new AnkermakePrinter(this.stemPlugin, deviceProfile);
            case MIDEA_AC -> new MideaAC(this.stemPlugin, deviceProfile);
            case SNAPMAKER_PRINTER -> new SnapmakerPrinter(this.stemPlugin, deviceProfile);
            default -> null;
        };
    }

    /**
     * Stops the profile timers and MQTT subscriptions of all devices.
     */
    public void shutdown() {
        for (MqttDevice mqttDevice : this.mqttDevices.values()) {
            try {
                mqttDevice.getDeviceProfile().stopProfile();
                mqttDevice.stop();
            } catch (Exception e) {
                this.stemPlugin.getPluginLogger().error("Error while stopping device " + mqttDevice.getConfigName(), e);
            }
        }
        this.mqttDevices.clear();
    }

    /**
     * @param configName name of the device (case insensitive)
     * @return the device or null if no device with this name exists
     */
    public MqttDevice getMqttDevice(String configName) {
        if (configName == null) {
            return null;
        }
        return this.mqttDevices.get(configName.toLowerCase());
    }

    public Collection<MqttDevice> getAllDevices() {
        return Collections.unmodifiableCollection(this.mqttDevices.values());
    }
}
