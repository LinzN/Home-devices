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

package de.linzn.homeDevices.events;

import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.stem.stemSystem.modules.eventModule.CancelableEvent;

import java.time.LocalTime;
import java.util.Date;

public class AutoSwitchOffTimerEvent extends CancelableEvent {

    private final TasmotaMQTTDevice tasmotaMQTTDevice;
    private final LocalTime startTime;
    private final LocalTime stopTime;
    private final Date lastSwitch;

    public AutoSwitchOffTimerEvent(TasmotaMQTTDevice tasmotaMQTTDevice, LocalTime startTime, LocalTime stopTime, Date lastSwitch) {
        this.tasmotaMQTTDevice = tasmotaMQTTDevice;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.lastSwitch = lastSwitch;
    }

    public TasmotaMQTTDevice getTasmotaMQTTDevice() {
        return tasmotaMQTTDevice;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getStopTime() {
        return stopTime;
    }

    public Date getLastSwitch() {
        return lastSwitch;
    }
}
