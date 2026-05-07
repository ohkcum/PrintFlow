/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.services;

import org.printflow.lite.core.services.helpers.IRateLimiterListener;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface RateLimiterService extends StatefulService {

    enum LimitEnum {
        /**
         * Web App Login.
         */
        USER_AUTH_FAILURE_BY_IP,
        /**
         * Web App User Registration.
         */
        USER_REG_BY_IP,
        /**
         * Application Programming Interface.
         */
        API_FAILURE_BY_IP,
        /**
         * Print-in.
         */
        PRINT_IN_FAILURE_BY_ADDR;
    }

    /** */
    interface IEvent {
        /**
         * @return Event ID.
         */
        String id();

        /**
         * @return Subject description.
         */
        String subject();
    }

    /** */
    final class IPEvent implements IEvent {
        /** */
        private final String ipAddr;
        /** */
        private final String subject;

        /**
         * @param addr
         *            IP address as Event ID.
         * @param subj
         *            Subject description.
         */
        public IPEvent(final String addr, final String subj) {
            this.ipAddr = addr;
            this.subject = subj;
        }

        @Override
        public String id() {
            return this.ipAddr;
        }

        @Override
        public String subject() {
            return this.subject;
        }
    }

    /** */
    final class EndlessWaitException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor.
         */
        public EndlessWaitException() {
            super("");
        }
    }

    /**
     * Consumes an event.
     *
     * @param limitEnum
     * @param event
     *
     * @return {@code false} if consumption is denied at this moment in time or
     *         will never happen, because of an endless wait.
     */
    boolean consumeEvent(LimitEnum limitEnum, IEvent event);

    /**
     * Gives waiting time (milliseconds) until an event can be consumed (without
     * consuming it).
     *
     * @param limitEnum
     * @param event
     * @return waiting time or a negative value in case of an endless wait.
     */
    long waitTimeForEvent(LimitEnum limitEnum, IEvent event);

    /**
     * Consumes an event. If consumption fails, a notification message is send
     * to the {@link IRateLimiterListener} before a waiting time is applied
     * until a next event <i>can</i> be consumed.
     *
     * @param limitType
     *            {@link LimitEnum}.
     * @param event
     *            {@link RateLimiterService.IEvent}.
     * @param listener
     *            {@link IRateLimiterListener} that receives notification
     *            messages.
     *
     * @return time waited, zero if event was consumed.
     *
     * @throws EndlessWaitException
     *             in case of an endless wait or when {@link Thread#sleep(long)}
     *             was interrupted.
     */
    long consumeOrWaitForEvent(LimitEnum limitType,
            RateLimiterService.IEvent event, IRateLimiterListener listener)
            throws EndlessWaitException;

    /**
     * Removes idle rate limit instances of all limit types.
     *
     * @return number of rate limiters cleaned up (removed).
     */
    int cleanUp();

    /**
     * Removes idle rate limiter instances of a specific type of limit.
     *
     * @param limitEnum
     *            type of limit.
     * @return number of rate limiters cleaned up (removed).
     */
    int cleanUp(LimitEnum limitEnum);

}
