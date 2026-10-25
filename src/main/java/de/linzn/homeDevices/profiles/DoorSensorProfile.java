package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.simplyConfiguration.FileConfiguration;

public class DoorSensorProfile extends DeviceProfile {

    public DoorSensorProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
    }

    @Override
    public void loadProfile() {
    }

    @Override
    public void runProfile() {
    }

    @Override
    public boolean changeSmartProfile() {
        /* nothing to change, a door sensor has no profile dependent settings */
        this.getPlugin().getPluginLogger().debug("Change of smartHome profile for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SENSOR.name() + ", " + this.getDeviceTechnology().name() + ") is not supported!");
        return true;
    }
}
