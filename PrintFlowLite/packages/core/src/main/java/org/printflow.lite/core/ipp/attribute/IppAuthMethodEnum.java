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
package org.printflow.lite.core.ipp.attribute;

/**
 * IPP Authentication Method values.
 *
 * @author Rijk Ravestein
 *
 */
public enum IppAuthMethodEnum {

    /** */
    NONE("none"),
    /** RFC7617. */
    BASIC("basic"),
    /** RFC8446. */
    CERTIFICATE("certificate"),
    /** RFC7616. */
    DIGEST("digest"),
    /** RFC4559. */
    NEGOTIATE("negotiate"),
    /** RFC6749, RFC6750. */
    OAUTH("oauth"),
    /** */
    REQUESTING_USER_NAME("requesting-user-name");

    /** */
    private final String keyword;

    IppAuthMethodEnum(final String kw) {
        this.keyword = kw;
    }

    /**
     * @return IPP keyword.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Does this method challenge an IPP Client to prove user identity?
     *
     * @return {@code true} if client is challenged.
     */
    public boolean isClientChallenged() {
        return this != NONE && this != REQUESTING_USER_NAME;
    }
}
