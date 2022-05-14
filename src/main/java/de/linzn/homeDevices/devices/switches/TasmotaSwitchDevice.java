package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.DeviceBrand;
import de.linzn.homeDevices.DeviceCategory;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.events.MQTTUpdateDeviceEvent;
import de.linzn.homeDevices.events.SwitchDeviceEvent;
import de.linzn.homeDevices.events.ToggleDeviceDeviceEvent;
import de.stem.stemSystem.STEMSystemApp;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class TasmotaSwitchDevice extends SwitchableMQTTDevice {


    public TasmotaSwitchDevice(HomeDevicesPlugin homeDevicesPlugin, String configName, String deviceHardAddress, DeviceCategory deviceCategory, String description) {
        super(homeDevicesPlugin, deviceHardAddress, description, deviceCategory, configName.toLowerCase(), DeviceBrand.TASMOTA, "stat/" + deviceHardAddress + "/RESULT");
    }


    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent tasmotaSwitchEvent = new SwitchDeviceEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(tasmotaSwitchEvent);

        if (!tasmotaSwitchEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            if (status) {
                mqttMessage.setPayload("ON".getBytes());
            } else {
                mqttMessage.setPayload("OFF".getBytes());
            }
            this.mqttModule.publish("cmnd/" + this.deviceHardAddress + "/Power", mqttMessage);
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(toggleDeviceEvent);

        if (!toggleDeviceEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            mqttMessage.setPayload("TOGGLE".getBytes());
            this.mqttModule.publish("cmnd/" + this.deviceHardAddress + "/Power", mqttMessage);
        }
    }

    @Override
    protected void request_initial_status() {
        while (this.deviceStatus == null) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + this.deviceHardAddress + "/Power", mqttMessage);
            STEMSystemApp.LOGGER.INFO("MQTT initialization request for hardId: " + this.deviceHardAddress + " configName: " + this.configName);
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
        boolean status = jsonPayload.getString("POWER").equalsIgnoreCase("ON");
        final MQTTUpdateDeviceEvent mqttUpdateDeviceEvent = new MQTTUpdateDeviceEvent(this, status);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(mqttUpdateDeviceEvent);
        this.update_status(status);
    }
}
