/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlets.DoSFilter;
import org.printflow.lite.core.json.JsonRollingTimeSeries;
import org.printflow.lite.core.json.TimeSeriesInterval;
import org.printflow.lite.core.util.CidrChecker;
import org.printflow.lite.core.util.InetUtils;

/**
 * Restricts access on remote IP address. This class is modeled after
 * {@link DoSFilter}.
 *
 * @author Rijk Ravestein
 *
 */
public final class InetAccessFilter implements Filter {

    /** */
    private static final String MSG_TITLE = "Denied Requests";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link DoSFilterMonitor#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        /** */
        public static final InetAccessFilter INSTANCE = new InetAccessFilter();
    }

    /** */
    private volatile boolean enabled;

    /** */
    private volatile List<CidrChecker> blackListCidr = new ArrayList<>();

    /** */
    private volatile Set<String> blackListAddr = new HashSet<>();

    /** */
    private volatile List<CidrChecker> whiteListCidr = new ArrayList<>();

    /** */
    private volatile boolean whitelistEmptyAllowAll;

    /** */
    private volatile Set<String> whiteListAddr = new HashSet<>();
    /**
     * Max number of {@link TimeSeriesInterval#HOUR} intervals.
     */
    private static final int TIME_SERIES_INTERVAL_HOUR_MAX_POINTS = 24;

    /**
     * Max points of {@link TimeSeriesInterval#DAY} intervals.
     */
    private static final int TIME_SERIES_INTERVAL_DAY_MAX_POINTS = 30;

    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesDeniedHour =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.HOUR,
                    TIME_SERIES_INTERVAL_HOUR_MAX_POINTS, 0L);

    /** */
    private final JsonRollingTimeSeries<Long> timeSeriesDeniedDay =
            new JsonRollingTimeSeries<Long>(TimeSeriesInterval.DAY,
                    TIME_SERIES_INTERVAL_DAY_MAX_POINTS, 0L);

    /** */
    private final SecurityReporter securityReporter =
            new SecurityReporter(MSG_TITLE);

    /**
     * Just one static instance.
     */
    private InetAccessFilter() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static InetAccessFilter instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * @return whether this filter is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @param enable
     *            whether this filter is enabled
     */
    public void setEnabled(final boolean enable) {
        this.enabled = enable;
    }

    /**
     * @return {@code true} if empty white list is treated to allow all.
     */
    public boolean isWhitelistEmptyAllowAll() {
        return this.whitelistEmptyAllowAll;
    }

    /**
     * @param allowAll
     *            {@code true} if empty white list is treated to allow all.
     */
    public void setWhitelistEmptyAllowAll(final boolean allowAll) {
        this.whitelistEmptyAllowAll = allowAll;
    }

    /**
     * @param blackList
     */
    public void setBlackListCidr(final List<CidrChecker> blackList) {
        this.blackListCidr = blackList;
    }

    /**
     * @param blackList
     */
    public void setBlackListAddr(final Set<String> blackList) {
        this.blackListAddr = blackList;
    }

    /**
     * @param whiteList
     */
    public void setWhiteListCidr(final List<CidrChecker> whiteList) {
        this.whiteListCidr = whiteList;
    }

    /**
     * @param whiteList
     */
    public void setWhiteListAddr(final Set<String> whiteList) {
        this.whiteListAddr = whiteList;
    }

    /**
     * @param value
     */
    public void setMsgIntervalApplogMins(final int value) {
        this.securityReporter.setMsgIntervalApplogMins(value);
    }

    /**
     * @param value
     */
    public void setMsgIntervalRealtimeSecs(final int value) {
        this.securityReporter.setMsgIntervalRealtimeSecs(value);
    }

    /**
     * @return denied requests per hour.
     */
    public JsonRollingTimeSeries<Long> getTimeSeriesDeniedHour() {
        return this.timeSeriesDeniedHour;
    }

    /**
     * @return denied requests per day.
     */
    public JsonRollingTimeSeries<Long> getTimeSeriesDeniedDay() {
        return this.timeSeriesDeniedDay;
    }

    /**
     * @return last day denied addresses.
     */
    public List<String> getAddressesLastDay() {
        return this.securityReporter.getSubjectsLastDay();
    }

    @Override
    public void doFilter(final ServletRequest request,
            final ServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {

        this.doFilter((HttpServletRequest) request,
                (HttpServletResponse) response, filterChain);
    }

    /**
     * @param request
     * @return {@code true} if allowed.
     */
    private boolean isRequestAllowed(final HttpServletRequest request) {

        final String remoteAddr = AbstractFilterConfigurator
                .stripIpv6Brackets(request.getRemoteAddr());

        /*
         * (0) Loop-back requests are always allowed.
         */
        if (remoteAddr.equals(InetUtils.IPV4_LOOP_BACK_ADDR)
                || remoteAddr.equals(InetUtils.IPV6_LOOP_BACK_ADDR_FULL)) {
            return true;
        }

        /*
         * (1) Black list entries are always applied, so that even if an entry
         * matches the white list, a black list entry will override it.
         */
        if (this.blackListAddr.contains(remoteAddr)) {
            return false;
        }
        for (final CidrChecker cidr : this.blackListCidr) {
            if (cidr.isInRange(remoteAddr)) {
                return false;
            }
        }

        /*
         * (2) Check if empty white list means allow all.
         */
        if (this.whitelistEmptyAllowAll && this.whiteListAddr.isEmpty()
                && this.whiteListCidr.isEmpty()) {
            return true;
        }

        /*
         * (3) Allow requests that match the white list.
         */
        if (this.whiteListAddr.contains(remoteAddr)) {
            return true;
        }
        for (final CidrChecker cidr : this.whiteListCidr) {
            if (cidr.isInRange(remoteAddr)) {
                return true;
            }
        }

        /*
         * (4) No white list match found.
         */
        return false;
    }

    /**
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    private void doFilter(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {

        if (this.enabled && !this.isRequestAllowed(request)) {

            final Date now = new Date();

            this.timeSeriesDeniedHour.addDataPoint(now, 1L);
            this.timeSeriesDeniedDay.addDataPoint(now, 1L);

            final SecurityLogger.Message msg;

            if (SecurityLogger.isEnabled()) {
                msg = new SecurityLogger.Message();
                msg.append(this.getClass().getSimpleName());
                msg.append(request.getRemoteAddr()).append("DENIED");
                msg.append(request.getScheme());
                msg.append(request.getMethod());
                msg.append(request.getRequestURI());
                SecurityLogger.log(new Date(), msg);
            } else {
                msg = null;
            }

            this.securityReporter.onMessage(request, msg, now, true);

            /*
             * sendError(...) results in an ERROR dispatch back to the
             * ServletContext. SC_FORBIDDEN (403)
             */
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Access denied from your location.");
            return;
        }

        filterChain.doFilter(request, response);
    }

}
