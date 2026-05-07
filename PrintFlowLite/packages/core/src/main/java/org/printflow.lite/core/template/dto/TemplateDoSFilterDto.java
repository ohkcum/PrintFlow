/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.template.dto;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.printflow.lite.core.json.TimeSeriesHelper;
import org.printflow.lite.core.json.TimeSeriesInterval;
import org.printflow.lite.ext.server.IDoSFilterStatistics;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateDoSFilterDto implements TemplateDto {

    public static class Counter {

        private String summary;
        private Number all;
        private Number delayed;
        private Number throttled;
        private Number rejected;
        private Number aborted;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public Number getAll() {
            return all;
        }

        public void setAll(Number all) {
            this.all = all;
        }

        public Number getDelayed() {
            return delayed;
        }

        public void setDelayed(Number delayed) {
            this.delayed = delayed;
        }

        public Number getThrottled() {
            return throttled;
        }

        public void setThrottled(Number throttled) {
            this.throttled = throttled;
        }

        public Number getRejected() {
            return rejected;
        }

        public void setRejected(Number rejected) {
            this.rejected = rejected;
        }

        public Number getAborted() {
            return aborted;
        }

        public void setAborted(Number aborted) {
            this.aborted = aborted;
        }

    }

    /** */
    private Counter today;

    /** */
    private Counter yesterday;

    public Counter getToday() {
        return today;
    }

    public void setToday(Counter today) {
        this.today = today;
    }

    public Counter getYesterday() {
        return yesterday;
    }

    public void setYesterday(Counter yesterday) {
        this.yesterday = yesterday;
    }

    /**
     * @param stats
     * @param observationTime
     * @return counter
     */
    private static Counter createCounter(final IDoSFilterStatistics stats,
            final Date observationTime) {

        final Counter counter = new Counter();
        counter.setDelayed(
                TimeSeriesHelper.getIntervalTotal(stats.getTimeSeriesDelayDay(),
                        TimeSeriesInterval.DAY, observationTime));

        counter.setThrottled(TimeSeriesHelper.getIntervalTotal(
                stats.getTimeSeriesThrottleDay(), TimeSeriesInterval.DAY,
                observationTime));

        counter.setRejected(TimeSeriesHelper.getIntervalTotal(
                stats.getTimeSeriesRejectDay(), TimeSeriesInterval.DAY,
                observationTime));

        counter.setAborted(
                TimeSeriesHelper.getIntervalTotal(stats.getTimeSeriesAbortDay(),
                        TimeSeriesInterval.DAY, observationTime));

        counter.setAll(counter.getAborted().longValue()
                + counter.getDelayed().longValue()
                + counter.getRejected().longValue()
                + counter.getThrottled().longValue());

        if (counter.getAll().longValue() > 0) {
            counter.setSummary(counter.getAll().toString());
        } else {
            counter.setSummary(null);
        }

        return counter;
    }

    /**
     * Creates template from info.
     *
     * @param stats
     *            {@link IDoSFilterStatistics}.
     * @return template.
     */
    public static TemplateDoSFilterDto
            create(final IDoSFilterStatistics stats) {

        final Date today = new Date();
        final Date yesterday = DateUtils.addDays(today, -1);

        final TemplateDoSFilterDto dto = new TemplateDoSFilterDto();

        dto.setToday(createCounter(stats, today));
        dto.setYesterday(createCounter(stats, yesterday));

        return dto;
    }
}
