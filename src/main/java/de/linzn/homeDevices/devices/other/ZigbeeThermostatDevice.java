package de.linzn.homeDevices.devices.other;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.homeDevices.profiles.ThermostatDeviceProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Date;

public class ZigbeeThermostatDevice extends MqttDevice {

    private volatile Date lastCollection;
    private volatile Date healthSwitchDateRequest;
    /* target temperature as reported by the device, NaN until known */
    private volatile double currentTemperature = Double.NaN;

    public ZigbeeThermostatDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    public void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.THERMOSTAT.name() + ") is not supported!");
        if (!this.hasData() && this.getDeviceProfile() instanceof ThermostatDeviceProfile thermostatDeviceProfile) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("TuyaTempTarget", thermostatDeviceProfile.getCurrentConfigTemperature());
            this.update_data(jsonObject);
        }
    }

    public void setTemperature(double value) {
        if (Double.isNaN(value) || value < 0 || value > 50) {
            this.getLogger().warning("Ignore invalid thermostat temperature " + value + " for " + this.getConfigName());
            return;
        }
        JSONObject messagePayload = new JSONObject();
        messagePayload.put("device", deviceProfile.getDeviceHardAddress());
        JSONObject zbCommand = new JSONObject();
        zbCommand.put("TuyaTempTarget", value * 10);
        messagePayload.put("write", zbCommand);
        this.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zbsend", messagePayload.toString());
    }

    protected void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        if (jsonObject.has("TuyaTempTarget")) {
            this.currentTemperature = jsonObject.getDouble("TuyaTempTarget");
            this.getLogger().info("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress() + " DATA: [TuyaTempTarget:" + this.currentTemperature + "]");
        }
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        JSONObject section = jsonPayload.optJSONObject("ZbReceived");
        JSONObject data = section != null ? section.optJSONObject(deviceProfile.getDeviceHardAddress()) : null;
        if (data != null) {
            this.update_data(data);
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
        double temp = this.currentTemperature;
        if (Double.isNaN(temp)) {
            return;
        }
        /* writing the current value makes the device report back; sent twice as zigbee sleepy devices drop single writes */
        this.setTemperature(temp);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        this.setTemperature(temp);
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastCollection, this.healthSwitchDateRequest, Duration.ofMinutes(120));
    }

    @Override
    public boolean hasData() {
        return !Double.isNaN(this.currentTemperature);
    }

    public double getCurrentTemperature() {
        return this.currentTemperature;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        double temp = this.currentTemperature;
        jsonObject.put("current_heating_setpoint", Double.isNaN(temp) ? "error" : temp);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "ERROR");
        if (jsonInput.has("current_heating_setpoint")) {
            this.setTemperature(jsonInput.getDouble("current_heating_setpoint"));
            jsonObject.put("status", "OK");
        }
        return jsonObject;
    }
}
