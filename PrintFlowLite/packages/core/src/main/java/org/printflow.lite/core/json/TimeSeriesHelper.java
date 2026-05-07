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
package org.printflow.lite.core.json;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.SpException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TimeSeriesHelper implements IUtility {

    /** Utility class. */
    private TimeSeriesHelper() {
    }

    /**
     * Traverses back in time to find the interval that matches the reference
     * date to return its total.
     *
     * @param timeSeries
     *            source
     * @param timeInterval
     *            interval
     * @param referenceDate
     *            reference
     * @return Interval total (can be {@code 0}). If no matching interval is
     *         found {@code 0} is returned.
     */
    public static Number getIntervalTotal(
            final JsonRollingTimeSeries<? extends Number> timeSeries,
            final TimeSeriesInterval timeInterval, final Date referenceDate) {

        final Number nZero = timeSeries.getZeroNumber();

        // No intervals present.
        if (timeSeries.getData() == null) {
            return nZero;
        }

        /*
         * Traverse back in time to find matching interval. Start off with first
         * interval.
         */
        Date intervalStartCur = new Date(timeSeries.getLastTime());

        for (final Number total : timeSeries.getData()) {

            Date intervalStartPrv; // Previous in time, next in the list.
            Date intervalEndCur;

            switch (timeInterval) {
            case DAY:
                intervalStartPrv = DateUtils.addDays(intervalStartCur, -1);
                intervalEndCur = DateUtils.addDays(intervalStartCur, 1);
                break;
            case MONTH:
                intervalStartPrv = DateUtils.addMonths(intervalStartCur, -1);
                intervalEndCur = DateUtils.addMonths(intervalStartCur, 1);
                break;
            case WEEK:
                intervalStartPrv = DateUtils.addWeeks(intervalStartCur, -1);
                intervalEndCur = DateUtils.addWeeks(intervalStartCur, 1);
                break;
            case HOUR:
                intervalStartPrv = DateUtils.addHours(intervalStartCur, -1);
                intervalEndCur = DateUtils.addHours(intervalStartCur, 1);
                break;
            default:
                throw new SpException(
                        "Oops missed interval [" + timeInterval + "]");
            }

            // Reference date is more recent then current interval, no need to
            // traverse back in time.
            if (referenceDate.getTime() > intervalEndCur.getTime()) {
                return nZero;
            }

            // Within range?
            if (referenceDate.getTime() >= intervalStartCur.getTime()
                    && referenceDate.getTime() < intervalEndCur.getTime()) {
                return total;
            }

            // Next interval (back in time).
            intervalStartCur = intervalStartPrv;
        }

        // No interval found
        return nZero;
    }

}
