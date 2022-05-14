package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.DeviceBrand;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.stem.stemSystem.STEMSystemApp;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class ZigbeeEnvironmentSensor extends EnvironmentSensor {
    private final String zigbeeGatewayMqttName;

    public ZigbeeEnvironmentSensor(HomeDevicesPlugin homeDevicesPlugin, String deviceHardAddress, String description, String configName, String zigbeeGatewayMqttName) {
        super(homeDevicesPlugin, deviceHardAddress, description, configName, DeviceBrand.ZIGBEE, "tele/" + zigbeeGatewayMqttName + "/" + deviceHardAddress + "/SENSOR");
        this.zigbeeGatewayMqttName = zigbeeGatewayMqttName;
    }

    @Override
    public void request_initial_status() {
        while (!this.hasData()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(("0x" + this.getDeviceHardAddress()).getBytes());
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + zigbeeGatewayMqttName + "/ZbInfo", mqttMessage);
            STEMSystemApp.LOGGER.INFO("MQTT initialization request for sensor: " + this.deviceHardAddress + " configName: " + this.configName);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        String key;
        JSONObject sensor = null;

        if (jsonPayload.has("ZbReceived")) {
            key = (String) jsonPayload.getJSONObject("ZbReceived").names().get(0);
            sensor = jsonPayload.getJSONObject("ZbReceived").getJSONObject(key);
        } else if (jsonPayload.has("ZbInfo")) {
            key = (String) jsonPayload.getJSONObject("ZbInfo").names().get(0);
            sensor = jsonPayload.getJSONObject("ZbInfo").getJSONObject(key);
        }

        if (sensor != null) {
            if (sensor.has("Temperature")) {
                this.update_temperature(sensor.getDouble("Temperature"));
            }
            if (sensor.has("Humidity")) {
                this.update_humidity(sensor.getDouble("Humidity"));
            }
            if (sensor.has("BatteryPercentage")) {
                this.update_batteryPercentage(sensor.getDouble("BatteryPercentage"));
            }
        }
    }
}
