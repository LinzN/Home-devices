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

package de.linzn.homeDevices.events.cancelable;

import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.stem.modules.eventModule.CancelableEvent;

import java.time.LocalTime;
import java.util.Date;

public class AutoSwitchOffTimerEvent extends CancelableEvent {

    private final MqttSwitch mqttSwitch;
    private final LocalTime startTime;
    private final LocalTime stopTime;
    private final Date lastSwitch;

    public AutoSwitchOffTimerEvent(MqttSwitch mqttSwitch, LocalTime startTime, LocalTime stopTime, Date lastSwitch) {
        this.mqttSwitch = mqttSwitch;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.lastSwitch = lastSwitch;
    }

    public MqttSwitch getSwitchableMQTTDevice() {
        return mqttSwitch;
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
