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

package de.linzn.homeDevices.events.records;


import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.stem.stemSystem.modules.eventModule.StemEvent;

import java.util.Date;

public record EnvironmentSensorUpdateDataEvent(EnvironmentSensor environmentSensor,
                                               Date timestamp) implements StemEvent {
}
