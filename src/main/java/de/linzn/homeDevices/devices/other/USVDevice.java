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

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.openJL.converter.TimeAdapter;
import de.linzn.openJL.math.FloatingPoint;
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

public class USVDevice extends MqttDevice {

    private static final int MAX_CAPACITY = 24000;
    private static final int CHARGE_TICK = 5;
    private static final int DISCHARGE_TICK = 100;
    public Date lastData;
    private AtomicBoolean isACMode;
    private Date healthSwitchDateRequest;
    private InformationBlock informationBlock;
    /* only calculated based on how long ac is on */
    private long batteryCapacityTicks = 0;

    public USVDevice(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "stat/" + deviceProfile.getDeviceHardAddress() + "/status");
    }

    @Override
    protected void request_initial_status() {
        STEMApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.USV.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        boolean acMode = jsonPayload.getBoolean("isACMode");
        STEMApp.LOGGER.DEBUG("USV DATA: [acMode:" + acMode + "]");
        this.lastData = new Date();
        this.isACMode = new AtomicBoolean(acMode);

        if (!this.isACMode.get()) {
            if (informationBlock == null) {
                informationBlock = new InformationBlock("USV", "USV is running in battery mode!", HomeDevicesPlugin.homeDevicesPlugin, "USV is running in battery mode!");
                informationBlock.setIcon("USV");
                informationBlock.setExpireTime(-1L);
                informationBlock.addIntent(InformationIntent.NOTIFY_USER);
                informationBlock.addIntent(InformationIntent.SHOW_DISPLAY);
                STEMApp.getInstance().getInformationModule().queueInformationBlock(informationBlock);
            } else {
                informationBlock.setDescription("USV is running in battery mode!");
                informationBlock.setExpireTime(-1L);
            }
            if ((this.batteryCapacityTicks - DISCHARGE_TICK) >= 0) {
                this.batteryCapacityTicks = this.batteryCapacityTicks - DISCHARGE_TICK;
            } else {
                this.batteryCapacityTicks = 0;
            }
        } else {
            if (informationBlock != null) {
                informationBlock.setDescription("USV was running in battery mode!");
                Instant expireDate = TimeAdapter.getTimeInstant().plus(2, ChronoUnit.HOURS);
                informationBlock.setExpireTime(expireDate);
                this.informationBlock = null;
            }
            if ((this.batteryCapacityTicks + CHARGE_TICK) <= MAX_CAPACITY) {
                this.batteryCapacityTicks = this.batteryCapacityTicks + CHARGE_TICK;
            }
        }
    }

    private float calculateCapacity() {
        return FloatingPoint.round((100F / MAX_CAPACITY) * this.batteryCapacityTicks, 1);
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(30, ChronoUnit.SECONDS).toEpochMilli() <= this.lastData.getTime();
    }

    @Override
    public boolean hasData() {
        return this.isACMode != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("acMode", this.isACMode.get());
        jsonObject.put("capacity", this.calculateCapacity());
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

}
