/*
 * Copyright (C) 2021. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.homeDevices.devices.other.nanoCLR;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.api.scheduler.ScheduledTask;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class KeyBoardDevice extends MqttDevice implements Runnable {

    public volatile Date lastData;
    /* guarded by this */
    public Date lastModeSwitch;
    private volatile Date healthSwitchDateRequest;
    private volatile int mode = -1;
    private boolean hasPendingStandbyRequest;
    private ScheduledTask standbyTask;

    public KeyBoardDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/nanoCLR/" + deviceProfile.getDeviceHardAddress() + "/data");
        this.hasPendingStandbyRequest = false;
    }

    @Override
    public synchronized void start() {
        super.start();
        if (this.standbyTask == null) {
            this.standbyTask = this.getHomeDevicesPlugin().getStemApi().scheduler().runRepeating(this, 30, 3, TimeUnit.SECONDS);
        }
    }

    @Override
    public synchronized void stop() {
        if (this.standbyTask != null) {
            this.standbyTask.cancel();
            this.standbyTask = null;
        }
        super.stop();
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.NANOCLR_KEYBOARD.name() + ") is not supported!");
        this.sendMqttModeUpdate("startUp");
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        this.lastData = new Date();
        JSONObject jsonPayload = new JSONObject(payload);
        if (!jsonPayload.has("value")) {
            return;
        }
        int value = jsonPayload.getBoolean("value") ? 1 : 0;
        boolean loggedIn;
        synchronized (this) {
            if (this.mode == value) {
                return;
            }
            this.lastModeSwitch = new Date();
            this.mode = value;
            loggedIn = this.mode != 0;
            this.hasPendingStandbyRequest = !loggedIn;
        }
        if (!loggedIn) {
            this.sendMqttModeUpdate("penningLogout");
        } else {
            /* outside the lock: a profile change touches every device */
            HomeDevicesPlugin.homeDevicesPlugin.getProfileController().requestProfileChange(SmartHomeProfile.DEFAULT);
            this.sendMqttModeUpdate("loggedIn");
        }
    }

    private void sendMqttModeUpdate(String modeName) {
        JSONObject data = new JSONObject();
        data.put("command", "led");
        data.put("mode", modeName);
        this.publish("cmnd/nanoCLR/" + getDeviceHardAddress() + "/data", data.toString());
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastData, this.healthSwitchDateRequest, Duration.ofMinutes(1));
    }

    @Override
    public boolean hasData() {
        return this.mode != -1;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mode", this.mode);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        return notSupported();
    }

    @Override
    public void run() {
        synchronized (this) {
            if (!this.hasPendingStandbyRequest || this.lastModeSwitch == null) {
                return;
            }
            if (this.lastModeSwitch.toInstant().plus(5, ChronoUnit.MINUTES).toEpochMilli() > System.currentTimeMillis()) {
                return;
            }
            this.hasPendingStandbyRequest = false;
        }
        HomeDevicesPlugin.homeDevicesPlugin.getProfileController().requestProfileChange(SmartHomeProfile.STANDBY);
        sendMqttModeUpdate("loggedOut");
    }
}
