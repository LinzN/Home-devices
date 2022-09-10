package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.stem.stemSystem.STEMSystemApp;

public class EnvironmentSensorProfile extends DeviceProfile{

    public EnvironmentSensorProfile(MqttDevice mqttDevice) {
        super(mqttDevice);
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
