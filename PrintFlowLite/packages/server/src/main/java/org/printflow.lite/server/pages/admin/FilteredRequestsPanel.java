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
package org.printflow.lite.server.pages.admin;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.markup.html.basic.Label;
import org.printflow.lite.core.i18n.AdjectiveEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.json.JsonRollingTimeSeries;
import org.printflow.lite.core.json.TimeSeriesHelper;
import org.printflow.lite.core.json.TimeSeriesInterval;
import org.printflow.lite.server.DoSFilterMonitor;
import org.printflow.lite.server.HTTP2RateControlMonitor;
import org.printflow.lite.server.InetAccessFilter;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.pages.StatsTotalPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class FilteredRequestsPanel extends StatsTotalPanel {

    private static final long serialVersionUID = 1L;

    /** */
    private final boolean isDoSFilter;
    /** */
    private final boolean isInetAccessFilter;
    /** */
    private final boolean isHTTP2RateControl;

    /**
     * @param id
     *            Wicket ID.
     * @param doSFilter
     * @param inetAccessFilter
     * @param http2RateControl
     */
    public FilteredRequestsPanel(final String id, final boolean doSFilter,
            final boolean inetAccessFilter, final boolean http2RateControl) {
        super(id);
        this.isDoSFilter = doSFilter;
        this.isInetAccessFilter = inetAccessFilter;
        this.isHTTP2RateControl = http2RateControl;
    }

    /**
     *
     */
    @Override
    public void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        final DoSFilterMonitor dosFilterMonitor = DoSFilterMonitor.instance();
        final InetAccessFilter inetAccessFilter = InetAccessFilter.instance();
        final HTTP2RateControlMonitor http2RateControlMonitor =
                HTTP2RateControlMonitor.instance();

        helper.addLabel("header-title", DoSFilterMonitor.MSG_TITLE);

        // Header
        helper.addLabel("period", NounEnum.PERIOD);

        this.addHeaderLabel(helper, "delay", AdjectiveEnum.DELAYED,
                this.isDoSFilter);
        this.addHeaderLabel(helper, "throttle", AdjectiveEnum.THROTTLED,
                this.isDoSFilter);
        this.addHeaderLabel(helper, "reject", AdjectiveEnum.REJECTED,
                this.isDoSFilter);
        this.addHeaderLabel(helper, "abort", AdjectiveEnum.ABORTED,
                this.isDoSFilter);

        this.addHeaderLabel(helper, "denied", AdjectiveEnum.DENIED,
                this.isInetAccessFilter);

        this.addHeaderLabel(helper, "closed", AdjectiveEnum.CLOSED,
                this.isHTTP2RateControl);

        // Row 1,2
        helper.addLabel("hour", NounEnum.HOUR);
        helper.addLabel("day", NounEnum.DAY);

        //
        this.addLabelCount(helper, "delay-hour", TimeSeriesInterval.HOUR,
                dosFilterMonitor.getTimeSeriesDelayHour());
        this.addLabelCount(helper, "delay-day", TimeSeriesInterval.DAY,
                dosFilterMonitor.getTimeSeriesDelayDay());
        //
        this.addLabelCount(helper, "throttle-hour", TimeSeriesInterval.HOUR,
                dosFilterMonitor.getTimeSeriesThrottleHour());
        this.addLabelCount(helper, "throttle-day", TimeSeriesInterval.DAY,
                dosFilterMonitor.getTimeSeriesThrottleDay());
        //
        this.addLabelCount(helper, "reject-hour", TimeSeriesInterval.HOUR,
                dosFilterMonitor.getTimeSeriesRejectHour());
        this.addLabelCount(helper, "reject-day", TimeSeriesInterval.DAY,
                dosFilterMonitor.getTimeSeriesRejectDay());
        //
        this.addLabelCount(helper, "abort-hour", TimeSeriesInterval.HOUR,
                dosFilterMonitor.getTimeSeriesAbortHour());
        this.addLabelCount(helper, "abort-day", TimeSeriesInterval.DAY,
                dosFilterMonitor.getTimeSeriesAbortDay());

        //
        this.addLabelCount(helper, "denied-hour", TimeSeriesInterval.HOUR,
                inetAccessFilter.getTimeSeriesDeniedHour());
        this.addLabelCount(helper, "denied-day", TimeSeriesInterval.DAY,
                inetAccessFilter.getTimeSeriesDeniedDay());

        //
        this.addLabelCount(helper, "closed-hour", TimeSeriesInterval.HOUR,
                http2RateControlMonitor.getTimeSeriesClosedHour());
        this.addLabelCount(helper, "closed-day", TimeSeriesInterval.DAY,
                http2RateControlMonitor.getTimeSeriesClosedDay());

        //
        final Set<String> allSubjects = new HashSet<>();

        allSubjects.addAll(dosFilterMonitor.getAddressesLastDay());
        allSubjects.addAll(inetAccessFilter.getAddressesLastDay());
        allSubjects.addAll(http2RateControlMonitor.getSubjectsLastDay());

        final StringBuilder subjects = new StringBuilder();

        for (String s : allSubjects) {
            if (subjects.length() > 0) {
                subjects.append(", ");
            }
            subjects.append(s);
        }
        helper.encloseLabel("subjects-day", subjects.toString(),
                subjects.length() > 0);
    }

    /**
     * Adds a Wicket label for a time series count.
     *
     * @param helper
     * @param wicketId
     * @param intervalWlk
     * @param data
     */
    private void addLabelCount(final MarkupHelper helper, final String wicketId,
            final TimeSeriesInterval intervalWlk,
            final JsonRollingTimeSeries<Long> data) {

        final long total = (Long) TimeSeriesHelper.getIntervalTotal(data,
                intervalWlk, new Date());

        helper.addLabel(wicketId, helper.localizedNumberOrSpace(total))
                .setEscapeModelStrings(false);
    }

    /**
     * @param helper
     * @param wicketId
     * @param txt
     * @param isEnabled
     */
    private void addHeaderLabel(final MarkupHelper helper,
            final String wicketId, final AdjectiveEnum txt,
            final boolean isEnabled) {
        final Label label = helper.addLabel(wicketId, txt);
        if (!isEnabled) {
            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_STYLE,
                    MarkupHelper.ATTR_STYLE_RIGHT_TEXT_ALIGN_OPACITY_HALF);
        }
    }

}
