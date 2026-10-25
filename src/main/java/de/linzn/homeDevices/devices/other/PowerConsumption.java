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
import de.linzn.homeDevices.events.records.PowerConsumptionUpdateDataEvent;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class PowerConsumption extends MqttDevice {

    /* current power in W and today's energy in kWh, as reported by tasmota */
    private volatile int power = 0;
    private volatile double today = 0;
    private volatile Date lastCollection;
    private volatile Date healthSwitchDateRequest;

    public PowerConsumption(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    protected void request_initial_status() {
        this.getLogger().debug("Initial request for device " + this.getDeviceHardAddress() + " (" + MqttDeviceCategory.POWERCONSUMPTION.name() + ") is not supported!");
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        JSONObject energy = jsonPayload.optJSONObject("ENERGY");
        if (energy == null) {
            return;
        }
        this.power = energy.getInt("Power");
        this.today = energy.getDouble("Today");
        this.lastCollection = new Date();
        Date date = this.lastCollection;
        String time = jsonPayload.optString("Time", null);
        if (time != null && time.length() >= 10) {
            try {
                /* tasmota sends ISO local date time, only the date is relevant */
                date = Date.from(LocalDate.parse(time.substring(0, 10)).atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                this.getLogger().warning("Invalid time " + time + " from power consumption device " + this.getConfigName());
            }
        }
        this.fireEvent(new PowerConsumptionUpdateDataEvent(this, date));
    }

    public int getPower() {
        return this.power;
    }

    public double getToday() {
        return this.today;
    }

    @Override
    public void requestHealthCheck() {
        this.healthSwitchDateRequest = new Date();
    }

    @Override
    public boolean healthCheckStatus() {
        return receivedSince(this.lastCollection, this.healthSwitchDateRequest, Duration.ofMinutes(5));
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
        return notSupported();
    }
}
