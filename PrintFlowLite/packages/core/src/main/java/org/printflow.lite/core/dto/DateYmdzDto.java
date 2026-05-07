/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.core.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.printflow.lite.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Date split in components.
 *
 * @author Rijk Ravestein
 *
 */
public class DateYmdzDto extends JsonAbstractBase {

    /** */
    @JsonProperty("ccyy")
    private int year;

    /** */
    @JsonProperty("mm")
    private int month = 1;

    /** */
    @JsonProperty("dd")
    private int day = 1;

    /** */
    @JsonProperty("zzz")
    private String zone = "Z";

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    /**
     * @return Date.
     * @throws ParseException
     */
    public Date asDate() throws ParseException {
        final String pattern = "yyyyMMdd'T'HHmmssX";
        final SimpleDateFormat fm = new SimpleDateFormat();
        fm.applyPattern(pattern);
        final String fmDate = String.format("%04d%02d%02dT000000%s", this.year,
                this.month, this.day, this.zone);
        return fm.parse(fmDate);
    }

    /**
     * @return Time.
     * @throws ParseException
     */
    public long asTime() throws ParseException {
        return this.asDate().getTime();
    }
}
