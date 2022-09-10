package de.linzn.homeDevices.devices.interfaces;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.profiles.EnvironmentSensorProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

public abstract class EnvironmentSensor extends MqttSensor {

    private AtomicDouble temperature;
    private AtomicDouble humidity;
    private AtomicDouble battery;

    private double offsetTemperature = 0D;
    private double offsetHumidity = 0D;

    public EnvironmentSensor(STEMPlugin stemPlugin, String deviceHardAddress, String description, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        super(stemPlugin, deviceHardAddress, description, SensorCategory.ENVIRONMENT, configName, deviceTechnology, mqttTopic);
        this.setDeviceProfile(new EnvironmentSensorProfile(this));
        this.getDeviceProfile().loadProfile();
        this.getDeviceProfile().runProfile();
    }

    protected void update_data(JSONObject jsonObject) {
        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + configName + " DeviceHardAddress: " + deviceHardAddress);
        this.temperature = new AtomicDouble(jsonObject.getDouble("temperature"));
        this.humidity = new AtomicDouble(jsonObject.getDouble("humidity"));
        this.battery = new AtomicDouble(jsonObject.getDouble("battery"));
        STEMSystemApp.LOGGER.INFO("DATA: [temperature:" + this.temperature + "], [humidity:" + this.humidity + "], [battery:" + battery + "]");
    }

    public AtomicDouble getTemperature() {
        return new AtomicDouble(this.temperature.doubleValue() + this.offsetTemperature);
    }

    public AtomicDouble getHumidity() {
        return new AtomicDouble(this.humidity.doubleValue() + this.offsetHumidity);
    }

    public AtomicDouble getBattery() {
        return battery;
    }

    public void setOffsetTemperature(double offsetTemperature){
        this.offsetTemperature = offsetTemperature;
    }
    public void setOffsetHumidity(double offsetHumidity){
        this.offsetHumidity = offsetHumidity;
    }

    @Override
    public boolean hasData() {
        return this.temperature != null & this.humidity != null & this.battery != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("temperature", this.getTemperature());
        jsonObject.put("humidity", this.getHumidity());
        jsonObject.put("battery", this.getBattery());
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return null;
    }
}
