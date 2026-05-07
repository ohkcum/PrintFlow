/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.core.ipp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.printflow.lite.core.util.LocaleHelper;

/**
 * Enumeration of IPP printer events.
 *
 * @author Rijk Ravestein
 *
 */
public enum IppPrinterEventEnum {

    /** */
    IPP_CONFIG_CHANGED("printer-config-changed", "CONFIG CHANGED"),
    /** */
    IPP_FINISHINGS_CHANGED("printer-finishings-changed", "FINISHINGS CHANGED"),
    /** */
    IPP_MEDIA_CHANGED("printer-media-changed", "MEDIA CHANGED"),
    /** */
    IPP_QUEUE_ORDER_CHANGED("printer-queue-order-changed",
            "QUEUE ORDER CHANGED"),
    /** */
    IPP_RESTARTED("printer-restarted", "RESTARTED"),
    /** */
    IPP_SHUTDOWN("printer-shutdown", "SHUTDOWN"),
    /** */
    IPP_STATE_CHANGED("printer-state-changed", "STATE CHANGED"),
    /** */
    IPP_STOPPED("printer-stopped", "STOPPED"),

    /** CUPS event. */
    CUPS_ADDED("printer-added", "ADDED"),
    /** CUPS event. */
    CUPS_DELETED("printer-deleted", "DELETED"),
    /** CUPS event. */
    CUPS_MODIFIED("printer-modified", "MODIFIED");

    /** */
    private final String printerEvent;

    /** */
    private static final Map<String, IppPrinterEventEnum> ENUM_MAP;

    /* */
    static {
        final Map<String, IppPrinterEventEnum> map = new HashMap<>();
        for (IppPrinterEventEnum value : values()) {
            map.put(value.asEvent(), value);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Text to be used in user interface.
     */
    private final String logText;

    /**
     * Creates an enum value from an integer.
     *
     * @param event
     *            IPP/CUPS event.
     * @param text
     *            Text to be used in user interface.
     */
    IppPrinterEventEnum(final String event, final String text) {
        this.printerEvent = event;
        this.logText = text;
    }

    /**
     * Gets the event representing this enum value.
     *
     * @return The event value.
     */
    public String asEvent() {
        return this.printerEvent;
    }

    /**
     *
     * @return Text string to be used for logging.
     */
    public String asLogText() {
        return this.logText;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * @param value
     *            The printer event.
     * @return The enum value.
     * @throws IllegalStateException
     *             If printer event is unknown.
     */
    public static IppPrinterEventEnum asEnum(final String value) {
        final IppPrinterEventEnum enumVal = ENUM_MAP.get(value);
        if (enumVal == null) {
            throw new IllegalStateException(
                    String.format("Unknown CUPS printer event [%s]", value));
        }
        return enumVal;
    }

}
