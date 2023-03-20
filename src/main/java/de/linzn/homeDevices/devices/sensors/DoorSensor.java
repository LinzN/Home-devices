package de.linzn.homeDevices.devices.sensors;

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.informationModule.InformationBlock;
import de.stem.stemSystem.modules.informationModule.InformationIntent;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class DoorSensor extends MqttDevice {
    private final String zigbeeGatewayMqttName;
    private Date lastCollection;
    private Date healthSwitchDateRequest;
    private AtomicBoolean doorClosed;

    private InformationBlock informationBlock;
    public DoorSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress());
        this.zigbeeGatewayMqttName = deviceProfile.getZigbeeGateway();
    }

    @Override
    public void request_initial_status() {
        STEMSystemApp.LOGGER.WARNING("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.THERMOSTAT.name() + ") is not supported!");
    }

    protected void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        if (jsonObject.has("contact")) {
            this.doorClosed = new AtomicBoolean(jsonObject.getBoolean("contact"));
        }

        STEMSystemApp.LOGGER.INFO("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress());
        STEMSystemApp.LOGGER.INFO("DATA: [door_closed:" + this.doorClosed.get() + "]");

        if(!this.doorClosed.get()) {
            if (informationBlock == null || !informationBlock.isActive()) {
                informationBlock = new InformationBlock("DOOR", "Door is open!", HomeDevicesPlugin.homeDevicesPlugin);
                informationBlock.setIcon("DOOR");
                informationBlock.setExpireTime(-1L);
                informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
                STEMSystemApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
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
        this.update_data(jsonPayload);
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
