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

import de.linzn.homeDevices.DeviceCategory;
import de.stem.stemSystem.modules.eventModule.StemEvent;

public class AutoModeRestApiChangeRequestEvent implements StemEvent {

    private final DeviceCategory deviceCategory;
    private final boolean oldValue;
    private final boolean newValue;
    private boolean isCanceled;

    public AutoModeRestApiChangeRequestEvent(DeviceCategory deviceCategory, boolean oldValue, boolean newValue) {
        this.deviceCategory = deviceCategory;
        this.newValue = newValue;
        this.oldValue = oldValue;
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

    public DeviceCategory getDeviceCategory() {
        return deviceCategory;
    }

    public boolean isOldValue() {
        return oldValue;
    }

    public boolean isNewValue() {
        return newValue;
    }
}
