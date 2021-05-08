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

package de.linzn.homeDevices;

import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.stem.stemSystem.STEMSystemApp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class AutoStartStopTimer implements Runnable {

    private final TasmotaMQTTDevice tasmotaMQTTDevice;
    private final LinkedList<SwitchTimer> timerList;
    private final long offSet = 200;
    private boolean isEnabled;


    public AutoStartStopTimer(TasmotaMQTTDevice tasmotaMQTTDevice) {
        this.tasmotaMQTTDevice = tasmotaMQTTDevice;
        this.timerList = new LinkedList<>();
        this.loadTimer();
        this.resortTimerForCurrentTime();
    }


    private void loadTimer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

        if (HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().contains("tasmota." + this.tasmotaMQTTDevice.getConfigName() + ".autoStartStopTimer")) {
            isEnabled = true;
            Map<String, Map> objectMap = (LinkedHashMap<String, Map>) HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().get("tasmota." + this.tasmotaMQTTDevice.getConfigName() + ".autoStartStopTimer");

            for (String key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("tasmota." + this.tasmotaMQTTDevice.getConfigName() + ".autoStartStopTimer." + key + ".time"), dateTimeFormatter);
                boolean value = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getBoolean("tasmota." + this.tasmotaMQTTDevice.getConfigName() + ".autoStartStopTimer." + key + ".value");
                STEMSystemApp.LOGGER.CONFIG("Add timer for hardId: " + this.tasmotaMQTTDevice.deviceHardAddress + " configName: " + this.tasmotaMQTTDevice.configName + " time: " + localTime.toString() + " value: " + value);
                this.timerList.add(new SwitchTimer(localTime, value));
            }

        } else {
            isEnabled = false;
        }
    }

    private void resortTimerForCurrentTime() {
        if (isEnabled) {
            int moveCounter = 0;
            for (int i = 0; i < this.timerList.size(); i++) {
                SwitchTimer first = this.timerList.getFirst();
                LocalTime offSetTime = first.localTime.plus(offSet, ChronoUnit.MILLIS);
                if (offSetTime.isBefore(LocalTime.now())) {
                    first = this.timerList.removeFirst();
                    this.timerList.addLast(first);
                    STEMSystemApp.LOGGER.CONFIG("Add timer to tail in timer list:  " + first.localTime.toString() + " value: " + first.status);
                    moveCounter++;
                    continue;
                }
                break;
            }
            STEMSystemApp.LOGGER.CONFIG("Resort timer list done. Remaining timers for today: " + (this.timerList.size() - moveCounter));
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (isEnabled && this.tasmotaMQTTDevice.deviceStatus != null) {
                    SwitchTimer first = this.timerList.getFirst();
                    LocalTime offSetTime = first.localTime.plus(offSet, ChronoUnit.MILLIS);
                    if (first.localTime.isBefore(LocalTime.now()) && offSetTime.isAfter(LocalTime.now())) {
                        first = this.timerList.removeFirst();
                        /* switch device */
                        STEMSystemApp.LOGGER.INFO("Timer switch hardId: " + this.tasmotaMQTTDevice.deviceHardAddress + " configName: " + this.tasmotaMQTTDevice.configName + " status: " + first.status);
                        this.tasmotaMQTTDevice.switchDevice(first.status);
                        /* switch device */
                        this.timerList.addLast(first);
                    }
                }
                Thread.sleep(50);
            } catch (Exception e) {
                STEMSystemApp.LOGGER.ERROR(e);
            }
        }
    }

    private static class SwitchTimer {

        public LocalTime localTime;
        public boolean status;

        public SwitchTimer(LocalTime localTime, boolean status) {
            this.localTime = localTime;
            this.status = status;
        }
    }
}
