package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.stem.stemSystem.STEMSystemApp;

public class EnvironmentSensorProfile extends DeviceProfile {

    public EnvironmentSensorProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
    }

    @Override
    public void loadProfile() {
        EnvironmentSensor sensor = (EnvironmentSensor) this.getMqttDevice();

        String optionPath = "offset";

        if (this.hasOwnConfig() && this.getLoadedConfig().contains(optionPath)) {
            double offsetTemperature = this.getLoadedConfig().getDouble(optionPath + ".temperature");
            double offsetHumidity = this.getLoadedConfig().getDouble(optionPath + ".humidity");

            sensor.setOffsetTemperature(offsetTemperature);
            sensor.setOffsetHumidity(offsetHumidity);
            STEMSystemApp.LOGGER.CONFIG("Load sensor offset values for hardId " + this.getMqttDevice().getDeviceHardAddress() + " configName " + this.getMqttDevice().getConfigName());
        }

    }

    @Override
    public void runProfile() {

    }
}
