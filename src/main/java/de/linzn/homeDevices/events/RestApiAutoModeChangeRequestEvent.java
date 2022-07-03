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

import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.stem.stemSystem.modules.eventModule.CancelableEvent;

public class RestApiAutoModeChangeRequestEvent extends CancelableEvent {

    private final SwitchCategory switchCategory;
    private final boolean oldValue;
    private final boolean newValue;

    public RestApiAutoModeChangeRequestEvent(SwitchCategory switchCategory, boolean oldValue, boolean newValue) {
        this.switchCategory = switchCategory;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public SwitchCategory getDeviceCategory() {
        return switchCategory;
    }

    public boolean isOldValue() {
        return oldValue;
    }

    public boolean isNewValue() {
        return newValue;
    }
}
