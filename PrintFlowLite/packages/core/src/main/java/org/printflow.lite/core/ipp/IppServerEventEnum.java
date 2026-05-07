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
 * Enumeration of IPP server events.
 *
 * @author Rijk Ravestein
 *
 */
public enum IppServerEventEnum {

    /** */
    AUDIT("server-audit", "AUDIT"),
    /** */
    RESTARTED("server-restarted", "RESTARTED"),
    /** */
    STARTED("server-started", "STARTED"),
    /** */
    STOPPED("server-stopped", "STOPPED");

    /** */
    private final String event;

    /** */
    private static final Map<String, IppServerEventEnum> ENUM_MAP;

    /* */
    static {
        final Map<String, IppServerEventEnum> map = new HashMap<>();
        for (IppServerEventEnum value : values()) {
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
     * @param evnt
     *            IPP/CUPS event.
     * @param text
     *            Text to be used in user interface.
     */
    IppServerEventEnum(final String evnt, final String text) {
        this.event = evnt;
        this.logText = text;
    }

    /**
     * Gets the event representing this enum value.
     *
     * @return The event value.
     */
    public String asEvent() {
        return this.event;
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
    public static IppServerEventEnum asEnum(final String value) {
        final IppServerEventEnum enumVal = ENUM_MAP.get(value);
        if (enumVal == null) {
            throw new IllegalStateException(
                    String.format("Unknown CUPS server event [%s]", value));
        }
        return enumVal;
    }

}
