package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.events.records.EnvironmentSensorUpdateDataEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Date;

public abstract class EnvironmentSensor extends MqttSensor {

    /* raw values as reported by the device, NaN until the first value arrived */
    private volatile double temperature = Double.NaN;
    private volatile double humidity = Double.NaN;
    private volatile double battery = Double.NaN;
    private volatile Date lastCollection;
    private volatile Date healthSwitchDateRequest;
    private volatile double offsetTemperature = 0D;
    private volatile double offsetHumidity = 0D;

    public EnvironmentSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile, String mqttTopic) {
        super(stemPlugin, deviceProfile, SensorCategory.ENVIRONMENT, mqttTopic);
    }

    protected void update_data(JSONObject jsonObject) {
        if (jsonObject.has("Temperature")) {
            this.temperature = jsonObject.getDouble("Temperature");
        }
        if (jsonObject.has("Humidity")) {
            this.humidity = jsonObject.getDouble("Humidity");
        }
        if (jsonObject.has("BatteryPercentage")) {
            this.battery = jsonObject.getDouble("BatteryPercentage");
        }
        Date collection = new Date();
        this.lastCollection = collection;
        this.getLogger().debug("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress()
                + " DATA: [temperature:" + this.temperature + "], [humidity:" + this.humidity + "], [battery:" + this.battery + "]");
        this.fireEvent(new EnvironmentSensorUpdateDataEvent(this, collection));
    }

    /**
     * @return temperature including the configured offset, NaN if no value has arrived yet
     */
    public double getTemperature() {
        return this.temperature + this.offsetTemperature;
    }

    /**
     * @return humidity including the configured offset, NaN if no value has arrived yet
     */
    public double getHumidity() {
        return this.humidity + this.offsetHumidity;
    }

    /**
     * @return battery in percent, NaN if no value has arrived yet
     */
    public double getBattery() {
        return battery;
    }

    public Date getLastCollection() {
        return this.lastCollection;
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
        return receivedSince(this.lastCollection, this.healthSwitchDateRequest, Duration.ofHours(1));
    }

    @Override
    public boolean hasData() {
        return !Double.isNaN(this.temperature) && !Double.isNaN(this.humidity) && !Double.isNaN(this.battery);
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        if (!this.hasData()) {
            jsonObject.put("status", "error");
            return jsonObject;
        }
        jsonObject.put("temperature", this.getTemperature());
        jsonObject.put("humidity", this.getHumidity());
        jsonObject.put("battery", this.getBattery());
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        return notSupported();
    }
}
