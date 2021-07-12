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

public class TasmotaUpdateEvent implements StemEvent {
    private final TasmotaMQTTDevice tasmotaMQTTDevice;
    private final boolean newStatus;

    public TasmotaUpdateEvent(final TasmotaMQTTDevice tasmotaMQTTDevice, final boolean newStatus) {
        this.tasmotaMQTTDevice = tasmotaMQTTDevice;
        this.newStatus = newStatus;
    }

    public TasmotaMQTTDevice getTasmotaMQTTDevice() {
        return tasmotaMQTTDevice;
    }

    public boolean getNewStatus() {
        return newStatus;
    }
}
