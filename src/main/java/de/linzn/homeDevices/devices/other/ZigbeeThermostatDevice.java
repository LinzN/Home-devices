package de.linzn.homeDevices.devices.other;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class ZigbeeThermostatDevice extends MqttDevice {
    private final String zigbeeGatewayMqttName;

    private AtomicDouble currentTemperature;
    private AtomicBoolean isBatteryLow;

    private String systemMode;
    private AtomicBoolean childLock;
    private AtomicBoolean awayMode;


    public ZigbeeThermostatDevice(STEMPlugin stemPlugin, String deviceHardAddress, String description, String configName, String zigbeeGatewayMqttName) {
        super(stemPlugin, deviceHardAddress, description, configName, DeviceTechnology.ZIGBEE, zigbeeGatewayMqttName + "/" + deviceHardAddress);
        this.zigbeeGatewayMqttName = zigbeeGatewayMqttName;
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
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/set", mqttMessage);
    }

    public void setSystemMode(String value) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject temperature = new JSONObject();
        temperature.put("system_mode", value);
        mqttMessage.setPayload(temperature.toString().getBytes());
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/set", mqttMessage);
    }

    public void requestTemperature() {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject temperature = new JSONObject();
        temperature.put("current_heating_setpoint", "");
        mqttMessage.setPayload(temperature.toString().getBytes());
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/get", mqttMessage);
    }

    protected void update_data(JSONObject jsonObject) {
        this.currentTemperature = new AtomicDouble(jsonObject.getDouble("current_heating_setpoint"));
        this.awayMode = new AtomicBoolean(jsonObject.getString("away_mode").equalsIgnoreCase("ON"));
        this.childLock = new AtomicBoolean(jsonObject.getString("child_lock").equalsIgnoreCase("LOCK"));
        this.isBatteryLow = new AtomicBoolean(jsonObject.getBoolean("battery_low"));
        this.systemMode = jsonObject.getString("system_mode");
        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + configName + " DeviceHardAddress: " + deviceHardAddress);
        STEMSystemApp.LOGGER.INFO("DATA: [current_heating_setpoint:" + this.currentTemperature + "], [away_mode:" + this.awayMode + "], [child_lock:" + childLock + "], [system_mode:" + this.systemMode + "]");
    }


    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        this.update_data(jsonPayload);
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
