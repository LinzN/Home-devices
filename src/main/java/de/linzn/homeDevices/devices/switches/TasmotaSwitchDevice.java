package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.cancelable.SwitchDeviceEvent;
import de.linzn.homeDevices.events.cancelable.ToggleDeviceDeviceEvent;
import de.linzn.homeDevices.events.records.MQTTUpdateDeviceEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

public class TasmotaSwitchDevice extends MqttSwitch {

    public TasmotaSwitchDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, SwitchCategory.valueOf(deviceProfile.getSubDeviceCategory()), "stat/" + deviceProfile.getDeviceHardAddress() + "/RESULT");
    }

    private String powerTopic() {
        return "cmnd/" + this.getDeviceHardAddress() + "/Power";
    }

    @Override
    public void switchDevice(boolean status) {
        SwitchDeviceEvent tasmotaSwitchEvent = new SwitchDeviceEvent(this);
        this.fireEvent(tasmotaSwitchEvent);
        if (!tasmotaSwitchEvent.isCanceled()) {
            this.publish(this.powerTopic(), status ? "ON" : "OFF");
        }
    }

    @Override
    public void toggleDevice() {
        ToggleDeviceDeviceEvent toggleDeviceEvent = new ToggleDeviceDeviceEvent(this);
        this.fireEvent(toggleDeviceEvent);
        if (!toggleDeviceEvent.isCanceled()) {
            this.publish(this.powerTopic(), "TOGGLE");
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
        /* an empty Power command makes tasmota report the current state */
        this.requestUntilData(() -> this.publish(this.powerTopic(), ""), MqttDeviceCategory.SWITCH.name());
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        /* RESULT is also used for other commands, only power results are relevant */
        String power = jsonPayload.optString("POWER", null);
        if (power == null) {
            return;
        }
        boolean status = power.equalsIgnoreCase("ON");
        this.fireEvent(new MQTTUpdateDeviceEvent(this, status));
        this.update_status(status);
    }
}
