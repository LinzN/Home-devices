package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;


public abstract class MqttSensor extends MqttDevice {

    public final SensorCategory sensorCategory;

    public MqttSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile, SensorCategory sensorCategory, String mqttTopic) {
        super(stemPlugin, deviceProfile, mqttTopic);
        this.sensorCategory = sensorCategory;
        STEMApp.LOGGER.CONFIG("SensorCategory: " + sensorCategory.name());
    }

    public SensorCategory getSensorCategory() {
        return this.sensorCategory;
    }
}
