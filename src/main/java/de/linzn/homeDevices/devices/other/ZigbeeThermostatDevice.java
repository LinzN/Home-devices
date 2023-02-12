package de.linzn.homeDevices.devices.other;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZigbeeThermostatDevice extends MqttDevice {
    private final String zigbeeGatewayMqttName;

    private Date lastCollection;

    private Date healthSwitchDateRequest;
    private AtomicDouble currentTemperature;
    private AtomicBoolean isBatteryLow;

    private String systemMode;
    private AtomicBoolean childLock;
    private AtomicBoolean awayMode;


    public ZigbeeThermostatDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress());
        this.zigbeeGatewayMqttName = deviceProfile.getZigbeeGateway();
    }

    @Override
    public void request_initial_status() {
        STEMSystemApp.LOGGER.WARNING("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.THERMOSTAT.name() + ") is not supported!");
    }

    public void setTemperature(double value) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject temperature = new JSONObject();
        temperature.put("current_heating_setpoint", value);
        mqttMessage.setPayload(temperature.toString().getBytes());
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + getDeviceHardAddress() + "/set", mqttMessage);
    }

    public void setSystemMode(String value) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject temperature = new JSONObject();
        temperature.put("system_mode", value);
        mqttMessage.setPayload(temperature.toString().getBytes());
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + getDeviceHardAddress() + "/set", mqttMessage);
    }

    public void requestTemperature() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject temperature = new JSONObject();
        temperature.put("current_heating_setpoint", "");
        mqttMessage.setPayload(temperature.toString().getBytes());
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + getDeviceHardAddress() + "/get", mqttMessage);
    }

    protected void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        if (jsonObject.has("current_heating_setpoint")) {
            this.currentTemperature = new AtomicDouble(jsonObject.getDouble("current_heating_setpoint"));
        } else {
            this.currentTemperature = new AtomicDouble(-1);
        }
        if (jsonObject.has("away_mode")) {
            this.awayMode = new AtomicBoolean(jsonObject.getString("away_mode").equalsIgnoreCase("ON"));
        } else {
            this.awayMode = new AtomicBoolean(false);
        }
        if (jsonObject.has("child_lock")) {
            this.childLock = new AtomicBoolean(jsonObject.getString("child_lock").equalsIgnoreCase("LOCK"));
        } else {
            this.childLock = new AtomicBoolean(false);
        }

        if (jsonObject.has("battery_low")) {
            this.isBatteryLow = new AtomicBoolean(jsonObject.getBoolean("battery_low"));
        } else {
            this.isBatteryLow = new AtomicBoolean(false);
        }
        if (jsonObject.has("system_mode")) {
            this.systemMode = jsonObject.getString("system_mode");
        } else {
            this.systemMode = "auto";
        }

        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress());
        STEMSystemApp.LOGGER.INFO("DATA: [current_heating_setpoint:" + this.currentTemperature + "], [away_mode:" + this.awayMode + "], [child_lock:" + childLock + "], [system_mode:" + this.systemMode + "]");
    }


    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        this.update_data(jsonPayload);
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
        this.setTemperature(this.currentTemperature.doubleValue());
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(60, ChronoUnit.SECONDS).toEpochMilli() <= this.lastCollection.getTime();
    }

    @Override
    public boolean hasData() {
        return this.currentTemperature != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current_heating_setpoint", this.currentTemperature);
        jsonObject.put("away_mode", this.awayMode);
        jsonObject.put("child_lock", this.childLock);
        jsonObject.put("battery_low", this.isBatteryLow);
        jsonObject.put("system_mode", this.systemMode);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();

        if (jsonInput.has("current_heating_setpoint")) {
            this.setTemperature(jsonInput.getDouble("current_heating_setpoint"));
            jsonObject.put("status", "OK");
        }
        if (jsonInput.has("system_mode")) {
            this.setSystemMode(jsonInput.getString("system_mode"));
            jsonObject.put("status", "OK");
        }
        return jsonObject;
    }

}
