package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.cancelable.SwitchDeviceEvent;
import de.linzn.homeDevices.events.cancelable.ToggleDeviceDeviceEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.BooleanAdapter;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

public class ZigbeeSwitchDevice extends MqttSwitch {

    private static final String[] DATA_KEYS = {"ZbReceived", "ZbLight", "ZbInfo"};

    public ZigbeeSwitchDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, SwitchCategory.valueOf(deviceProfile.getSubDeviceCategory()), "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    private void sendZbCommand(JSONObject zbCommand) {
        JSONObject messagePayload = new JSONObject();
        messagePayload.put("device", deviceProfile.getDeviceHardAddress());
        messagePayload.put("send", zbCommand);
        this.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zbsend", messagePayload.toString());
    }

    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent deviceSwitchEvent = new SwitchDeviceEvent(this);
        this.fireEvent(deviceSwitchEvent);
        if (!deviceSwitchEvent.isCanceled()) {
            JSONObject zbCommand = new JSONObject();
            zbCommand.put("power", status ? "true" : "false");
            this.sendZbCommand(zbCommand);
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        this.fireEvent(toggleDeviceEvent);
        if (!toggleDeviceEvent.isCanceled()) {
            JSONObject zbCommand = new JSONObject();
            zbCommand.put("power", "TOGGLE");
            this.sendZbCommand(zbCommand);
        }
    }

    @Override
    public boolean isDimmable() {
        return true;
    }

    @Override
    protected void request_initial_status() {
        this.requestUntilData(() -> this.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/" + this.getDeviceHardAddress() + "/zblight", this.getDeviceHardAddress()),
                MqttDeviceCategory.SWITCH.name());
    }

    @Override
    public void setBrightness(int brightness) {
        JSONObject zbCommand = new JSONObject();
        zbCommand.put("dimmer", brightness);
        this.sendZbCommand(zbCommand);
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        String address = deviceProfile.getDeviceHardAddress();
        for (String key : DATA_KEYS) {
            JSONObject section = jsonPayload.optJSONObject(key);
            if (section == null) {
                continue;
            }
            JSONObject data = section.optJSONObject(address);
            if (data == null) {
                continue;
            }
            if (data.has("Power")) {
                this.update_status(BooleanAdapter.adapt(data.getInt("Power")));
            }
            if (data.has("Dimmer")) {
                this.update_brightness(data.getInt("Dimmer"));
            }
        }
    }
}
