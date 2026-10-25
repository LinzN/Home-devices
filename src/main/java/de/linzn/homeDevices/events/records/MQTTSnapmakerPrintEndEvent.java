/*
 * Copyright (c) 2026 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */

package de.linzn.homeDevices.events.records;

import de.linzn.homeDevices.devices.other.SnapmakerPrinter;
import de.linzn.stem.modules.eventModule.StemEvent;

/**
 * Fired when a job on a Snapmaker printer ends.
 *
 * @param snapmakerPrinter the printer
 * @param eventType        {@code print_finished}, {@code print_failed} or {@code print_cancelled}
 * @param printName        G-code file name
 * @param printDuration    seconds actually printed
 */
public record MQTTSnapmakerPrintEndEvent(SnapmakerPrinter snapmakerPrinter, String eventType, String printName,
                                         long printDuration) implements StemEvent {

    public boolean isSuccess() {
        return "print_finished".equals(eventType);
    }
}
