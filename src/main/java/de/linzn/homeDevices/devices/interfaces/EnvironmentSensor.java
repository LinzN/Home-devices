package de.linzn.homeDevices.devices.interfaces;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

public abstract class EnvironmentSensor extends MqttSensor {

    private AtomicDouble temperature;
    private AtomicDouble humidity;
    private AtomicDouble battery;

    public EnvironmentSensor(STEMPlugin stemPlugin, String deviceHardAddress, String description, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        super(stemPlugin, deviceHardAddress, description, SensorCategory.ENVIRONMENT, configName, deviceTechnology, mqttTopic);
    }


    protected void update_temperature(double temperature) {
        if (this.temperature == null) {
            STEMSystemApp.LOGGER.INFO("MQTT initialization sensor hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " temperature: " + temperature);
            this.temperature = new AtomicDouble(temperature);
        } else {
            STEMSystemApp.LOGGER.INFO("Update sensor hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " temperature: " + temperature);
            this.temperature.set(temperature);
        }
    }

    protected void update_humidity(double humidity) {
        if (this.humidity == null) {
            STEMSystemApp.LOGGER.INFO("MQTT initialization sensor hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " humidity: " + humidity);
            this.humidity = new AtomicDouble(humidity);
        } else {
            STEMSystemApp.LOGGER.INFO("Update sensor hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " humidity: " + humidity);
            this.humidity.set(humidity);
        }
    }

    protected void update_batteryPercentage(double battery) {
        if (this.battery == null) {
            STEMSystemApp.LOGGER.INFO("MQTT initialization sensor hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " battery: " + battery);
            this.battery = new AtomicDouble(battery);
        } else {
            STEMSystemApp.LOGGER.INFO("Update sensor hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " battery: " + battery);
            this.battery.set(battery);
        }
    }

    public AtomicDouble getTemperature() {
        return temperature;
    }

    public AtomicDouble getHumidity() {
        return humidity;
    }

    public AtomicDouble getBattery() {
        return battery;
    }

    @Override
    public boolean hasData() {
        return this.temperature != null & this.humidity != null & this.battery != null;
    }
}
