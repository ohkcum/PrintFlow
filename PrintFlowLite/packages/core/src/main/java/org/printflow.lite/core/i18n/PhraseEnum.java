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
 * Common phrases.
 *
 * @author Rijk Ravestein
 *
 */
public enum PhraseEnum {

    /** */
    ACTION_CANNOT_BE_UNDONE,

    /** */
    ACTIVATE_CARD_READER,

    /** */
    AMOUNT_EXCEEDS_CREDIT(true),

    /** */
    ANOTHER_BROWSER_SESSION_ACTIVE,

    /** */
    CERT_EXPIRED_ON(true),
    /** */
    CERT_EXPIRES_ON(true),
    /** */
    CERT_VALID_UNTIL(true),

    /** */
    MAIL_SENT,

    /** */
    MAIL_CHECK_FOR_FURTHER_INSTRUCTIONS,

    /** */
    NOT_FOUND,

    /** */
    FIND_TICKET_BY_ID,

    /** */
    FIND_USER_BY_ID,

    /** */
    PASSWORD_MISMATCH,

    /** */
    PDF_INVALID,
    /** */
    PDF_ENCRYPTED_UNSUPPORTED,

    /** */
    PDF_FONTS_NONE,
    /** */
    PDF_FONTS_ALL_EMBEDDED,
    /** */
    PDF_FONTS_ALL_NON_EMBEDDED,
    /** */
    PDF_FONTS_ALL_STANDARD,
    /** */
    PDF_FONTS_SOME_NON_EMBEDDED,
    /** */
    PDF_FONTS_SOME_NON_STANDARD_OR_EMBEDDED,
    /** */
    PDF_FONTS_SOME_NON_STANDARD_OR_EMBEDDED_SHORT,
    /** */
    PDF_FONTS_STANDARD_OR_EMBEDDED,
    /** */
    PDF_FONTS_STANDARD_OR_EMBEDDED_SHORT,
    /** */
    PDF_PASSWORD_UNSUPPORTED,
    /** */
    PDF_PRINTING_NOT_ALLOWED,
    /** */
    PDF_REPAIR_FAILED,
    /** */
    PDF_DYNAMIC_XFA_UNSUPPORTED,

    /** */
    PLEASE_WAIT,

    /** Question. */
    Q_DELETE_DOCUMENT,

    /** Question. */
    Q_REPLACE_SECRET_CODE,

    /** */
    REALTIME_ACTIVITY,
    /** */
    SELECT_AND_SORT,
    /** */
    SWIPE_CARD,
    /** */
    SYS_MAINTENANCE,
    /** */
    SYS_TEMP_UNAVAILABLE,
    /** */
    USER_DELETE_WARNING,
    /** */
    WINDOWS_DRIVER_MSG,

    /** */
    DO_NOT_CHANGE,
    /** */
    KEEP_CURRENT_VALUE,
    /** */
    LEAVE_UNCHANGED,
    /** */
    LOGIN_TO_CONTINUE;

    /**
     * {@code true} if message needs arguments.
     */
    private final boolean hasArguments;

    PhraseEnum() {
        this.hasArguments = false;
    }

    PhraseEnum(final boolean arg) {
        this.hasArguments = arg;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        if (this.hasArguments) {
            throw new IllegalArgumentException(
                    String.format("%s needs arguments.", this.toString()));
        }
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * Get the localized UI text with argument. To be used for:
     * {@link #CERT_EXPIRED_ON}, {@link #CERT_EXPIRES_ON},
     * {@link #CERT_VALID_UNTIL}.
     *
     * @param locale
     *            The {@link Locale}.
     * @param args
     *            The arguments.
     * @return The localized text.
     */
    public String uiText(final Locale locale, final String... args) {
        if (this.hasArguments) {
            return LocaleHelper.uiTextArgs(this, locale, args);
        }
        throw new IllegalArgumentException(String
                .format("%s does not support arguments.", this.toString()));
    }

}
