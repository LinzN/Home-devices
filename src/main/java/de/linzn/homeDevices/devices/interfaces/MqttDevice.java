package de.linzn.homeDevices.devices.interfaces;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.api.StemLogger;
import de.linzn.stem.api.mqtt.MqttSubscription;
import de.linzn.stem.modules.eventModule.StemEvent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class MqttDevice {

    /* QoS used for all commands sent to devices */
    protected static final int COMMAND_QOS = 2;

    private final STEMPlugin stemPlugin;
    private final List<String> topics;
    private final List<MqttSubscription> subscriptions;
    protected final DeviceProfile deviceProfile;
    private volatile boolean running;

    public MqttDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile, String mqttTopic) {
        this.stemPlugin = stemPlugin;
        this.deviceProfile = deviceProfile;
        this.topics = new ArrayList<>();
        this.subscriptions = new CopyOnWriteArrayList<>();
        this.topics.add(mqttTopic);
        StemLogger logger = stemPlugin.getPluginLogger();
        logger.debug("Register mqttDevice:" + deviceProfile.getName());
        logger.debug("Description: " + deviceProfile.getDescription());
        logger.debug("DeviceTechnology: " + deviceProfile.getDeviceTechnology().name());
        logger.debug("SubDeviceCategory: " + deviceProfile.getSubDeviceCategory());
        logger.debug("DeviceHardAddress: " + deviceProfile.getDeviceHardAddress());
    }

    /**
     * Adds a further topic this device listens to. Must be called from the constructor, the
     * subscriptions are created by {@link #start()}.
     */
    protected final void addTopic(String topic) {
        this.topics.add(topic);
    }

    /**
     * Subscribes to all topics and requests the initial status. Called by the device manager once
     * the device and its profile are fully initialized, so no message can arrive at a half built object.
     */
    public void start() {
        if (this.running) {
            return;
        }
        this.running = true;
        for (String topic : this.topics) {
            try {
                this.subscriptions.add(this.stemPlugin.getStemApi().mqtt().subscribe(topic, this::messageArrived));
            } catch (Exception e) {
                this.getLogger().error("Unable to subscribe to " + topic + " for device " + this.getConfigName(), e);
            }
        }
        this.stemPlugin.getStemApi().scheduler().runAsync(() -> {
            try {
                this.request_initial_status();
            } catch (Exception e) {
                this.getLogger().error("Initial status request failed for device " + this.getConfigName(), e);
            }
        });
    }

    /**
     * Removes the MQTT subscriptions of this device.
     */
    public void stop() {
        this.running = false;
        for (MqttSubscription subscription : this.subscriptions) {
            try {
                subscription.unsubscribe();
            } catch (Exception ignored) {
                /* client may already be disconnected */
            }
        }
        this.subscriptions.clear();
    }

    public boolean isRunning() {
        return this.running;
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

    protected StemLogger getLogger() {
        return this.stemPlugin.getPluginLogger();
    }

    protected void fireEvent(StemEvent event) {
        this.stemPlugin.getStemApi().events().fire(event);
    }

    /**
     * Publishes a command to the device.
     *
     * @return true if the message was handed to the MQTT client
     */
    protected boolean publish(String topic, String payload) {
        boolean published = this.stemPlugin.getStemApi().mqtt().publish(topic, payload.getBytes(StandardCharsets.UTF_8), COMMAND_QOS, false);
        if (!published) {
            this.getLogger().warning("Unable to publish to " + topic + " for device " + this.getConfigName());
        }
        return published;
    }

    /**
     * Repeats {@code request} once per second until the device has data or 30 attempts are done.
     */
    protected void requestUntilData(Runnable request, String categoryName) {
        int counter = 0;
        while (this.running && !this.hasData() && counter <= 30) {
            request.run();
            this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + categoryName + ", " + this.getDeviceTechnology().name() + ")");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            counter++;
        }
        if (counter > 30) {
            this.getLogger().warning("Seems device " + this.getDeviceHardAddress() + " is disconnected!");
        }
    }

    /**
     * @return true if data was received after {@code request} minus {@code tolerance}. False if one of the dates is unknown.
     */
    protected static boolean receivedSince(Date lastData, Date request, Duration tolerance) {
        if (lastData == null || request == null) {
            return false;
        }
        return request.toInstant().minus(tolerance).toEpochMilli() <= lastData.getTime();
    }

    protected abstract void request_initial_status();

    private void messageArrived(String topic, byte[] payload) {
        if (!this.running) {
            return;
        }
        try {
            this.mqttMessageEvent(topic, new String(payload, StandardCharsets.UTF_8));
        } catch (Exception e) {
            this.getLogger().error("Invalid MQTT data for device " + this.getConfigName() + " on topic " + topic, e);
        }
    }

    public abstract void mqttMessageEvent(String topic, String payload);

    public abstract void requestHealthCheck();

    public abstract boolean healthCheckStatus();

    public abstract boolean hasData();

    public abstract JSONObject getJSONData();

    public abstract JSONObject setJSONData(JSONObject jsonInput);

    protected static JSONObject notSupported() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }
}
