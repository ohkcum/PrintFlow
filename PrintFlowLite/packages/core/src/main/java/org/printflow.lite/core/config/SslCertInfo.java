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
package org.printflow.lite.core.config;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.util.DateUtil;
import org.printflow.lite.core.util.LocaleHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SslCertInfo {

    /** */
    private static final long DAYS_IN_MONTH = 30;

    /** */
    private static final long DAYS_IN_YEAR = 365;

    /** */
    private final String issuerCN;
    /** */
    private final String subjectCN;
    /** */
    private final Date creationDate;
    /** */
    private final Date notAfter;
    /** */
    private final boolean selfSigned;
    /** */
    private Set<String> subjectAltNames;

    @SuppressWarnings("unused")
    private SslCertInfo() {
        this.issuerCN = null;
        this.subjectCN = null;
        this.creationDate = null;
        this.notAfter = null;
        this.selfSigned = false;
        this.subjectAltNames = new HashSet<String>();
    }

    public SslCertInfo(final String issuerCN, final String subjectCN,
            final Date creationDate, final Date notAfter,
            final boolean selfSigned) {
        this.issuerCN = issuerCN;
        this.subjectCN = subjectCN;
        this.creationDate = creationDate;
        this.notAfter = notAfter;
        this.selfSigned = selfSigned;
        this.subjectAltNames = new HashSet<String>();
    }

    public String getIssuerCN() {
        return issuerCN;
    }

    public String getSubjectCN() {
        return subjectCN;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public boolean isSelfSigned() {
        return selfSigned;
    }

    public Set<String> getSubjectAltNames() {
        return subjectAltNames;
    }

    public void setSubjectAltNames(Set<String> altNames) {
        this.subjectAltNames = altNames;
    }

    public boolean isComplete() {
        return this.issuerCN != null && this.subjectCN != null
                && this.creationDate != null && this.notAfter != null;
    }

    /**
     * Checks if notAfter date is within a year.
     *
     * @param dateRef
     *            Reference date.
     * @return {@code true} when warning is applicable.
     */
    public boolean isNotAfterWithinYear(final Date dateRef) {
        final long delta = this.getNotAfter().getTime() - dateRef.getTime();
        return delta < DateUtil.DURATION_MSEC_DAY * DAYS_IN_YEAR;
    }

    /**
     * Checks if notAfter date is within a month.
     *
     * @param dateRef
     *            Reference date.
     * @return {@code true} when warning is applicable.
     */
    public boolean isNotAfterWithinMonth(final Date dateRef) {
        final long delta = this.getNotAfter().getTime() - dateRef.getTime();
        return delta < DateUtil.DURATION_MSEC_DAY * DAYS_IN_MONTH;
    }

    /**
     * Checks if notAfter date is within a day.
     *
     * @param dateRef
     *            Reference date.
     * @return {@code true} when error is applicable.
     */
    public boolean isNotAfterWithinDay(final Date dateRef) {
        final long delta = this.getNotAfter().getTime() - dateRef.getTime();
        return delta < DateUtil.DURATION_MSEC_DAY;
    }

    /**
     * @param pfx
     *            prefix
     */
    public void logInfo(final String pfx) {

        final LocaleHelper helperEN = new LocaleHelper(Locale.ENGLISH);
        final StringBuilder logMsg = new StringBuilder();

        SpInfo.instance().log(pfx + " SSL Certificate info.");

        if (this.getIssuerCN() != null) {
            logMsg.setLength(0);
            logMsg.append("... " + pfx + " SSL Cert Issuer  [")
                    .append(this.getIssuerCN()).append("]");
            if (this.isSelfSigned()) {
                logMsg.append(" self-signed.");
            }
            SpInfo.instance().log(logMsg.toString());
        }
        if (this.getSubjectCN() != null) {
            logMsg.setLength(0);
            logMsg.append("... " + pfx + " SSL Cert Subject [")
                    .append(this.getSubjectCN()).append("]");
            SpInfo.instance().log(logMsg.toString());
        }
        if (this.getCreationDate() != null) {
            logMsg.setLength(0);
            logMsg.append("... " + pfx + " SSL Cert Created [")
                    .append(helperEN
                            .getLongMediumDateTime(this.getCreationDate()))
                    .append("]");
            SpInfo.instance().log(logMsg.toString());
        }
        if (this.getNotAfter() != null) {
            logMsg.setLength(0);
            logMsg.append("... " + pfx + " SSL Cert Expires [")
                    .append(helperEN.getLongMediumDateTime(this.getNotAfter()))
                    .append("]");
            SpInfo.instance().log(logMsg.toString());
        }
        logMsg.setLength(0);
        logMsg.append("... " + pfx + " SSL Cert Subject Alt Names ")
                .append(this.getSubjectAltNames().toString());
        SpInfo.instance().log(logMsg.toString());
    }
}
