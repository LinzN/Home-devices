package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.AutoStartStopTimer;
import de.linzn.homeDevices.AutoSwitchOffTimer;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.events.DeviceUpdateEvent;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MqttSwitch extends MqttDevice {


    public final SwitchCategory switchCategory;
    private final AutoSwitchOffTimer autoSwitchOffTimer;
    private final AutoStartStopTimer autoStartStopTimer;
    public AtomicBoolean deviceStatus;
    public Date lastSwitch;

    protected MqttSwitch(STEMPlugin stemPlugin, String deviceHardAddress, String description, SwitchCategory switchCategory, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        super(stemPlugin, deviceHardAddress, description, configName, deviceTechnology, mqttTopic);
        this.switchCategory = switchCategory;
        this.autoSwitchOffTimer = new AutoSwitchOffTimer(this);
        this.autoStartStopTimer = new AutoStartStopTimer(this);
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, autoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, autoStartStopTimer, 2, TimeUnit.SECONDS);
    }

    protected void update_status(boolean newStatus) {
        if (this.deviceStatus == null) {
            this.deviceStatus = new AtomicBoolean(newStatus);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " status: " + this.deviceStatus);
        } else {
            this.deviceStatus.set(newStatus);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceTechnology.name() + " status: " + this.deviceStatus);
        }
        this.lastSwitch = new Date();
        DeviceUpdateEvent deviceUpdateEvent = new DeviceUpdateEvent(this, newStatus);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceUpdateEvent);

        DeviceWrapperListener.updateStatus(this.configName, this.deviceStatus.get());
    }

    public boolean getDeviceStatus() {
        return this.deviceStatus.get();
    }

    public abstract void switchDevice(boolean status);

    public abstract void toggleDevice();

    @Override
    public boolean hasData() {
        return this.deviceStatus != null;
    }

    public SwitchCategory getSwitchCategory() {
        return switchCategory;
    }

}
