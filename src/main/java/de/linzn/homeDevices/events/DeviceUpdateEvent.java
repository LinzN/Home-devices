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
import de.stem.stemSystem.modules.eventModule.StemEvent;

public class DeviceUpdateEvent implements StemEvent {
    private final SwitchableMQTTDevice switchableMQTTDevice;
    private final boolean newStatus;

    public DeviceUpdateEvent(final SwitchableMQTTDevice switchableMQTTDevice, final boolean newStatus) {
        this.switchableMQTTDevice = switchableMQTTDevice;
        this.newStatus = newStatus;
    }

    public SwitchableMQTTDevice getSwitchableMQTTDevice() {
        return switchableMQTTDevice;
    }

    public boolean getNewStatus() {
        return newStatus;
    }
}
