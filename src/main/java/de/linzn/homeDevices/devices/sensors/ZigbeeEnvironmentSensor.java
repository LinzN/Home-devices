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
        super(stemPlugin, deviceProfile, deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress());
    }

    @Override
    public void request_initial_status() {
        STEMSystemApp.LOGGER.WARNING("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SENSOR.name() + ", " + this.getDeviceTechnology().name() + ") is not supported!");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("temperature", 0);
        jsonObject.put("humidity", 0);
        jsonObject.put("battery", 100);
        this.update_data(jsonObject);
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        this.update_data(jsonPayload);
    }
}
