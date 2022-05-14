package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.DeviceBrand;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.SensorCategory;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class MqttSensor implements IMqttMessageListener {

    public final String description;
    public final String configName;
    public final String deviceHardAddress;
    public final SensorCategory sensorCategory;
    public final DeviceBrand deviceBrand;
    private final HomeDevicesPlugin homeDevicesPlugin;
    private final String topic;
    protected MqttModule mqttModule;

    public MqttSensor(HomeDevicesPlugin homeDevicesPlugin, String deviceHardAddress, String description, SensorCategory sensorCategory, String configName, DeviceBrand deviceBrand, String mqttTopic) {
        this.homeDevicesPlugin = homeDevicesPlugin;
        this.deviceBrand = deviceBrand;
        this.configName = configName.toLowerCase();
        this.deviceHardAddress = deviceHardAddress;
        this.sensorCategory = sensorCategory;
        this.description = description;
        this.topic = mqttTopic;
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe(topic, this);
        STEMSystemApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getDeviceHardAddress() {
        return deviceHardAddress;
    }

    public abstract void request_initial_status();

    @Override
    public abstract void messageArrived(String s, MqttMessage mqttMessage);

    public abstract boolean hasData();
}
