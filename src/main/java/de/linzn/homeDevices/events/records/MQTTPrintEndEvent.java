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


import de.linzn.homeDevices.devices.other.AnkermakePrinter;
import de.linzn.stem.modules.eventModule.StemEvent;

public record MQTTPrintEndEvent(AnkermakePrinter ankermakePrinter, int eventValue, String printName) implements StemEvent {

}
