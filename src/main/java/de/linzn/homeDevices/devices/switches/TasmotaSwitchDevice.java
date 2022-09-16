package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.MQTTUpdateDeviceEvent;
import de.linzn.homeDevices.events.SwitchDeviceEvent;
import de.linzn.homeDevices.events.ToggleDeviceDeviceEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class TasmotaSwitchDevice extends MqttSwitch {


    public TasmotaSwitchDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, SwitchCategory.valueOf(deviceProfile.getSubDeviceCategory()), "stat/" + deviceProfile.getDeviceHardAddress() + "/RESULT");
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
            this.mqttModule.publish("cmnd/" + this.getDeviceHardAddress() + "/Power", mqttMessage);
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
        while (!this.hasData()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + this.getDeviceHardAddress() + "/Power", mqttMessage);
            STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SWITCH.name() + ", " + this.getDeviceTechnology().name() + ")");
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
