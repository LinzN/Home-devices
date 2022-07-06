package de.linzn.homeDevices.devices.interfaces;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

public abstract class EnvironmentSensor extends MqttSensor {

    private AtomicDouble temperature;
    private AtomicDouble humidity;
    private AtomicDouble battery;

    public EnvironmentSensor(STEMPlugin stemPlugin, String deviceHardAddress, String description, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        super(stemPlugin, deviceHardAddress, description, SensorCategory.ENVIRONMENT, configName, deviceTechnology, mqttTopic);
    }

    protected void update_data(JSONObject jsonObject) {
        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + configName + " DeviceHardAddress: " + deviceHardAddress);
        this.temperature = new AtomicDouble(jsonObject.getDouble("temperature"));
        this.humidity = new AtomicDouble(jsonObject.getDouble("humidity"));
        this.battery = new AtomicDouble(jsonObject.getDouble("battery"));
        STEMSystemApp.LOGGER.INFO("DATA: [temperature:" + this.temperature + "], [humidity:" + this.humidity + "], [battery:" + battery + "]");
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
