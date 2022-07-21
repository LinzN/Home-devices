package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.SwitchDeviceEvent;
import de.linzn.homeDevices.events.ToggleDeviceDeviceEvent;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class ZigbeeSwitchDevice extends MqttSwitch {

    private final String zigbeeGatewayMqttName;

    public ZigbeeSwitchDevice(STEMPlugin stemPlugin, String configName, String deviceHardAddress, SwitchCategory switchCategory, String description, String zigbeeGatewayMqttName) {
        super(stemPlugin, deviceHardAddress, description, switchCategory, configName.toLowerCase(), DeviceTechnology.ZIGBEE, zigbeeGatewayMqttName + "/" + deviceHardAddress);
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
    public boolean isDimmable() {
        return true;
    }

    @Override
    protected void request_initial_status() {
        while (!this.hasData()) {
            JSONObject state = new JSONObject();
            state.put("state", "");
            state.put("brightness", "");
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(state.toString().getBytes());
            mqttMessage.setQos(2);
            this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/get", mqttMessage);
            STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SWITCH.name() + ", " + this.deviceTechnology.name() + ")");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void setBrightness(int brightness) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("brightness", brightness);
        jsonObject.put("transition", 1);
        mqttMessage.setPayload(jsonObject.toString().getBytes());
        this.mqttModule.publish(zigbeeGatewayMqttName + "/" + deviceHardAddress + "/set", mqttMessage);

    }


    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("state")) {
            this.update_status(jsonPayload.getString("state").equalsIgnoreCase("ON"));
        }
        if (jsonPayload.has("brightness")) {
            this.update_brightness(jsonPayload.getInt("brightness"));
        }
    }

}
