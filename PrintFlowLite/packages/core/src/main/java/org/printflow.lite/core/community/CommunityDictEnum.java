/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.community;

import java.util.Locale;

import org.printflow.lite.core.util.LocaleHelper;

/**
 * A dictionary of common words.
 *
 * @author Rijk Ravestein
 *
 */
public enum CommunityDictEnum {

    /**
     * The single word (tm) community name: DO NOT CHANGE THIS NAME.
     */
    PrintFlowLite("PrintFlowLite"),

    /**
     * A short slogan.
     */
    PRINTFLOWLITE_SLOGAN("Open Print Portal"),

    /** */
    PRINTFLOWLITE_DOT_ORG("PrintFlowLite.org"),

    /** */
    PRINTFLOWLITE_WWW_DOT_ORG("www.PrintFlowLite.org"),

    /** */
    PRINTFLOWLITE_WWW_DOT_ORG_URL("https://printflowlite.local"),

    /** */
    COMMUNITY_SOURCE_CODE_URL("https://gitlab.com/PrintFlowLite"),

    /** */
    COMMUNITY("Community"),

    /** */
    DATRAVERSE_BV("Datraverse B.V."),

    /** */
    DATRAVERSE_BV_URL("https://www.datraverse.com"),

    /**
     * URL European Union.
     */
    EU_URL("https://europa.eu/"),

    /**
     * URL of GDPR. Note: SSL Server Certificate issued to Organization (O):
     * European Commission.
     */
    EU_GDPR_URL("https://ec.europa.eu/info/law/law-topic/data-protection_en"),

    /** */
    EU_FULL_TXT("European Union"),

    /** */
    EU_GDPR_FULL_TXT("General Data Protection Regulation"),

    /** */
    PRINTFLOWLITE_SUPPORT("PrintFlowLite Support"),

    /** */
    PRINTFLOWLITE_COMMUNITY_URL("https://community.PrintFlowLite.org"),

    /** */
    PRINTFLOWLITE_SUPPORT_URL("https://support.PrintFlowLite.org"),

    /** */
    PRINTFLOWLITE_WIKI_URL("https://wiki.PrintFlowLite.org"),

    /** */
    MEMBER("Member"),

    /** */
    MEMBER_CARD("Member Card"),

    /** */
    MEMBERSHIP("Membership"),

    /** */
    VISITOR("Visitor"),

    /** */
    VISITING_GUEST("Visiting Guest"),

    /** */
    CARD_HOLDER,

    /** */
    WEB_PRINT,

    /** */
    INTERNET_PRINT,

    /** */
    RESTFUL_PRINT("RESTful Print"),

    /** */
    DELEGATED_PRINT,

    /** */
    DOC_STORE,

    /** */
    ECO_PRINT,

    /** */
    PROXY_PRINT,

    /** */
    MAIL_PRINT,

    /** */
    PARTICIPANTS,

    /** */
    USER_MANUAL("User Manual"),

    /** */
    USERS;

    /**
     * .
     */
    private final String word;

    /**
     * {@code true} when this term must be translated for internalization.
     */
    private final boolean translatable;

    /**
     * Constructor.
     *
     * @param word
     *            The unique non-translatable word for the dictionary entry.
     */
    CommunityDictEnum(final String word) {
        this.word = word;
        this.translatable = false;
    }

    /**
     * Constructor.
     *
     * @param translatable
     */
    CommunityDictEnum() {
        this.word = null;
        this.translatable = true;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String getWord(final Locale locale) {
        if (this.translatable) {
            return LocaleHelper.uiText(this, locale);
        }
        return word;
    }

    /**
     * @return The default localized text.
     */
    public String getWord() {
        return this.getWord(Locale.getDefault());
    }

}
