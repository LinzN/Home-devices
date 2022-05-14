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

import de.linzn.homeDevices.devices.switches.SwitchableMQTTDevice;
import de.stem.stemSystem.modules.eventModule.CancelableEvent;

import java.time.LocalTime;
import java.util.Date;

public class AutoSwitchOffTimerEvent extends CancelableEvent {

    private final SwitchableMQTTDevice switchableMQTTDevice;
    private final LocalTime startTime;
    private final LocalTime stopTime;
    private final Date lastSwitch;

    public AutoSwitchOffTimerEvent(SwitchableMQTTDevice switchableMQTTDevice, LocalTime startTime, LocalTime stopTime, Date lastSwitch) {
        this.switchableMQTTDevice = switchableMQTTDevice;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.lastSwitch = lastSwitch;
    }

    public SwitchableMQTTDevice getSwitchableMQTTDevice() {
        return switchableMQTTDevice;
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
