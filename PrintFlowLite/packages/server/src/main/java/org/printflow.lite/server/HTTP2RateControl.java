/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2011-2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2011-2026 Datraverse B.V. <info@datraverse.com>
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

import java.time.Duration;

import org.eclipse.jetty.http2.parser.RateControl;
import org.eclipse.jetty.http2.parser.WindowRateControl;
import org.eclipse.jetty.io.EndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension of {@link WindowRateControl} for overriding
 * {@link WindowRateControl#onEvent(Object)} to monitor events that exceed the
 * rate limit.
 *
 * @author Rijk Ravestein
 *
 */
public final class HTTP2RateControl extends WindowRateControl {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HTTP2RateControl.class);

    /**
     * @param maxEvents
     * @param window
     */
    public HTTP2RateControl(final int maxEvents, final Duration window) {
        super(maxEvents, window);
    }

    @Override
    public boolean onEvent(final Object event) {

        final boolean rateWithinLimits = super.onEvent(event);

        if (!rateWithinLimits) {

            if (event instanceof org.eclipse.jetty.http2.frames.Frame) {

                HTTP2RateControlMonitor.instance().onRateBeyondLimit(
                        ((org.eclipse.jetty.http2.frames.Frame) event)
                                .getType());
            } else {
                LOGGER.warn("Unkown out-of-limits event: " + event.toString());
            }
        }
        return rateWithinLimits;
    }

    /**
     * {@link RateControl.Factory} that creates an {@link HTTP2RateControl}
     * instance.
     */
    public static final class Factory implements RateControl.Factory {

        /** */
        private final int maxEvents;

        /**
         * @param http2Config
         */
        public Factory(final HTTP2Configuration http2Config) {
            this.maxEvents = http2Config.getMaxRequestsPerSec();
        }

        @Override
        public RateControl newRateControl(final EndPoint endPoint) {
            return new HTTP2RateControl(this.maxEvents, Duration.ofSeconds(1));
        }
    }

}
