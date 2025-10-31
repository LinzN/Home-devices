package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.exceptions.DeviceNotInitializedException;
import de.linzn.homeDevices.events.records.DeviceUpdateEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MqttSwitch extends MqttDevice {

    public final SwitchCategory switchCategory;
    public AtomicBoolean deviceStatus;
    public Date lastSwitch;
    protected AtomicInteger brightness;

    private Date healthSwitchDateRequest;

    protected MqttSwitch(STEMPlugin stemPlugin, DeviceProfile deviceProfile, SwitchCategory switchCategory, String mqttTopic) {
        super(stemPlugin, deviceProfile, mqttTopic);
        this.switchCategory = switchCategory;
        STEMSystemApp.LOGGER.CONFIG("SwitchCategory: " + switchCategory.name());
    }

    protected void update_status(boolean newStatus) {
        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + this.getConfigName() + " DeviceHardAddress: " + this.getDeviceHardAddress());
        this.deviceStatus = new AtomicBoolean(newStatus);
        this.lastSwitch = new Date();
        DeviceUpdateEvent deviceUpdateEvent = new DeviceUpdateEvent(this, newStatus);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceUpdateEvent);
        STEMSystemApp.LOGGER.INFO("DATA: [status:" + newStatus + "]");
        DeviceWrapperListener.updateStatus(this.getConfigName(), this.deviceStatus.get());
    }

    protected void update_brightness(int brightness) {
        this.brightness = new AtomicInteger(brightness);
        STEMSystemApp.LOGGER.INFO("DATA: [brightness:" + brightness + "]");
    }

    public boolean getDeviceStatus() throws DeviceNotInitializedException {
        if (this.deviceStatus != null) {
            return this.deviceStatus.get();
        } else {
            throw new DeviceNotInitializedException();
        }
    }

    public int getBrightness() throws DeviceNotInitializedException {
        if (this.brightness != null) {
            return this.brightness.get();
        } else {
            throw new DeviceNotInitializedException();
        }
    }

    public abstract void setBrightness(int brightness);

    public abstract void switchDevice(boolean status);

    public abstract void toggleDevice();

    public abstract boolean isDimmable();

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
        if (this.deviceStatus != null) {
            this.switchDevice(this.deviceStatus.get());
        }
    }

    @Override
    public boolean healthCheckStatus() {
        if (this.lastSwitch != null) {
            return this.lastSwitch.getTime() >= this.healthSwitchDateRequest.getTime();
        } else {
            return false;
        }
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
        try {
            jsonObject.put("status", this.getDeviceStatus());
            if (this.isDimmable()) {
                jsonObject.put("brightness", this.getBrightness());
            }
        } catch (DeviceNotInitializedException e) {
            jsonObject.put("status", "error");
            if (this.isDimmable()) {
                jsonObject.put("brightness", "error");
            }
            //STEMSystemApp.LOGGER.WARNING(e.getMessage());
        }

        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        if (jsonInput.has("status")) {
            boolean requestStatus = jsonInput.getBoolean("status");
            this.switchDevice(requestStatus);
            jsonObject.put("status", "OK");
        }
        if (jsonInput.has("brightness")) {
            this.setBrightness(jsonInput.getInt("brightness"));
            jsonObject.put("status", "OK");
        }
        return jsonObject;
    }
}
