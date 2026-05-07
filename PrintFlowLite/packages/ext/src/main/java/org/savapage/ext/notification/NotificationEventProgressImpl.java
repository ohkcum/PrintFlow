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

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a progress report about the number of times a notification
 * event was accepted and rejected.
 *
 * @author Rijk Ravestein
 *
 */
public final class NotificationEventProgressImpl
        implements NotificationEventProgress {

    /** */
    private final Map<Class<? extends NotificationPlugin>, Integer> //
    acceptedByClass;
    /** */
    private final Map<Class<? extends NotificationPlugin>, Integer> //
    rejectedByClass;

    /** */
    private int accepted;
    /** */
    private int rejected;

    /**
     * Constructor.
     */
    public NotificationEventProgressImpl() {
        this.accepted = 0;
        this.rejected = 0;
        this.acceptedByClass = new HashMap<>();
        this.rejectedByClass = new HashMap<>();
    }

    /**
     * Accepts an event.
     *
     * @param plugin
     *            The plugin.
     */
    public void onAccept(final NotificationPlugin plugin) {
        this.accepted++;
        addEvent(this.acceptedByClass, plugin.getClass());
    }

    /**
     * Rejects an event.
     *
     * @param plugin
     *            The plugin.
     */
    public void onReject(final NotificationPlugin plugin) {
        this.rejected++;
        addEvent(this.rejectedByClass, plugin.getClass());
    }

    @Override
    public int acceptedEvents() {
        return this.accepted;
    }

    @Override
    public int rejectedEvents() {
        return this.rejected;
    }

    @Override
    public int acceptedEvents(final Class<? extends NotificationPlugin> clazz) {
        return countEvents(this.acceptedByClass, clazz);
    }

    @Override
    public int rejectedEvents(final Class<? extends NotificationPlugin> clazz) {
        return countEvents(this.rejectedByClass, clazz);
    }

    /**
     *
     * @param map
     *            The counter.
     * @param clazz
     *            The plugin class.
     * @return Number of events from plugin class so far.
     */
    private static int countEvents(
            final Map<Class<? extends NotificationPlugin>, Integer> map,
            final Class<? extends NotificationPlugin> clazz) {
        if (map.containsKey(clazz)) {
            return map.get(clazz).intValue();
        }
        return 0;
    }

    /**
     *
     * @param map
     *            The counter.
     * @param clazz
     *            The plugin class.
     */
    private static void addEvent(
            final Map<Class<? extends NotificationPlugin>, Integer> map,
            final Class<? extends NotificationPlugin> clazz) {

        final int counter;
        if (map.containsKey(clazz)) {
            counter = map.get(clazz).intValue();
        } else {
            counter = 0;
        }
        map.put(clazz, Integer.valueOf(counter + 1));
    }

}
