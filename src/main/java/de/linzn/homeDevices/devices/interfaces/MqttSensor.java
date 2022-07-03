package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;


public abstract class MqttSensor extends MqttDevice {

    public final SensorCategory sensorCategory;

    public MqttSensor(STEMPlugin stemPlugin, String deviceHardAddress, String description, SensorCategory sensorCategory, String configName, DeviceTechnology deviceTechnology, String mqttTopic) {
        super(stemPlugin, deviceHardAddress, description, configName, deviceTechnology, mqttTopic);
        this.sensorCategory = sensorCategory;
    }

    public SensorCategory getSensorCategory() {
        return this.sensorCategory;
    }
}
