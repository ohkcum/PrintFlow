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
package org.printflow.lite.core.i18n;

import java.util.Locale;

import org.printflow.lite.core.util.LocaleHelper;

/**
 * Common UI Nouns.
 *
 * @author Rijk Ravestein
 *
 */
public enum NounEnum {

    /** */
    ACTIVITY(true),
    /** */
    ADDRESS(true),
    /**  */
    ADMINISTRATOR(true),
    /**  */
    ALIAS(true),
    /** */
    ARCHIVE,
    /** */
    AUTHENTICATION,
    /** */
    AUTHOR,
    /** */
    BACKGROUND,
    /** Financial position. */
    BALANCE,
    /** */
    BORDER,
    /** */
    BRUSH,
    /** NFC card number. */
    CARD_NUMBER,
    /** */
    CIRCLE(true),
    /** */
    CLIENT(true),
    /** */
    COMMENT,
    /** */
    CONFIRMATION,
    /** */
    COST,
    /** */
    CREDIT_LIMIT,
    /** */
    CURRENCY,
    /** */
    DATABASE,
    /** */
    DATE(true),
    /** */
    DAY(true), WEEK(true), MONTH(true), YEAR(true),
    /** */
    DELEGATE(true),
    /** */
    DELEGATOR(true),
    /** */
    DESTINATION,
    /** */
    DEVICE(true),
    /** */
    DISK_SPACE,
    /** */
    DOCUMENT(true),
    /** */
    DOWNLOAD(true),
    /** */
    DRAWING(true),
    /** */
    EMAIL,
    /** */
    ENCRYPTION,
    /** */
    ENVIRONMENT,
    /** */
    ERROR(true),
    /** */
    EXPIRATION,
    /** */
    FILE(true),
    /** */
    FILL,
    /** */
    FONT(true),
    /** */
    FOREGROUND,
    /** */
    GROUP(true),
    /** */
    HOUR(true),
    /** */
    ID,
    /** */
    ID_NUMBER,
    /** */
    INPUT,
    /** */
    INVOICE,
    /** */
    INVOICING,
    /** */
    ITEM(true),
    /** */
    JOURNAL,
    /** */
    KEY(true),
    /** */
    KEYWORD(true),
    /** */
    LAYOUT,
    /** */
    LETTERHEAD(true),
    /** */
    LEVEL(true),
    /** As in "Rate Limiter". */
    LIMITER(true),
    /** */
    LINE(true),
    /** */
    LINK(true),
    /** */
    LOCATION,
    /** */
    MAIL,
    /** */
    MANUFACTURER(true),
    /** */
    MESSAGE(true),
    /** */
    MODE(true),
    /** As in Type/Model. */
    MODEL(true),
    /** */
    NAME(true),
    /** */
    NOTE(true),
    /** */
    OPACITY,
    /** */
    OFFICE(true),
    /** */
    OPTION(true),
    /** */
    ORIGIN,
    /** */
    ORIGINAL,
    /** */
    OUTPUT,
    /** */
    PAGE(true),
    /** */
    PASSWORD(true),
    /** */
    PAYMENT(true),
    /** */
    PERIOD(true),
    /** */
    PERSON(true),
    /** */
    POSITION,
    /** */
    PREFERRED_LIST,
    /** */
    PRINTER(true),
    /** */
    PROTOCOL(true),
    /** */
    QUEUE(true),
    /** */
    RANGE,
    /** As in "Rate Limiter". */
    RATE,
    /** */
    REASON(true),
    /** */
    RECEIPT,
    /** */
    RECTANGLE(true),
    /** */
    REFUND(true),
    /** */
    REGISTRATION(true),
    /** */
    REMARK(true),
    /** */
    RENDERING,
    /** */
    REPORT,
    /** */
    REQUEST(true),
    /** */
    ROLE(true),
    /** */
    SCOPE,
    /** */
    SERVER(true),
    /** */
    SHAPE(true),
    /** */
    SHOP(true),
    /** */
    SIZE,
    /** */
    SIGNATURE,
    /** */
    SORTING,
    /** */
    STATISTICS,
    /** */
    STATUS,
    /** */
    SUBJECT,
    /** */
    SYNCHRONIZATION,
    /** */
    TERMINAL(true),
    /** */
    TEXT(true),
    /** */
    TIME,
    /** */
    TITLE,
    /** */
    TOTAL(true),
    /** */
    TRANSACTION(true),
    /** */
    TRIANGLE(true),
    /** */
    TYPE(true),
    /** */
    VALUE(true),
    /** */
    VERIFICATION,
    /** */
    VISIBILITY,
    /** */
    WARNING(true),
    /** */
    WIDTH,
    /** */
    USER(true);

    /**
     *
     */
    private static final String PLURAL_SUFFIX = "_P";

    /**
     *
     */
    private static final String SINGULAR_SUFFIX = "_S";

    /**
     * {@code true} when noun has a plural form.
     */
    private final boolean hasPlural;

    /**
     *
     */
    NounEnum() {
        this.hasPlural = false;
    }

    /**
     *
     * @param plural
     *            {@code true} when noun has a plural form.
     */
    NounEnum(final boolean plural) {
        this.hasPlural = plural;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {

        if (this.hasPlural) {
            return LocaleHelper.uiText(this, locale, SINGULAR_SUFFIX);
        }
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @param plural
     *            {@code true} if plural form.
     * @return The localized text.
     */
    public String uiText(final Locale locale, final boolean plural) {

        if (!this.hasPlural) {
            return LocaleHelper.uiText(this, locale);
        }

        final String sfx;

        if (plural) {
            sfx = PLURAL_SUFFIX;
        } else {
            sfx = SINGULAR_SUFFIX;
        }
        return LocaleHelper.uiText(this, locale, sfx);
    }

}
