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

import java.util.Date;
import java.util.List;

import org.eclipse.jetty.http2.frames.FrameType;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.json.JsonRollingTimeSeries;
import org.printflow.lite.core.json.TimeSeriesInterval;
import org.printflow.lite.ext.server.IServiceFilterStatistics;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class HTTP2RateControlMonitor implements IServiceFilterStatistics {

    /** */
    public static final String MSG_TITLE =
            "HTTP/2 Rate Limited Requests (Closed)";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link HTTP2RateControlMonitor#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static final class SingletonHolder {
        /** */
        public static final HTTP2RateControlMonitor INSTANCE =
                new HTTP2RateControlMonitor();
    }

    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesAbortHour =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.HOUR,
                    TIME_SERIES_INTERVAL_HOUR_MAX_POINTS, 0L);

    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesAbortDay =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.DAY,
                    TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0L);

    /** */
    private final SecurityReporter securityReporter =
            new SecurityReporter(MSG_TITLE);

    /** */
    private HTTP2RateControlMonitor() {
        this.init();
    }

    @Override
    public void init() {
        for (IConfigProp.Key key : ConfigManager
                .getHTTP2MaxRequestsKeysWarn()) {
            this.onConfig(key);
        }
    }

    /**
     * @param key
     *            Configuration key.
     */
    private void onConfig(final IConfigProp.Key key) {

        switch (key) {

        case SYS_HTTP2_MAX_REQUESTS_WARN_INTERVAL_APPLOG_MINS:
            this.securityReporter.setMsgIntervalApplogMins(
                    ConfigManager.instance().getConfigInt(key));
            break;

        case SYS_HTTP2_MAX_REQUESTS_WARN_INTERVAL_WEBAPP_SECS:
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

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static HTTP2RateControlMonitor instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * @return closed connections per hour.
     */
    public JsonRollingTimeSeries<Long> getTimeSeriesClosedHour() {
        return this.timeSeriesAbortHour;
    }

    /**
     * @return closed connections per day.
     */
    public JsonRollingTimeSeries<Long> getTimeSeriesClosedDay() {
        return this.timeSeriesAbortDay;
    }

    /**
     * @return last day HTTP/2 frames that triggered closed connections.
     */
    public List<String> getSubjectsLastDay() {
        return this.securityReporter.getSubjectsLastDay();
    }

    /**
     * @param frameType
     *            HTTP/2 Frame Type that went over the limit. Frame types:
     *            <a href="https://www.rfc-editor.org/rfc/rfc9113.html"
     *            >RFC9113</a>.
     */
    public void onRateBeyondLimit(final FrameType frameType) {

        final Date now = new Date();

        this.timeSeriesAbortHour.addDataPoint(now, 1L);
        this.timeSeriesAbortDay.addDataPoint(now, 1L);

        final SecurityLogger.Message msg;

        if (SecurityLogger.isEnabled()) {
            msg = new SecurityLogger.Message();
            msg.append(HTTP2RateControl.class.getSimpleName());
            msg.append(frameType.toString());
        } else {
            msg = null;
        }

        this.securityReporter.onMessage(frameType, msg, now, true);
    }

}
