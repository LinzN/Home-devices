package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.exceptions.DeviceNotInitializedException;
import de.linzn.homeDevices.events.records.DeviceUpdateEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.util.Date;

public abstract class MqttSwitch extends MqttDevice {

    public final SwitchCategory switchCategory;
    /* written by the MQTT thread, read by timers, web api and other plugins */
    private volatile Boolean deviceStatus;
    private volatile Date lastSwitch;
    private volatile Integer brightness;
    private volatile Date healthSwitchDateRequest;

    protected MqttSwitch(STEMPlugin stemPlugin, DeviceProfile deviceProfile, SwitchCategory switchCategory, String mqttTopic) {
        super(stemPlugin, deviceProfile, mqttTopic);
        this.switchCategory = switchCategory;
        this.getLogger().debug("SwitchCategory: " + switchCategory.name());
    }

    protected void update_status(boolean newStatus) {
        this.getLogger().info("DeviceUpdate - ConfigName: " + this.getConfigName() + " DeviceHardAddress: " + this.getDeviceHardAddress() + " DATA: [status:" + newStatus + "]");
        this.deviceStatus = newStatus;
        this.lastSwitch = new Date();
        this.fireEvent(new DeviceUpdateEvent(this, newStatus));
        DeviceWrapperListener.updateStatus(this.getHomeDevicesPlugin(), this.getConfigName(), newStatus);
    }

    protected void update_brightness(int brightness) {
        this.brightness = brightness;
        this.getLogger().debug("DATA: [" + this.getConfigName() + " brightness:" + brightness + "]");
    }

    public boolean getDeviceStatus() throws DeviceNotInitializedException {
        Boolean status = this.deviceStatus;
        if (status == null) {
            throw new DeviceNotInitializedException();
        }
        return status;
    }

    /**
     * @return true if the device is known to be switched on, false if it is off or has no data yet
     */
    public boolean isSwitchedOn() {
        return Boolean.TRUE.equals(this.deviceStatus);
    }

    /**
     * @return time of the last status update from the device, null if no data has arrived yet
     */
    public Date getLastSwitch() {
        return this.lastSwitch;
    }

    public int getBrightness() throws DeviceNotInitializedException {
        Integer value = this.brightness;
        if (value == null) {
            throw new DeviceNotInitializedException();
        }
        return value;
    }

    public abstract void setBrightness(int brightness);

    public abstract void switchDevice(boolean status);

    public abstract void toggleDevice();

    public abstract boolean isDimmable();

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
        Boolean status = this.deviceStatus;
        if (status != null) {
            /* re-send the current status, the device answers with a status update */
            this.switchDevice(status);
        }
    }

    @Override
    public boolean healthCheckStatus() {
        Date last = this.lastSwitch;
        Date request = this.healthSwitchDateRequest;
        return last != null && request != null && last.getTime() >= request.getTime();
    }

    @Override
    public boolean hasData() {
        return this.deviceStatus != null;
    }

    public SwitchCategory getSwitchCategory() {
        return switchCategory;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        Boolean status = this.deviceStatus;
        Integer currentBrightness = this.brightness;
        jsonObject.put("status", status != null ? status : "error");
        if (this.isDimmable()) {
            jsonObject.put("brightness", currentBrightness != null ? currentBrightness : "error");
        }
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "ERROR");
        if (jsonInput.has("status")) {
            boolean requestStatus = jsonInput.getBoolean("status");
            this.switchDevice(requestStatus);
            jsonObject.put("status", "OK");
        }
        if (jsonInput.has("brightness")) {
            int value = jsonInput.getInt("brightness");
            if (value < 0 || value > 255) {
                jsonObject.put("status", "ERROR");
                jsonObject.put("error", "brightness must be between 0 and 255");
                return jsonObject;
            }
            this.setBrightness(value);
            jsonObject.put("status", "OK");
        }
        return jsonObject;
    }
}
