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

public class ToggleDeviceDeviceEvent extends SwitchDeviceEvent {

    public ToggleDeviceDeviceEvent(final MqttSwitch mqttSwitch) {
        super(mqttSwitch);
    }

}
