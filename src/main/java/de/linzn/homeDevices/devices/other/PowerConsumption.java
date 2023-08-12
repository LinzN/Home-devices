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

import com.google.common.util.concurrent.AtomicDouble;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class PowerConsumption extends MqttDevice {

    public AtomicInteger power = new AtomicInteger(0);
    public AtomicDouble today = new AtomicDouble(0);
    private Date lastCollection;
    private Date healthSwitchDateRequest;

    public PowerConsumption(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    protected void request_initial_status() {
        STEMSystemApp.LOGGER.INFO("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.DOORRING.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(MqttMessage mqttMessage) {
        this.lastCollection = new Date();
        String payload = new String(mqttMessage.getPayload());
        JSONObject jsonPayload = new JSONObject(payload);
        JSONObject energy = jsonPayload.getJSONObject("ENERGY");
        this.power = new AtomicInteger(energy.getInt("Power"));
        this.today = new AtomicDouble(energy.getDouble("Today"));
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return this.healthSwitchDateRequest.toInstant().minus(5, ChronoUnit.MINUTES).toEpochMilli() <= this.lastCollection.getTime();
    }

    @Override
    public boolean hasData() {
        return this.lastCollection != null;
    }

    @Override
    public JSONObject getJSONData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "ok");
        jsonObject.put("power", this.power);
        jsonObject.put("today", this.today);
        return jsonObject;
    }

    @Override
    public JSONObject setJSONData(JSONObject jsonInput) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "Not supported");
        return jsonObject;
    }

}
