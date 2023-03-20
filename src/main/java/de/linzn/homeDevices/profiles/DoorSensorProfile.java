package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.homeDevices.devices.sensors.DoorSensor;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.stem.stemSystem.STEMSystemApp;

public class DoorSensorProfile extends DeviceProfile {

    public DoorSensorProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
    }

    @Override
    public void loadProfile() {
        DoorSensor sensor = (DoorSensor) this.getMqttDevice();

    }

    @Override
    public void runProfile() {

    }

    @Override
    public boolean changeSmartProfile() {
        STEMSystemApp.LOGGER.WARNING("Change of smartHome profile for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SENSOR.name() + ", " + this.getDeviceTechnology().name() + ") is not supported!");
        return true;
    }
}
