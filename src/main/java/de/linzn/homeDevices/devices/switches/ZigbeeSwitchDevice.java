package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.DeviceBrand;
import de.linzn.homeDevices.DeviceCategory;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.events.SwitchDeviceEvent;
import de.linzn.homeDevices.events.ToggleDeviceDeviceEvent;
import de.stem.stemSystem.STEMSystemApp;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class ZigbeeSwitchDevice extends SwitchableMQTTDevice {

    private String zigbeeGatewayMqttName;

    public ZigbeeSwitchDevice(HomeDevicesPlugin homeDevicesPlugin, String configName, String deviceHardAddress, DeviceCategory deviceCategory, String description, String zigbeeGatewayMqttName) {
        super(homeDevicesPlugin, deviceHardAddress, description, deviceCategory, configName.toLowerCase(), DeviceBrand.ZIGBEE, zigbeeGatewayMqttName + "/" + deviceHardAddress);
        this.zigbeeGatewayMqttName = zigbeeGatewayMqttName;
    }


    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent deviceSwitchEvent = new SwitchDeviceEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceSwitchEvent);

        if (!deviceSwitchEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            JSONObject state = new JSONObject();
            if (status) {
                state.put("state", "ON");
            } else {
                state.put("state", "OFF");
            }
            mqttMessage.setPayload(state.toString().getBytes());
            this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/set", mqttMessage);
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(toggleDeviceEvent);

        if (!toggleDeviceEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            JSONObject state = new JSONObject();
            state.put("state", "TOGGLE");
            mqttMessage.setPayload(state.toString().getBytes());
            this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/set", mqttMessage);
        }
    }

    @Override
    protected void request_initial_status() {
        while (this.deviceStatus == null) {
            JSONObject state = new JSONObject();
            state.put("state", "");
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(state.toString().getBytes());
            mqttMessage.setQos(2);
            this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/get", mqttMessage);
            STEMSystemApp.LOGGER.INFO("MQTT initialization request for device: " + this.deviceHardAddress);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("state")) {
            this.update_status(jsonPayload.getString("state").equalsIgnoreCase("ON"));
        }
    }
}
