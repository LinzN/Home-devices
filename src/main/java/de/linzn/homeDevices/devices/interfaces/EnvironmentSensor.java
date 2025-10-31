package de.linzn.homeDevices.devices.interfaces;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.events.records.EnvironmentSensorUpdateDataEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public abstract class EnvironmentSensor extends MqttSensor {

    private AtomicDouble temperature;
    private AtomicDouble humidity;
    private AtomicDouble battery;

    private Date lastCollection;

    private Date healthSwitchDateRequest;

    private double offsetTemperature = 0D;
    private double offsetHumidity = 0D;

    public EnvironmentSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile, String mqttTopic) {
        super(stemPlugin, deviceProfile, SensorCategory.ENVIRONMENT, mqttTopic);
    }

    protected void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress());
        if (jsonObject.has("Temperature")) {
            this.temperature = new AtomicDouble(jsonObject.getDouble("Temperature"));
        }
        if (jsonObject.has("Humidity")) {
            this.humidity = new AtomicDouble(jsonObject.getDouble("Humidity"));
        }
        if (jsonObject.has("BatteryPercentage")) {
            this.battery = new AtomicDouble(jsonObject.getDouble("BatteryPercentage"));
        }
        STEMSystemApp.LOGGER.DEBUG("DATA: [temperature:" + this.temperature + "], [humidity:" + this.humidity + "], [battery:" + battery + "]");

        EnvironmentSensorUpdateDataEvent deviceUpdateEvent = new EnvironmentSensorUpdateDataEvent(this, this.lastCollection);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceUpdateEvent);
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

    public void setOffsetTemperature(double offsetTemperature) {
        this.offsetTemperature = offsetTemperature;
    }

    public void setOffsetHumidity(double offsetHumidity) {
        this.offsetHumidity = offsetHumidity;
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(1, ChronoUnit.HOURS).toEpochMilli() <= this.lastCollection.getTime();
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
