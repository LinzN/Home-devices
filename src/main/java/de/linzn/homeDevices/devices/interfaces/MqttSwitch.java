package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.AutoStartStopTimer;
import de.linzn.homeDevices.AutoSwitchOffTimer;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.exceptions.DeviceNotInitializedException;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.events.DeviceUpdateEvent;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MqttSwitch extends MqttDevice {


    public final SwitchCategory switchCategory;
    private final AutoSwitchOffTimer autoSwitchOffTimer;
    private final AutoStartStopTimer autoStartStopTimer;
    public AtomicBoolean deviceStatus;
    public Date lastSwitch;
    protected AtomicInteger brightness;

    protected MqttSwitch(STEMPlugin stemPlugin, String deviceHardAddress, String description, SwitchCategory switchCategory, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        super(stemPlugin, deviceHardAddress, description, configName, deviceTechnology, mqttTopic);
        this.switchCategory = switchCategory;
        STEMSystemApp.LOGGER.CONFIG("SwitchCategory: " + switchCategory.name());
        this.autoSwitchOffTimer = new AutoSwitchOffTimer(this);
        this.autoStartStopTimer = new AutoStartStopTimer(this);
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, autoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, autoStartStopTimer, 2, TimeUnit.SECONDS);
    }

    protected void update_status(boolean newStatus) {
        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + configName + " DeviceHardAddress: " + deviceHardAddress);
        this.deviceStatus = new AtomicBoolean(newStatus);
        this.lastSwitch = new Date();
        DeviceUpdateEvent deviceUpdateEvent = new DeviceUpdateEvent(this, newStatus);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceUpdateEvent);
        STEMSystemApp.LOGGER.INFO("DATA: [status:" + newStatus + "]");
        DeviceWrapperListener.updateStatus(this.configName, this.deviceStatus.get());
    }

    protected void update_brightness(int brightness) {
        this.brightness = new AtomicInteger(brightness);
        STEMSystemApp.LOGGER.INFO("DATA: [brightness:" + brightness + "]");
    }

    public boolean getDeviceStatus() throws DeviceNotInitializedException {
        if(this.deviceStatus != null){
            return this.deviceStatus.get();
        } else {
            throw new DeviceNotInitializedException();
        }
    }

    public int getBrightness() throws DeviceNotInitializedException {
        if(this.brightness != null) {
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
            e.printStackTrace();
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
