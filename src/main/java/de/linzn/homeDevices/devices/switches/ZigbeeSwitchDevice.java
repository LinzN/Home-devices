package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.cancelable.SwitchDeviceEvent;
import de.linzn.homeDevices.events.cancelable.ToggleDeviceDeviceEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.BooleanAdapter;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class ZigbeeSwitchDevice extends MqttSwitch {


    public ZigbeeSwitchDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, SwitchCategory.valueOf(deviceProfile.getSubDeviceCategory()), "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }


    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent deviceSwitchEvent = new SwitchDeviceEvent(this);
        STEMApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceSwitchEvent);

        if (!deviceSwitchEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            JSONObject messagePayload = new JSONObject();
            messagePayload.put("device", deviceProfile.getDeviceHardAddress());
            JSONObject zbCommand = new JSONObject();
            if (status) {
                zbCommand.put("power", "true");
            } else {
                zbCommand.put("power", "false");
            }
            messagePayload.put("send", zbCommand);
            mqttMessage.setPayload(messagePayload.toString().getBytes());
            this.mqttModule.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zbsend", mqttMessage);
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        STEMApp.getInstance().getEventModule().getStemEventBus().fireEvent(toggleDeviceEvent);

        if (!toggleDeviceEvent.isCanceled()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(2);
            JSONObject messagePayload = new JSONObject();
            messagePayload.put("device", deviceProfile.getDeviceHardAddress());
            JSONObject zbCommand = new JSONObject();
            zbCommand.put("power", "TOGGLE");
            messagePayload.put("send", zbCommand);
            mqttMessage.setPayload(messagePayload.toString().getBytes());
            this.mqttModule.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zbsend", mqttMessage);
        }
    }

    @Override
    public boolean isDimmable() {
        return true;
    }

    @Override
    protected void request_initial_status() {
        int counter = 0;
        while (!this.hasData() && counter <= 30) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(this.getDeviceHardAddress().getBytes());
            mqttMessage.setQos(2);
            this.mqttModule.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zblight", mqttMessage);
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
    public void setBrightness(int brightness) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        JSONObject messagePayload = new JSONObject();
        messagePayload.put("device", deviceProfile.getDeviceHardAddress());
        JSONObject zbCommand = new JSONObject();
        zbCommand.put("dimmer", brightness);
        messagePayload.put("send", zbCommand);
        mqttMessage.setPayload(messagePayload.toString().getBytes());
        this.mqttModule.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zbsend", mqttMessage);
    }


    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("ZbReceived")) {
            if (jsonPayload.getJSONObject("ZbReceived").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbReceived").getJSONObject(deviceProfile.getDeviceHardAddress());
                if (data.has("Power")) {
                    this.update_status(BooleanAdapter.adapt(data.getInt("Power")));
                }
                if (data.has("Dimmer")) {
                    this.update_brightness(data.getInt("Dimmer"));
                }
            }
        }
        if (jsonPayload.has("ZbLight")) {
            if (jsonPayload.getJSONObject("ZbLight").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbLight").getJSONObject(deviceProfile.getDeviceHardAddress());
                if (data.has("Power")) {
                    this.update_status(BooleanAdapter.adapt(data.getInt("Power")));
                }
                if (data.has("Dimmer")) {
                    this.update_brightness(data.getInt("Dimmer"));
                }
            }
        }
        if (jsonPayload.has("ZbInfo")) {
            if (jsonPayload.getJSONObject("ZbInfo").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbInfo").getJSONObject(deviceProfile.getDeviceHardAddress());
                if (data.has("Power")) {
                    this.update_status(BooleanAdapter.adapt(data.getInt("Power")));
                }
                if (data.has("Dimmer")) {
                    this.update_brightness(data.getInt("Dimmer"));
                }
            }
        }
    }
}
