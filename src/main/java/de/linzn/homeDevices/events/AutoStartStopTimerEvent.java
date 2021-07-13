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
import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.stem.stemSystem.modules.eventModule.StemEvent;

public class AutoStartStopTimerEvent implements StemEvent {

    private final TasmotaMQTTDevice tasmotaMQTTDevice;
    private final AutoStartStopTimer.SwitchTimer timer;
    private boolean isCanceled;

    public AutoStartStopTimerEvent(TasmotaMQTTDevice tasmotaMQTTDevice, AutoStartStopTimer.SwitchTimer timer) {
        this.tasmotaMQTTDevice = tasmotaMQTTDevice;
        this.timer = timer;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public void cancel() {
        this.isCanceled = true;
    }

    public AutoStartStopTimer.SwitchTimer getTimer() {
        return timer;
    }

    public TasmotaMQTTDevice getTasmotaMQTTDevice() {
        return tasmotaMQTTDevice;
    }
}
