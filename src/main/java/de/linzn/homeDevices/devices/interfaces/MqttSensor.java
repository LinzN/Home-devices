package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;


public abstract class MqttSensor extends MqttDevice {

    public final SensorCategory sensorCategory;

    public MqttSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile, SensorCategory sensorCategory, String mqttTopic) {
        super(stemPlugin, deviceProfile, mqttTopic);
        this.sensorCategory = sensorCategory;
        STEMSystemApp.LOGGER.CONFIG("SensorCategory: " + sensorCategory.name());
    }

    public SensorCategory getSensorCategory() {
        return this.sensorCategory;
    }
}
