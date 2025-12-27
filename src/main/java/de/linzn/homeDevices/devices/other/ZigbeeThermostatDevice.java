package de.linzn.homeDevices.devices.other;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.homeDevices.profiles.ThermostatDeviceProfile;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ZigbeeThermostatDevice extends MqttDevice {
    private Date lastCollection;
    private Date healthSwitchDateRequest;
    private AtomicDouble currentTemperature;


    public ZigbeeThermostatDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    public void request_initial_status() {
        STEMApp.LOGGER.WARNING("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.THERMOSTAT.name() + ") is not supported!");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("TuyaTempTarget", ((ThermostatDeviceProfile) this.getDeviceProfile()).getCurrentConfigTemperature());
        this.update_data(jsonObject);
    }

    public void setTemperature(double value) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject messagePayload = new JSONObject();
        messagePayload.put("device", deviceProfile.getDeviceHardAddress());
        JSONObject zbCommand = new JSONObject();
        zbCommand.put("TuyaTempTarget", value * 10);
        messagePayload.put("write", zbCommand);
        mqttMessage.setPayload(messagePayload.toString().getBytes());
        this.mqttModule.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zbsend", mqttMessage);
    }

    protected void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        if (jsonObject.has("TuyaTempTarget")) {
            this.currentTemperature = new AtomicDouble(jsonObject.getDouble("TuyaTempTarget"));
            STEMApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress());
            STEMApp.LOGGER.INFO("DATA: [TuyaTempTarget:" + this.currentTemperature + "]");
        }
    }


    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        if (jsonPayload.has("ZbReceived")) {
            if (jsonPayload.getJSONObject("ZbReceived").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbReceived").getJSONObject(deviceProfile.getDeviceHardAddress());
                this.update_data(data);
            }
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
        double temp = this.currentTemperature.get();
        this.setTemperature(temp);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        this.setTemperature(temp);
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(120, ChronoUnit.MINUTES).toEpochMilli() <= this.lastCollection.getTime();
    }

    @Override
    public boolean hasData() {
        return this.currentTemperature != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current_heating_setpoint", this.currentTemperature);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();

        if (jsonInput.has("current_heating_setpoint")) {
            this.setTemperature(jsonInput.getDouble("current_heating_setpoint"));
            jsonObject.put("status", "OK");
        }
        return jsonObject;
    }

}
