package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public abstract class MqttDevice implements IMqttMessageListener {
    private final STEMPlugin stemPlugin;
    private final String topic;
    public DeviceProfile deviceProfile;
    protected MqttModule mqttModule;

    public MqttDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile, String mqttTopic) {
        this.stemPlugin = stemPlugin;
        this.deviceProfile = deviceProfile;
        this.topic = mqttTopic;
        this.mqttModule = STEMSystemApp.getInstance().getMqttModule();
        this.mqttModule.subscribe(topic, this);
        STEMSystemApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
        STEMSystemApp.LOGGER.CONFIG("Register mqttDevice:" + deviceProfile.getName());
        STEMSystemApp.LOGGER.CONFIG("Description: " + deviceProfile.getDescription());
        STEMSystemApp.LOGGER.CONFIG("DeviceTechnology: " + deviceProfile.getDeviceTechnology().name());
        STEMSystemApp.LOGGER.CONFIG("SubDeviceCategory: " + deviceProfile.getSubDeviceCategory());
        STEMSystemApp.LOGGER.CONFIG("DeviceHardAddress: " + deviceProfile.getDeviceHardAddress());
    }

    public String getConfigName() {
        return this.deviceProfile.getName();
    }

    public String getDeviceHardAddress() {
        return this.deviceProfile.getDeviceHardAddress();
    }

    public DeviceTechnology getDeviceTechnology() {
        return this.deviceProfile.getDeviceTechnology();
    }

    public String getDescription() {
        return this.deviceProfile.getDescription();
    }

    public STEMPlugin getHomeDevicesPlugin() {
        return stemPlugin;
    }

    public DeviceProfile getDeviceProfile() {
        return this.deviceProfile;
    }

    protected abstract void request_initial_status();

    @Override
    public abstract void messageArrived(String s, MqttMessage mqttMessage);

    public abstract boolean hasData();

    public abstract JSONObject getJSONData();

    public abstract JSONObject setJSONData(JSONObject jsonInput);

}
