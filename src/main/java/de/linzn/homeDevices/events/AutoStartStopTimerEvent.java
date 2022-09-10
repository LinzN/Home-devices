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

import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.openJL.pairs.Pair;
import de.stem.stemSystem.modules.eventModule.CancelableEvent;

import java.time.LocalTime;


public class AutoStartStopTimerEvent extends CancelableEvent {

    private final MqttSwitch mqttSwitch;
    private final Pair<LocalTime, Boolean> timer;

    public AutoStartStopTimerEvent(MqttSwitch mqttSwitch, Pair<LocalTime, Boolean> timer) {
        this.mqttSwitch = mqttSwitch;
        this.timer = timer;
    }

    public Pair<LocalTime, Boolean> getTimer() {
        return timer;
    }

    public MqttSwitch getSwitchableMQTTDevice() {
        return mqttSwitch;
    }
}
