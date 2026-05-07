/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2011-2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.server;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.printflow.lite.core.ILoggerTSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Logger} that writes security messages in TSV format.
 *
 * @author Rijk Ravestein
 *
 */
public final class SecurityLogger implements ILoggerTSV {

    /**
     * Message to append columns on.
     */
    static class Message {

        /** */
        private final StringBuilder msg = new StringBuilder();

        public Message append(final String s) {
            this.msg.append(s).append("\t");
            return this;
        }

        public Message append(final long s) {
            return this.append(String.valueOf(s));
        }

        public int length() {
            return this.msg.length();
        }

        @Override
        public String toString() {
            return this.msg.toString();
        }
    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SecurityLogger.class);

    /** */
    private static final String DATEFORMAT_PATTERN =
            "yyyy-MM-dd'\t'HH:mm:ss.SSS";

    /** Utility class. */
    private SecurityLogger() {
    }

    /**
     * Logs a message, if logging {@link #isEnabled()}.
     *
     * @param now
     *            current time
     * @param msg
     *            message
     */
    public static void log(final Date now, final Message msg) {

        if (LOGGER.isWarnEnabled()) {

            final SimpleDateFormat dateFormat =
                    new SimpleDateFormat(DATEFORMAT_PATTERN);

            LOGGER.warn(String.format("%s\t%s\t%s", dateFormat.format(now), msg,
                    Thread.currentThread().getName()));
        }
    }

    /**
     * @return {@code true} if logger is enabled.
     */
    public static boolean isEnabled() {
        return LOGGER.isWarnEnabled();
    }

}
