package de.linzn.homeDevices.devices.other;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class ZigbeeThermostatDevice implements IMqttMessageListener {
    private HomeDevicesPlugin homeDevicesPlugin;
    private String deviceHardAddress;
    private String description;
    private String configName;
    private String zigbeeGatewayMqttName;
    protected MqttModule mqttModule;

    private AtomicDouble currentTemperature;
    private AtomicBoolean isBatteryLow;
    private AtomicBoolean childLock;
    private AtomicBoolean awayMode;


    public ZigbeeThermostatDevice(HomeDevicesPlugin homeDevicesPlugin, String deviceHardAddress, String description, String configName, String zigbeeGatewayMqttName) {
        this.homeDevicesPlugin = homeDevicesPlugin;
        this.deviceHardAddress = deviceHardAddress;
        this.description = description;
        this.configName = configName;
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe(zigbeeGatewayMqttName + "/" + deviceHardAddress, this);
        STEMSystemApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
    }

    public void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " is not fully supported!");
        requestTemperature();
    }

    public void setTemperature(double value) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject temperature = new JSONObject();
        temperature.put("current_heating_setpoint", value);
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

    protected void update_temperature(double value) {
        if (this.currentTemperature == null) {
            this.currentTemperature = new AtomicDouble(value);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee temperature: " + this.currentTemperature);
        } else {
            this.currentTemperature.set(value);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee temperature: " + this.currentTemperature);
        }
    }

    protected void update_awayMode(boolean value) {
        if (this.awayMode == null) {
            this.awayMode = new AtomicBoolean(value);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee awayMode: " + this.awayMode);
        } else {
            this.awayMode.set(value);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee awayMode: " + this.awayMode);
        }
    }

    protected void update_childLock(boolean value) {
        if (this.childLock == null) {
            this.childLock = new AtomicBoolean(value);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee childLock: " + this.childLock);
        } else {
            this.childLock.set(value);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee childLock: " + this.childLock);
        }
    }

    protected void update_batteryLow(boolean value) {
        if (this.isBatteryLow == null) {
            this.isBatteryLow = new AtomicBoolean(value);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee isBatteryLow: " + this.isBatteryLow);
        } else {
            this.isBatteryLow.set(value);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: zigbee isBatteryLow: " + this.isBatteryLow);
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        if (jsonPayload.has("current_heating_setpoint")) {
            this.update_temperature(jsonPayload.getDouble("current_heating_setpoint"));
        }
        if (jsonPayload.has("away_mode")) {
            this.update_awayMode(jsonPayload.getString("away_mode").equalsIgnoreCase("ON"));
        }
        if (jsonPayload.has("child_lock")) {
            this.update_childLock(jsonPayload.getString("child_lock").equalsIgnoreCase("LOCK"));
        }
        if (jsonPayload.has("battery_low")) {
            this.update_batteryLow(jsonPayload.getBoolean("battery_low"));
        }
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getDeviceHardAddress() {
        return deviceHardAddress;
    }

    public boolean hasData() {
        return this.currentTemperature != null;
    }
}
