package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.BooleanAdapter;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.informationModule.InformationBlock;
import de.linzn.stem.modules.informationModule.InformationIntent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class DoorSensor extends MqttDevice {
    private Date lastCollection;
    private Date healthSwitchDateRequest;
    private AtomicBoolean doorClosed;

    private InformationBlock informationBlock;

    public DoorSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    public void request_initial_status() {
        STEMApp.LOGGER.WARNING("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.THERMOSTAT.name() + ") is not supported!");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Contact", 0);
        this.update_data(jsonObject);
    }

    protected void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        this.doorClosed = new AtomicBoolean(!BooleanAdapter.adapt(jsonObject.getInt("Contact")));

        STEMApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress());
        STEMApp.LOGGER.INFO("DATA: [door_closed:" + this.doorClosed.get() + "]");

        if (!this.doorClosed.get()) {
            if (informationBlock == null || !informationBlock.isActive()) {
                informationBlock = new InformationBlock("DOOR", "Door is open!", HomeDevicesPlugin.homeDevicesPlugin);
                informationBlock.setIcon("DOOR");
                informationBlock.setExpireTime(-1L);
                informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
                STEMApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
            } else {
                informationBlock.setDescription("Door is open!");
                informationBlock.setExpireTime(-1L);
            }
        } else {
            if (informationBlock != null) {
                informationBlock.setDescription("Door is closed!");
                Instant expireDate = TimeAdapter.getTimeInstant().plus(2, ChronoUnit.MINUTES);
                informationBlock.setExpireTime(expireDate);
            }
        }
    }


    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        if (jsonPayload.has("ZbReceived")) {
            if (jsonPayload.getJSONObject("ZbReceived").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbReceived").getJSONObject(deviceProfile.getDeviceHardAddress());
                if (data.has("Contact")) {
                    this.update_data(data);
                }
            }
        }
        if (jsonPayload.has("ZbInfo")) {
            if (jsonPayload.getJSONObject("ZbInfo").has(deviceProfile.getDeviceHardAddress())) {
                JSONObject data = jsonPayload.getJSONObject("ZbInfo").getJSONObject(deviceProfile.getDeviceHardAddress());
                if (data.has("Contact")) {
                    this.update_data(data);
                }
            }
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(2, ChronoUnit.HOURS).toEpochMilli() <= this.lastCollection.getTime();
    }

    @Override
    public boolean hasData() {
        return this.doorClosed != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("door_closed", this.doorClosed.get());
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return null;
    }

}
