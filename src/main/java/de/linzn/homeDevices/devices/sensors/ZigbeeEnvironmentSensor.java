package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class ZigbeeEnvironmentSensor extends EnvironmentSensor {
    private final String zigbeeGatewayMqttName;

    public ZigbeeEnvironmentSensor(STEMPlugin stemPlugin, String deviceHardAddress, String description, String configName, String zigbeeGatewayMqttName) {
        super(stemPlugin, deviceHardAddress, description, configName, DeviceTechnology.ZIGBEE, zigbeeGatewayMqttName + "/" + deviceHardAddress);
        this.zigbeeGatewayMqttName = zigbeeGatewayMqttName;
    }

    @Override
    public void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " is not supported!");
        this.update_humidity(0);
        this.update_batteryPercentage(0);
        this.update_temperature(0);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("temperature")) {
            this.update_temperature(jsonPayload.getDouble("temperature"));
        }
        if (jsonPayload.has("humidity")) {
            this.update_humidity(jsonPayload.getDouble("humidity"));
        }
        if (jsonPayload.has("battery")) {
            this.update_batteryPercentage(jsonPayload.getDouble("battery"));
        }
    }
}
