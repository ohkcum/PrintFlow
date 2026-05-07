/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.ratelimiter;

/**
 * Interface for limiting action in response to a succession of events. Each
 * event is assigned a specific weight. Implementing classes evaluate event
 * density and decide if an event can be handled (consumed) immediately or after
 * a waiting period (which can be endless, so the event can be dropped).
 *
 * @author Rijk Ravestein
 *
 */
public interface IRateLimiter {

    /**
     * Consumes an event with weight 1.0.
     *
     * @return {@code false} if consumption is denied at this moment in time or
     *         will never happen, because of an endless wait.
     */
    boolean consumeEvent();

    /**
     * Consumes a weighted event.
     *
     * @param weight
     * @return {@code false} if consumption is denied at this moment in time or
     *         will never happen, because of an endless wait condition.
     */
    boolean consumeEvent(double weight);

    /**
     * Gives waiting time (milliseconds) until an event with weight 1.0 can be
     * consumed (without consuming it).
     *
     * @return waiting time or a negative value in case of an endless wait.
     */
    long waitTimeForEvent();

    /**
     * Gives waiting time (milliseconds) until a weighted event can be consumed
     * (without consuming it).
     *
     * @param weight
     * @return waiting time or a negative value in case of an endless wait.
     */
    long waitTimeForEvent(double weight);

    /**
     * @return {@code true} if consumed events occurred a long time ago and
     *         therefore are no longer relevant for rate limiting.
     */
    boolean isIdle();

}
