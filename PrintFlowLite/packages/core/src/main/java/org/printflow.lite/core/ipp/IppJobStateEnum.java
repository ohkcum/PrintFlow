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
 * Enumeration of IPP job states.
 *
 * @author Rijk Ravestein
 *
 */
public enum IppJobStateEnum {

    /** Out-of-band 'unknown'. */
    IPP_JOB_UNKNOWN(0x00, "UNKNOWN"),

    /** Job is waiting to be printed. */
    IPP_JOB_PENDING(0x03, "PENDING"),

    /** Job is held for printing. */
    IPP_JOB_HELD(0x04, "HELD"),

    /** Job is currently printing. */
    IPP_JOB_PROCESSING(0x05, "PROCESSING"),

    /** Job has been stopped. */
    IPP_JOB_STOPPED(0x06, "STOPPED"),

    /** Job has been canceled. */
    IPP_JOB_CANCELED(0x07, "CANCELED"),

    /** Job has aborted due to error. */
    IPP_JOB_ABORTED(0x08, "ABORTED"),

    /** Job has completed successfully. */
    IPP_JOB_COMPLETED(0x09, "COMPLETED");

    /** */
    private final int jobState;

    /**
     * Text to be used in user interface.
     */
    private final String logText;

    /** */
    private static final Map<Integer, IppJobStateEnum> ENUM_MAP;

    /* */
    static {
        final Map<Integer, IppJobStateEnum> map = new HashMap<>();
        for (IppJobStateEnum value : values()) {
            map.put(value.asInteger(), value);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Creates an enum value from an integer.
     *
     * @param value
     *            The integer.
     * @param text
     *            Text to be used in user interface.
     */
    IppJobStateEnum(final int value, final String text) {
        this.jobState = value;
        this.logText = text;
    }

    /**
     * Gets the int representing this enum value.
     *
     * @return The int value.
     */
    public int asInt() {
        return this.jobState;
    }

    /**
     * Gets the {@link Integer} representing this enum value.
     *
     * @return The Integer value.
     */
    public Integer asInteger() {
        return Integer.valueOf(this.asInt());
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
     * Checks if status means a job is present on CUPS queue.
     *
     * @return {@code true} when state is {@link #IPP_JOB_PENDING},
     *         {@link #IPP_JOB_HELD}, {@link #IPP_JOB_PROCESSING} or
     *         {@link #IPP_JOB_STOPPED}.
     */
    public boolean isPresentOnQueue() {
        return this.jobState != IPP_JOB_UNKNOWN.jobState
                && this.jobState < getFirstAbsentOnQueueOrdinal().asInt();
    }

    /**
     * Checks if status means a CUPS job reached an end state.
     *
     * @return {@code true} when state is {@link #IPP_JOB_CANCELED},
     *         {@link #IPP_JOB_ABORTED} or {@link #IPP_JOB_COMPLETED}.
     */
    public boolean isEndState() {
        return this.jobState >= getFirstAbsentOnQueueOrdinal().asInt();
    }

    /**
     * Checks if status means a job is finished and left the CUPS queue.
     *
     * @return {@code true} when state is COMPLETED, CANCELED, ABORTED or
     *         UNKNOWN.
     */
    public boolean isFinished() {
        return !this.isPresentOnQueue();
    }

    /**
     * Checks if status means a job has failed.
     *
     * @return {@code true} when state is {@link #IPP_JOB_CANCELED} or
     *         {@link #IPP_JOB_ABORTED}.
     */
    public boolean isFailure() {
        return this.jobState == IPP_JOB_CANCELED.jobState
                || this.jobState == IPP_JOB_ABORTED.jobState;
    }

    /**
     *
     * @return The {@link IppJobStateEnum} that is the first ordinal indicating
     *         a status that job is end-of-state.
     */
    public static IppJobStateEnum getFirstAbsentOnQueueOrdinal() {
        return IppJobStateEnum.IPP_JOB_CANCELED;
    }

    /**
     *
     * @param value
     *            The CUPS job state.
     * @return The enum value.
     * @throws IllegalStateException
     *             If job state is unidentified.
     */
    public static IppJobStateEnum asEnum(final Integer value) {

        final IppJobStateEnum evt = ENUM_MAP.get(value);
        if (evt == null) {
            throw new IllegalStateException(String
                    .format("Unknown CUPS job state [%d]", value.intValue()));
        }
        return evt;
    }

}
