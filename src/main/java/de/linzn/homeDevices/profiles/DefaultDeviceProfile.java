package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.simplyConfiguration.FileConfiguration;

public class DefaultDeviceProfile extends DeviceProfile {

    public DefaultDeviceProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);

    }

    @Override
    public void loadProfile() {

    }

    @Override
    public void runProfile() {

    }
}