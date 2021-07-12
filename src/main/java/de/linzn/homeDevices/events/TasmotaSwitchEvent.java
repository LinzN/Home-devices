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
import de.stem.stemSystem.modules.eventModule.StemEvent;

public class TasmotaSwitchEvent implements StemEvent {
    private final TasmotaMQTTDevice tasmotaMQTTDevice;
    private boolean isCanceled;

    public TasmotaSwitchEvent(final TasmotaMQTTDevice tasmotaMQTTDevice) {
        this.tasmotaMQTTDevice = tasmotaMQTTDevice;
        this.isCanceled = false;
    }

    public TasmotaMQTTDevice getTasmotaMQTTDevice() {
        return tasmotaMQTTDevice;
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
}
