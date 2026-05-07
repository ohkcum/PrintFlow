/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.util.BigDecimalUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateAmountTotalDto implements TemplateDto {

    /** */
    private static int NUMBER_OF_DECIMALS = 2;

    /** */
    private static long CENTS_IN_AMOUNT_UNIT = 100;

    /**
     * Number of amount values.
     */
    private String count;

    /**
     * Formatted sum of amounts (decimal point, two decimals).
     */
    private String sum;

    /**
     * @param n
     *            Number of amount values.
     * @param cents
     *            Sum of amounts in cents.
     * @param locale
     *            {@link Locale} used to format sum of amounts.
     */
    public TemplateAmountTotalDto(final Long n, final Long cents,
            final Locale locale) {

        try {
            this.count = BigDecimalUtil.localize(BigDecimal.valueOf(n), 0,
                    locale, true);

            this.sum = BigDecimalUtil.localize(
                    BigDecimal.valueOf(
                            (double) cents.longValue() / CENTS_IN_AMOUNT_UNIT),
                    NUMBER_OF_DECIMALS, locale, true);
        } catch (ParseException e) {
            throw new SpException(e);
        }
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

}
