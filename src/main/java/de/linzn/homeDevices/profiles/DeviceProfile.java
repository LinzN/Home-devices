package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.linzn.simplyConfiguration.provider.YamlConfiguration;

import java.io.File;

public abstract class DeviceProfile {
    private static final File defaultFile = new File(HomeDevicesPlugin.homeDevicesPlugin.getDataFolder(), "profiles/default.yml");
    private static final FileConfiguration defaultProfileConfig = YamlConfiguration.loadConfiguration(defaultFile);
    private final FileConfiguration profileConfig;
    private final String name;
    private final String deviceHardAddress;
    private final String description;
    private final DeviceTechnology deviceTechnology;
    private final MqttDeviceCategory mqttDeviceCategory;
    private final String subDeviceCategory;
    protected MqttDevice mqttDevice;
    private String zigbeeGateway;

    protected DeviceProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        this.profileConfig = profileConfig;
        this.name = name;
        this.deviceHardAddress = deviceHardAddress;
        this.description = description;
        this.deviceTechnology = deviceTechnology;
        this.mqttDeviceCategory = mqttDeviceCategory;
        this.subDeviceCategory = subDeviceCategory;

        if (this.deviceTechnology == DeviceTechnology.ZIGBEE) {
            if (profileConfig.contains("options.zigbeeGateway")) {
                this.zigbeeGateway = profileConfig.getString("options.zigbeeGateway");
            } else {
                this.zigbeeGateway = defaultProfileConfig.getString("options.zigbeeGateway");
            }
        }

    }

    public static DeviceProfile getDeviceProfile(File configFile) {
        FileConfiguration profileConfig = YamlConfiguration.loadConfiguration(configFile);
        String name = profileConfig.getString("deviceInfo.name");
        String deviceHardAddress = profileConfig.getString("deviceInfo.hardwareAddress");
        String description = profileConfig.getString("deviceInfo.description");
        DeviceTechnology deviceTechnology = DeviceTechnology.valueOf(profileConfig.getString("deviceInfo.deviceTechnology"));
        MqttDeviceCategory mqttDeviceCategory = MqttDeviceCategory.valueOf(profileConfig.getString("deviceInfo.deviceCategory"));
        String subDeviceCategory = profileConfig.getString("deviceInfo.subDeviceCategory", "none").toUpperCase();

        DeviceProfile deviceProfile = null;
        if (mqttDeviceCategory == MqttDeviceCategory.SWITCH) {
            deviceProfile = new SwitchDeviceProfile(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
        } else if (mqttDeviceCategory == MqttDeviceCategory.SENSOR) {
            if (SensorCategory.valueOf(subDeviceCategory) == SensorCategory.ENVIRONMENT) {
                deviceProfile = new EnvironmentSensorProfile(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
            }
        } else if (mqttDeviceCategory == MqttDeviceCategory.THERMOSTAT) {
            deviceProfile = new ThermostatDeviceProfile(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
        }

        if (deviceProfile == null) {
            deviceProfile = new DefaultDeviceProfile(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
        }
        return deviceProfile;
    }

    public static FileConfiguration getDefaultConfig() {
        return defaultProfileConfig;
    }

    public MqttDevice getMqttDevice() {
        return mqttDevice;
    }

    public void setMqttDevice(MqttDevice mqttDevice) {
        this.mqttDevice = mqttDevice;
    }

    public FileConfiguration getLoadedConfig() {
        return profileConfig;
    }

    public abstract void loadProfile();

    public abstract void runProfile();

    public abstract boolean changeSmartProfile();

    public String getDeviceHardAddress() {
        return deviceHardAddress;
    }

    public String getDescription() {
        return description;
    }

    public DeviceTechnology getDeviceTechnology() {
        return deviceTechnology;
    }

    public MqttDeviceCategory getMqttDeviceCategory() {
        return mqttDeviceCategory;
    }

    public String getSubDeviceCategory() {
        return subDeviceCategory;
    }

    public String getName() {
        return name;
    }

    public String getZigbeeGateway() {
        return zigbeeGateway;
    }
}
