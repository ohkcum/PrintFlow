/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Synchronized list of subjects with last time observed.
 *
 * @author Rijk Ravestein
 *
 */
public final class ClockedSubjectList {

    /**
     * Last time observed by subject: mirror of {@link #subjectsBylastTime}.
     */
    private final Map<String, Long> lastTimeBySubject = new HashMap<>();

    /**
     * Subjects by last time observed: mirror of {@link #lastTimeBySubject}.
     */
    private final TreeMap<Long, String> subjectsBylastTime = new TreeMap<>();

    /**
     * Max unique subjects to observe.
     */
    private final int maxSize;

    /**
     * @param capacity
     *            Max unique subjects to observe.
     */
    public ClockedSubjectList(final int capacity) {
        this.maxSize = capacity;
    }

    /**
     * Removes oldest.
     */
    private void removeOldest() {
        final Long oldestTime = this.subjectsBylastTime.firstKey();
        final String oldestSender = this.subjectsBylastTime.remove(oldestTime);
        this.lastTimeBySubject.remove(oldestSender);
    }

    /**
     * @param subject
     */
    private void addSubject(final String subject) {

        final Long prevTime = this.lastTimeBySubject.get(subject);

        if (prevTime == null) {
            if (this.lastTimeBySubject.size() == this.maxSize) {
                this.removeOldest(); // make room
            }
        } else {
            this.subjectsBylastTime.remove(prevTime);
        }
        final long now = new Date().getTime();

        this.lastTimeBySubject.put(subject, now);
        this.subjectsBylastTime.put(now, subject);
    }

    /**
     * @param subject
     *            to put on the list
     */
    public synchronized void put(final String subject) {
        try {
            // Force unique time.
            TimeUnit.MILLISECONDS.sleep(1);
            this.addSubject(subject);
        } catch (InterruptedException e) {
            // no code intended
        }
    }

    @Override
    public synchronized String toString() {
        final StringBuilder b = new StringBuilder();
        for (Entry<Long, String> entry : this.subjectsBylastTime.entrySet()) {
            b.append(DateUtil.localizedLongMediumDateTime(
                    new Date(entry.getKey()), Locale.ENGLISH));
            b.append(" : ").append(entry.getValue()).append("\n");
        }
        return b.toString();
    }

    /**
     * @param splitTime
     * @return list with subjects observed after split time
     */
    private List<String> lastTimeSubjects(final long splitTime) {
        final List<String> list = new ArrayList<>();
        for (Entry<Long, String> entry : this.subjectsBylastTime.entrySet()) {
            if (entry.getKey().longValue() < splitTime) {
                break;
            }
            list.add(entry.getValue());
        }
        return list;
    }

    /**
     * @return list with subjects observed last hour from now.
     */
    public synchronized List<String> lastHourSubjects() {
        return this.lastTimeSubjects(
                new Date().getTime() - DateUtil.DURATION_MSEC_HOUR);
    }

    /**
     * @return list with subjects observed last day from now.
     */
    public synchronized List<String> lastDaySubjects() {
        return this.lastTimeSubjects(
                new Date().getTime() - DateUtil.DURATION_MSEC_DAY);
    }
}
