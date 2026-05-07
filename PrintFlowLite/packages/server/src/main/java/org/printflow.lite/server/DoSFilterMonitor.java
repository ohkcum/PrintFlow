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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DurationUtils;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.servlets.DoSFilter.Action;
import org.eclipse.jetty.servlets.DoSFilter.OverLimit;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.json.JsonRollingTimeSeries;
import org.printflow.lite.core.json.TimeSeriesInterval;
import org.printflow.lite.ext.server.IDoSFilterStatistics;

/**
 * {@link DoSFilter.Listener} for actions taken against specific requests.
 */
public final class DoSFilterMonitor extends DoSFilter.Listener
        implements IDoSFilterStatistics {

    /** */
    public static final String MSG_TITLE = "Filtered Requests";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link DoSFilterMonitor#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static final class SingletonHolder {
        /** */
        public static final DoSFilterMonitor INSTANCE = new DoSFilterMonitor();
    }

    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesDelayHour =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.HOUR,
                    TIME_SERIES_INTERVAL_HOUR_MAX_POINTS, 0L);
    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesRejectHour =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.HOUR,
                    TIME_SERIES_INTERVAL_HOUR_MAX_POINTS, 0L);
    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesThrottleHour =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.HOUR,
                    TIME_SERIES_INTERVAL_HOUR_MAX_POINTS, 0L);
    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesAbortHour =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.HOUR,
                    TIME_SERIES_INTERVAL_HOUR_MAX_POINTS, 0L);

    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesDelayDay =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.DAY,
                    TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0L);
    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesRejectDay =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.DAY,
                    TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0L);
    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesThrottleDay =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.DAY,
                    TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0L);
    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesAbortDay =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.DAY,
                    TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0L);

    /** */
    private final SecurityReporter securityReporter =
            new SecurityReporter(MSG_TITLE);

    /** */
    private DoSFilterMonitor() {
        this.init();
    }

    @Override
    public void init() {
        for (IConfigProp.Key key : ConfigManager.getDoSFilterKeysWarn()) {
            this.onConfig(key);
        }
    }

    /**
     * @param key
     *            Configuration key.
     */
    private void onConfig(final IConfigProp.Key key) {

        switch (key) {

        case SYS_DOSFILTER_WARN_INTERVAL_APPLOG_MINS:
            this.securityReporter.setMsgIntervalApplogMins(
                    ConfigManager.instance().getConfigInt(key));
            break;

        case SYS_DOSFILTER_WARN_INTERVAL_WEBAPP_SECS:
            this.securityReporter.setMsgIntervalRealtimeSecs(
                    ConfigManager.instance().getConfigInt(key));
            break;

        default:
            throw new SpException(key.toString() + " NOT handled.");
        }
    }

    /**
     * @param key
     *            Configuration key.
     */
    public static void onConfigChanged(final IConfigProp.Key key) {
        instance().onConfig(key);
    }

    @Override
    public List<String> getAddressesLastHour() {
        return this.securityReporter.getSubjectsLastHour();
    }

    @Override
    public List<String> getAddressesLastDay() {
        return this.securityReporter.getSubjectsLastDay();
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesDelayHour() {
        return timeSeriesDelayHour;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesRejectHour() {
        return timeSeriesRejectHour;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesThrottleHour() {
        return timeSeriesThrottleHour;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesAbortHour() {
        return timeSeriesAbortHour;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesDelayDay() {
        return timeSeriesDelayDay;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesRejectDay() {
        return timeSeriesRejectDay;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesThrottleDay() {
        return timeSeriesThrottleDay;
    }

    @Override
    public JsonRollingTimeSeries<Long> getTimeSeriesAbortDay() {
        return timeSeriesAbortDay;
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static DoSFilterMonitor instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Process the onRequestOverLimit() behavior.
     *
     * @param request
     *            the request that is over the limit
     * @param dosFilter
     *            the {@link DoSFilter} that this event occurred on
     * @return the action to actually perform.
     */
    @Override
    public Action onRequestOverLimit(final HttpServletRequest request,
            final OverLimit overlimit, final DoSFilter dosFilter) {

        final Date now = new Date();

        final Action action = Action.fromDelay(dosFilter.getDelayMs());

        final String remoteAddr = request.getRemoteAddr();

        final long count = overlimit.getCount();
        final int msecOverlimit =
                DurationUtils.toMillisInt(overlimit.getDuration());

        boolean clockAddress = true;

        final SecurityLogger.Message msg;

        if (SecurityLogger.isEnabled()) {
            msg = new SecurityLogger.Message();
            msg.append(DoSFilter.class.getSimpleName());
            msg.append(remoteAddr).append(action.toString());
        } else {
            msg = null;
        }

        switch (action) {
        case DELAY:
            this.timeSeriesDelayHour.addDataPoint(now, 1L);
            this.timeSeriesDelayDay.addDataPoint(now, 1L);
            if (msg != null) {
                msg.append(dosFilter.getDelayMs());
                msg.append("msec");
            }
            break;
        case THROTTLE:
            this.timeSeriesThrottleHour.addDataPoint(now, 1L);
            this.timeSeriesThrottleDay.addDataPoint(now, 1L);
            break;
        case REJECT:
            this.timeSeriesRejectHour.addDataPoint(now, 1L);
            this.timeSeriesRejectDay.addDataPoint(now, 1L);
            break;
        case ABORT:
            this.timeSeriesAbortHour.addDataPoint(now, 1L);
            this.timeSeriesAbortDay.addDataPoint(now, 1L);
            break;
        case NO_ACTION:
        default:
            clockAddress = false;
            break;
        }

        if (msg != null && msg.length() > 0) {
            msg.append(count).append("Overlimit");
            msg.append(msecOverlimit).append("msec");
            msg.append(request.getScheme());
            msg.append(request.getMethod());
            msg.append(request.getRequestURI());
        }

        this.securityReporter.onMessage(request, msg, now, clockAddress);

        return action;
    }

}
