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

import de.linzn.homeDevices.AutoStartStopTimer;
import de.linzn.homeDevices.devices.switches.SwitchableMQTTDevice;
import de.stem.stemSystem.modules.eventModule.CancelableEvent;


public class AutoStartStopTimerEvent extends CancelableEvent {

    private final SwitchableMQTTDevice switchableMQTTDevice;
    private final AutoStartStopTimer.SwitchTimer timer;

    public AutoStartStopTimerEvent(SwitchableMQTTDevice switchableMQTTDevice, AutoStartStopTimer.SwitchTimer timer) {
        this.switchableMQTTDevice = switchableMQTTDevice;
        this.timer = timer;
    }

    public AutoStartStopTimer.SwitchTimer getTimer() {
        return timer;
    }

    public SwitchableMQTTDevice getSwitchableMQTTDevice() {
        return switchableMQTTDevice;
    }
}