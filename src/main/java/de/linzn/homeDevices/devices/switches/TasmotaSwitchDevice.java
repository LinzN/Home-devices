package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.cancelable.SwitchDeviceEvent;
import de.linzn.homeDevices.events.cancelable.ToggleDeviceDeviceEvent;
import de.linzn.homeDevices.events.records.MQTTUpdateDeviceEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class TasmotaSwitchDevice extends MqttSwitch {


    public TasmotaSwitchDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, SwitchCategory.valueOf(deviceProfile.getSubDeviceCategory()), "stat/" + deviceProfile.getDeviceHardAddress() + "/RESULT");
    }


    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent tasmotaSwitchEvent = new SwitchDeviceEvent(this);
        STEMApp.getInstance().getEventModule().getStemEventBus().fireEvent(tasmotaSwitchEvent);

        if (!tasmotaSwitchEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            if (status) {
                mqttMessage.setPayload("ON".getBytes());
            } else {
                mqttMessage.setPayload("OFF".getBytes());
            }
            this.mqttModule.publish("cmnd/" + this.getDeviceHardAddress() + "/Power", mqttMessage);
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        STEMApp.getInstance().getEventModule().getStemEventBus().fireEvent(toggleDeviceEvent);

        if (!toggleDeviceEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            mqttMessage.setPayload("TOGGLE".getBytes());
            this.mqttModule.publish("cmnd/" + this.getDeviceHardAddress() + "/Power", mqttMessage);
        }
    }

    @Override
    public void setBrightness(int brightness) {
        // not supported
    }

    @Override
    public boolean isDimmable() {
        return false;
    }

    @Override
    protected void request_initial_status() {
        int counter = 0;
        while (!this.hasData() && counter <= 30) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + this.getDeviceHardAddress() + "/Power", mqttMessage);
            STEMApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SWITCH.name() + ", " + this.getDeviceTechnology().name() + ")");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            counter++;
        }
        if (counter > 30) {
            STEMApp.LOGGER.WARNING("Seems device " + this.getDeviceHardAddress() + " is disconnected!");
        }
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        boolean status = jsonPayload.getString("POWER").equalsIgnoreCase("ON");
        final MQTTUpdateDeviceEvent mqttUpdateDeviceEvent = new MQTTUpdateDeviceEvent(this, status);
        STEMApp.getInstance().getEventModule().getStemEventBus().fireEvent(mqttUpdateDeviceEvent);
        this.update_status(status);
    }
}
