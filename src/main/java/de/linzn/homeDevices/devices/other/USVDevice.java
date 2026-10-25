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

package de.linzn.homeDevices.devices.other;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.openJL.math.FloatingPoint;
import de.linzn.stem.modules.informationModule.InformationBlock;
import de.linzn.stem.modules.informationModule.InformationIntent;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class USVDevice extends MqttDevice {

    private static final int MAX_CAPACITY = 24000;
    private static final int CHARGE_TICK = 5;
    private static final int DISCHARGE_TICK = 100;

    public volatile Date lastData;
    private volatile Boolean isACMode;
    private volatile Date healthSwitchDateRequest;
    /* guarded by this */
    private InformationBlock informationBlock;
    /* only calculated based on how long ac is on, guarded by this */
    private long batteryCapacityTicks = 0;

    public USVDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/status");
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.USV.name() + ") is not supported!");
    }

    @Override
    public synchronized void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        boolean acMode = jsonPayload.getBoolean("isACMode");
        this.getLogger().debug("USV DATA: [acMode:" + acMode + "]");
        this.lastData = new Date();
        this.isACMode = acMode;
        if (!acMode) {
            if (informationBlock == null) {
                informationBlock = new InformationBlock("USV", "USV is running in battery mode!", this.getHomeDevicesPlugin(), "USV is running in battery mode!");
                informationBlock.setIcon("USV");
                informationBlock.setExpireTime(-1L);
                informationBlock.addIntent(InformationIntent.NOTIFY_USER);
                informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
                this.getHomeDevicesPlugin().getStemApi().information().queue(informationBlock);
            } else {
                informationBlock.setDescription("USV is running in battery mode!");
                informationBlock.setExpireTime(-1L);
            }
            this.batteryCapacityTicks = Math.max(0, this.batteryCapacityTicks - DISCHARGE_TICK);
        } else {
            if (informationBlock != null) {
                informationBlock.setDescription("USV was running in battery mode!");
                informationBlock.setExpireTime(TimeAdapter.getTimeInstant().plus(2, ChronoUnit.HOURS));
                this.informationBlock = null;
            }
            this.batteryCapacityTicks = Math.min(MAX_CAPACITY, this.batteryCapacityTicks + CHARGE_TICK);
        }
    }

    private synchronized float calculateCapacity() {
        return FloatingPoint.round((100F / MAX_CAPACITY) * this.batteryCapacityTicks, 1);
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastData, this.healthSwitchDateRequest, Duration.ofSeconds(30));
    }

    @Override
    public boolean hasData() {
        return this.isACMode != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        Boolean acMode = this.isACMode;
        jsonObject.put("acMode", acMode != null ? acMode : "error");
        jsonObject.put("capacity", this.calculateCapacity());
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        return notSupported();
    }
}
