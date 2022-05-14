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
        super(homeDevicesPlugin, deviceHardAddress, description, deviceCategory, configName.toLowerCase(), DeviceBrand.ZIGBEE, "tele/" + zigbeeGatewayMqttName + "/" + deviceHardAddress + "/SENSOR");
        this.zigbeeGatewayMqttName = zigbeeGatewayMqttName;
    }


    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent deviceSwitchEvent = new SwitchDeviceEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceSwitchEvent);

        if (!deviceSwitchEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            mqttMessage.setPayload(("0x" + this.deviceHardAddress).getBytes());
            if (status) {
                mqttMessage.setPayload(("{\"Device\":\"0x" + this.deviceHardAddress + "\",\"Send\":{\"Power\":1}}").getBytes());
            } else {
                mqttMessage.setPayload(("{\"Device\":\"0x" + this.deviceHardAddress + "\",\"Send\":{\"Power\":0}}").getBytes());
            }
            this.mqttModule.publish("cmnd/" + zigbeeGatewayMqttName + "/ZbSend", mqttMessage);
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(toggleDeviceEvent);

        if (!toggleDeviceEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            mqttMessage.setPayload(("{\"Device\":\"0x" + this.deviceHardAddress + "\",\"Send\":{\"Power\":2}}").getBytes());
            this.mqttModule.publish("cmnd/" + zigbeeGatewayMqttName + "/ZbSend", mqttMessage);
        }
    }

    @Override
    protected void request_initial_status() {
        while (this.deviceStatus == null) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(("0x" + this.deviceHardAddress).getBytes());
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + zigbeeGatewayMqttName + "/ZbInfo", mqttMessage);
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
        String key = null;
        JSONObject deviceData = null;

        if (jsonPayload.has("ZbReceived")) {
            key = (String) jsonPayload.getJSONObject("ZbReceived").names().get(0);
            deviceData = jsonPayload.getJSONObject("ZbReceived").getJSONObject(key);
        } else if (jsonPayload.has("ZbInfo")) {
            key = (String) jsonPayload.getJSONObject("ZbInfo").names().get(0);
            deviceData = jsonPayload.getJSONObject("ZbInfo").getJSONObject(key);
        }

        if (deviceData != null) {
            if (deviceData.has("Power")) {
                this.update_status(deviceData.getInt("Power") == 1);
            }
        }

    }
}
