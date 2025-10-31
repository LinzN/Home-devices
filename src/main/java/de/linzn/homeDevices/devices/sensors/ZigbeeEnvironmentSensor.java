package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class ZigbeeEnvironmentSensor extends EnvironmentSensor {

    public ZigbeeEnvironmentSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    public void request_initial_status() {

        int counter = 0;
        while (!this.hasData() && counter <= 30) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            mqttMessage.setPayload(this.getDeviceHardAddress().getBytes());
            mqttModule.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/zbinfo", mqttMessage);
            STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SENSOR.name() + ", " + this.getDeviceTechnology().name() + ")");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            counter++;
        }
        if (counter > 30) {
            STEMSystemApp.LOGGER.WARNING("Seems device " + this.getDeviceHardAddress() + " is disconnected!");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Temperature", 0);
            jsonObject.put("Humidity", 0);
            jsonObject.put("BatteryPercentage", 100);
            this.update_data(jsonObject);
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
        if (jsonPayload.has("ZbInfo")) {
            if (jsonPayload.getJSONObject("ZbInfo").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbInfo").getJSONObject(deviceProfile.getDeviceHardAddress());
                this.update_data(data);
            }
        }
    }
}
