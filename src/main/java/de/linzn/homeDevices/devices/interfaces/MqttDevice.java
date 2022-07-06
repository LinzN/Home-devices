package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class MqttDevice implements IMqttMessageListener {

    public final String description;
    public final String configName;
    public final String deviceHardAddress;
    public final DeviceTechnology deviceTechnology;
    private final STEMPlugin stemPlugin;
    private final String topic;
    protected MqttModule mqttModule;

    public MqttDevice(STEMPlugin stemPlugin, String deviceHardAddress, String description, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        this.stemPlugin = stemPlugin;
        this.deviceTechnology = deviceTechnology;
        this.configName = configName.toLowerCase();
        this.deviceHardAddress = deviceHardAddress;
        this.description = description;
        this.topic = mqttTopic;
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe(topic, this);
        STEMSystemApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
        STEMSystemApp.LOGGER.CONFIG("Register mqttDevice:" + this.configName);
        STEMSystemApp.LOGGER.CONFIG("Description: " + description);
        STEMSystemApp.LOGGER.CONFIG("DeviceTechnology: " + deviceTechnology.name());
        STEMSystemApp.LOGGER.CONFIG("DeviceHardAddress: " + deviceHardAddress);
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getDeviceHardAddress() {
        return deviceHardAddress;
    }

    public DeviceTechnology getDeviceTechnology() {
        return this.deviceTechnology;
    }

    public String getDescription() {
        return this.description;
    }

    public STEMPlugin getHomeDevicesPlugin() {
        return stemPlugin;
    }

    protected abstract void request_initial_status();

    @Override
    public abstract void messageArrived(String s, MqttMessage mqttMessage);

    public abstract boolean hasData();

}
