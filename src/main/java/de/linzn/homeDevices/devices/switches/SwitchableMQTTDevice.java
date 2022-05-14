package de.linzn.homeDevices.devices.switches;

import de.linzn.homeDevices.*;
import de.linzn.homeDevices.events.DeviceUpdateEvent;
import de.linzn.homeDevices.stemLink.DeviceWrapperListener;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SwitchableMQTTDevice implements IMqttMessageListener {

    public final String description;
    public final String configName;
    public final String deviceHardAddress;
    public final DeviceCategory deviceCategory;
    public final DeviceBrand deviceBrand;
    protected final MqttModule mqttModule;
    private final HomeDevicesPlugin homeDevicesPlugin;
    private final String topic;
    private final AutoSwitchOffTimer autoSwitchOffTimer;
    private final AutoStartStopTimer autoStartStopTimer;
    public AtomicBoolean deviceStatus;
    public Date lastSwitch;

    protected SwitchableMQTTDevice(HomeDevicesPlugin homeDevicesPlugin, String deviceHardAddress, String description, DeviceCategory deviceCategory, String configName, DeviceBrand deviceBrand, String mqttTopic) {
        this.homeDevicesPlugin = homeDevicesPlugin;
        this.deviceBrand = deviceBrand;
        this.configName = configName.toLowerCase();
        this.deviceHardAddress = deviceHardAddress;
        this.deviceCategory = deviceCategory;
        this.description = description;
        this.topic = mqttTopic;
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe(topic, this);
        this.autoSwitchOffTimer = new AutoSwitchOffTimer(this);
        this.autoStartStopTimer = new AutoStartStopTimer(this);
        STEMSystemApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, autoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, autoStartStopTimer, 2, TimeUnit.SECONDS);
        STEMSystemApp.LOGGER.CONFIG("Register new mqtt " + this.deviceBrand.name() + " device with configName: " + this.configName + ", hardId: " + this.deviceHardAddress + " and category: " + this.deviceCategory.name());
        STEMSystemApp.LOGGER.CONFIG("Description: " + description);
    }

    protected void update_status(boolean newStatus) {
        if (this.deviceStatus == null) {
            this.deviceStatus = new AtomicBoolean(newStatus);
            STEMSystemApp.LOGGER.INFO("MQTT initialization hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceBrand.name() + " status: " + this.deviceStatus);
        } else {
            this.deviceStatus.set(newStatus);
            STEMSystemApp.LOGGER.INFO("Update hardId: " + this.deviceHardAddress + " configName: " + this.configName + " deviceBrand: " + this.deviceBrand.name() + " status: " + this.deviceStatus);
        }
        this.lastSwitch = new Date();
        DeviceUpdateEvent deviceUpdateEvent = new DeviceUpdateEvent(this, newStatus);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(deviceUpdateEvent);

        DeviceWrapperListener.updateStatus(this.configName, this.deviceStatus.get());
    }

    public boolean getDeviceStatus() {
        return this.deviceStatus.get();
    }

    public String getConfigName() {
        return configName;
    }

    public String getDeviceHardAddress() {
        return this.deviceHardAddress;
    }

    public String getDescription() {
        return this.description;
    }

    public DeviceCategory getDeviceCategory() {
        return deviceCategory;
    }

    public abstract void switchDevice(boolean status);

    public abstract void toggleDevice();

    protected abstract void request_initial_status();

    @Override
    public abstract void messageArrived(String s, MqttMessage mqttMessage);
}
