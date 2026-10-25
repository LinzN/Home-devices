package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.BooleanAdapter;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.stem.modules.informationModule.InformationBlock;
import de.linzn.stem.modules.informationModule.InformationIntent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DoorSensor extends MqttDevice {

    private volatile Date lastCollection;
    private volatile Date healthSwitchDateRequest;
    private volatile Boolean doorClosed;
    /* guarded by this */
    private InformationBlock informationBlock;

    public DoorSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    public void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.SENSOR.name() + ") is not supported!");
        if (!this.hasData()) {
            /* assume closed until the sensor reports */
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Contact", 0);
            this.update_data(jsonObject);
        }
    }

    protected synchronized void update_data(JSONObject jsonObject) {
        this.lastCollection = new Date();
        boolean closed = !BooleanAdapter.adapt(jsonObject.getInt("Contact"));
        this.doorClosed = closed;
        this.getLogger().info("DeviceUpdate - ConfigName: " + getConfigName() + " DeviceHardAddress: " + getDeviceHardAddress() + " DATA: [door_closed:" + closed + "]");
        if (!closed) {
            if (informationBlock == null || !informationBlock.isActive()) {
                informationBlock = new InformationBlock("DOOR", "Door is open!", this.getHomeDevicesPlugin());
                informationBlock.setIcon("DOOR");
                informationBlock.setExpireTime(-1L);
                informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
                this.getHomeDevicesPlugin().getStemApi().information().queue(informationBlock);
            } else {
                informationBlock.setDescription("Door is open!");
                informationBlock.setExpireTime(-1L);
            }
        } else {
            if (informationBlock != null) {
                informationBlock.setDescription("Door is closed!");
                /* kept, so a reopening within the next minutes reuses the same block */
                informationBlock.setExpireTime(TimeAdapter.getTimeInstant().plus(2, ChronoUnit.MINUTES));
            }
        }
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        String address = deviceProfile.getDeviceHardAddress();
        for (String key : new String[]{"ZbReceived", "ZbInfo"}) {
            JSONObject section = jsonPayload.optJSONObject(key);
            JSONObject data = section != null ? section.optJSONObject(address) : null;
            if (data != null && data.has("Contact")) {
                this.update_data(data);
            }
        }
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastCollection, this.healthSwitchDateRequest, Duration.ofHours(2));
    }

    @Override
    public boolean hasData() {
        return this.doorClosed != null;
    }

    public boolean isDoorClosed() {
        return !Boolean.FALSE.equals(this.doorClosed);
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        Boolean closed = this.doorClosed;
        jsonObject.put("door_closed", closed != null ? closed : "error");
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        return notSupported();
    }
}
