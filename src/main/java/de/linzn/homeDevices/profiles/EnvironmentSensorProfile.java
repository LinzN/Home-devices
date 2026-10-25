package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.simplyConfiguration.FileConfiguration;

public class EnvironmentSensorProfile extends DeviceProfile {

    public EnvironmentSensorProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
    }

    @Override
    public void loadProfile() {
        if (!(this.getMqttDevice() instanceof EnvironmentSensor sensor)) {
            return;
        }
        String optionPath = "offset";
        if (this.getLoadedConfig().contains(optionPath)) {
            double offsetTemperature = this.getLoadedConfig().getDouble(optionPath + ".temperature");
            double offsetHumidity = this.getLoadedConfig().getDouble(optionPath + ".humidity");
            sensor.setOffsetTemperature(offsetTemperature);
            sensor.setOffsetHumidity(offsetHumidity);
            this.getPlugin().getPluginLogger().debug("Load sensor offset values for hardId " + sensor.getDeviceHardAddress() + " configName " + sensor.getConfigName());
        }
    }

    @Override
    public void runProfile() {
    }

    @Override
    public boolean changeSmartProfile() {
        /* nothing to change, offsets do not depend on the smart home profile */
        this.getPlugin().getPluginLogger().debug("Change of smartHome profile for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SENSOR.name() + ", " + this.getDeviceTechnology().name() + ") is not supported!");
        return true;
    }
}
