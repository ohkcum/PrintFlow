/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.ext.notification;

/**
 * A progress report about the number of times a notification event was accepted
 * and rejected.
 *
 * @author Rijk Ravestein
 *
 */
public interface NotificationEventProgress {

    /**
     * @return Number of times a notification event was accepted up to now.
     */
    int acceptedEvents();

    /**
     * @return Number of times a notification event was rejected up to now.
     */
    int rejectedEvents();

    /**
     * @param clazz
     *            {@link NotificationPlugin} class.
     * @return Number of times a notification event was accepted by instances of
     *         plugin class up to now.
     */
    int acceptedEvents(Class<? extends NotificationPlugin> clazz);

    /**
     * @param clazz
     *            {@link NotificationPlugin} class.
     * @return Number of times a notification event was rejected by instances of
     *         plugin class up to now.
     */
    int rejectedEvents(Class<? extends NotificationPlugin> clazz);

}
