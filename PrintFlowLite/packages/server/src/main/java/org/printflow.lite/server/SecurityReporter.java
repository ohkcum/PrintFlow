/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jetty.http2.frames.FrameType;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.dao.enums.AppLogLevelEnum;
import org.printflow.lite.core.jpa.AppLog;
import org.printflow.lite.core.util.AppLogHelper;
import org.printflow.lite.core.util.ClockedSubjectList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SecurityReporter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SecurityReporter.class);

    /** */
    private static final int CLOCKED_LIST_CAPACITY = 50;

    /** */
    private ClockedSubjectList clockedSubjectList =
            new ClockedSubjectList(CLOCKED_LIST_CAPACITY);

    /** */
    private final AtomicLong intervalCountWebApp = new AtomicLong(0L);

    /** */
    private final AtomicLong intervalCountAppLog = new AtomicLong(0L);

    /** */
    private final AtomicLong lastMsgTimeWebApp = new AtomicLong(0L);

    /** */
    private final AtomicLong lastMsgTimeAppLog = new AtomicLong(0L);

    /**
     * Notification interval (seconds) for sending realtime message in Admin Web
     * App.
     */
    private final AtomicInteger msgIntervalRealtimeSecs = new AtomicInteger();

    /**
     * Notification interval (minutes) for writing message in {@link AppLog}.
     */
    private final AtomicInteger msgIntervalApplogMins = new AtomicInteger();

    /** */
    private final String msgFormat;

    /**
     *
     * @param title
     */
    public SecurityReporter(final String title) {
        this.msgFormat = title + ": %d";
    }

    /**
     * @param now
     * @param lastReportTime
     * @param intervalUnits
     *            Number of interval units
     * @param intervalUnitSecs
     *            Milliseconds of one (1) interval unit
     * @return {@code true} if interval reached.
     */
    private static boolean checkInterval(final Date now,
            final AtomicLong lastReportTime, final int intervalUnits,
            final long intervalUnitSecs) {

        if (lastReportTime.get() == 0 || now.getTime()
                - lastReportTime.get() > intervalUnits * intervalUnitSecs) {
            lastReportTime.set(now.getTime());
            return true;
        }
        return false;
    }

    /**
     * @return list with subjects observed last hour from now.
     */
    public List<String> getSubjectsLastHour() {
        return this.clockedSubjectList.lastHourSubjects();
    }

    /**
     * @return list with subjects observed last day from now.
     */
    public List<String> getSubjectsLastDay() {
        return this.clockedSubjectList.lastDaySubjects();
    }

    /**
     * @param value
     */
    public void setMsgIntervalApplogMins(final int value) {
        this.msgIntervalApplogMins.set(value);
    }

    /**
     * @param value
     */
    public void setMsgIntervalRealtimeSecs(final int value) {
        this.msgIntervalRealtimeSecs.set(value);
    }

    /**
     * Notifies a security message.
     *
     * @param request
     *            {@link HttpServletRequest}.
     * @param msg
     *            Message for {@link SecurityLogger}. If {@code null} no message
     *            is written.
     * @param now
     * @param clockSubject
     */
    public void onMessage(final HttpServletRequest request,
            final SecurityLogger.Message msg, final Date now,
            final boolean clockSubject) {

        this.onMessage(request.getRemoteAddr(), msg, now, clockSubject);
    }

    /**
     * Notifies an HTTP/2 security message.
     *
     * @param frameType
     *            HTTP/2 Frame Type that went over the limit. Frame types:
     *            <a href="https://www.rfc-editor.org/rfc/rfc9113.html"
     *            >RFC9113</a>.
     * @param msg
     *            Message for {@link SecurityLogger}. If {@code null} no message
     *            is written.
     * @param now
     * @param clockSubject
     */
    public void onMessage(final FrameType frameType,
            final SecurityLogger.Message msg, final Date now,
            final boolean clockSubject) {

        this.onMessage(frameType.toString(), msg, now, clockSubject);
    }

    /**
     * Notifies a security message.
     *
     * @param subject
     * @param msg
     *            Message for {@link SecurityLogger}. If {@code null} no message
     *            is written.
     * @param now
     * @param clockSubject
     */
    private void onMessage(final String subject,
            final SecurityLogger.Message msg, final Date now,
            final boolean clockSubject) {

        if (clockSubject) {
            this.clockedSubjectList.put(subject);
            this.intervalCountWebApp.incrementAndGet();
            this.intervalCountAppLog.incrementAndGet();
        }

        if (msg != null) {
            SecurityLogger.log(now, msg);
        }

        /*
         * Notification thresholds reached?
         */
        if (checkInterval(now, lastMsgTimeWebApp, msgIntervalRealtimeSecs.get(),
                DateUtils.MILLIS_PER_SECOND)) {

            final String warn = String.format(this.msgFormat,
                    this.intervalCountWebApp.getAndSet(0L));

            AdminPublisher.instance().publishAndPopup(PubTopicEnum.SYSTEM,
                    PubLevelEnum.WARN, warn);

            LOGGER.warn(warn);
        }

        if (checkInterval(now, lastMsgTimeAppLog, msgIntervalApplogMins.get(),
                DateUtils.MILLIS_PER_MINUTE)) {

            AppLogHelper.log(AppLogLevelEnum.WARN, String.format(this.msgFormat,
                    this.intervalCountAppLog.getAndSet(0L)));
        }
    }

}
