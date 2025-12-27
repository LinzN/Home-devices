package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.mqttModule.MqttModule;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
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
        this.mqttModule = STEMApp.getInstance().getMqttModule();
        this.mqttModule.subscribe(topic, this);
        STEMApp.getInstance().getScheduler().runTask(HomeDevicesPlugin.homeDevicesPlugin, this::request_initial_status);
        STEMApp.LOGGER.CONFIG("Register mqttDevice:" + deviceProfile.getName());
        STEMApp.LOGGER.CONFIG("Description: " + deviceProfile.getDescription());
        STEMApp.LOGGER.CONFIG("DeviceTechnology: " + deviceProfile.getDeviceTechnology().name());
        STEMApp.LOGGER.CONFIG("SubDeviceCategory: " + deviceProfile.getSubDeviceCategory());
        STEMApp.LOGGER.CONFIG("DeviceHardAddress: " + deviceProfile.getDeviceHardAddress());
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
    public void messageArrived(String s, MqttMessage mqttMessage) {
        try {
            this.mqttMessageEvent(mqttMessage);
        } catch (Exception e) {
            STEMApp.LOGGER.ERROR("Catch error in mqtt data call! Prevent thread freeze");
            STEMApp.LOGGER.ERROR(e);
        }
    }

    public abstract void mqttMessageEvent(MqttMessage mqttMessage);

    public abstract void requestHealthCheck();

    public abstract boolean healthCheckStatus();

    public abstract boolean hasData();

    public abstract JSONObject getJSONData();

    public abstract JSONObject setJSONData(JSONObject jsonInput);

}
