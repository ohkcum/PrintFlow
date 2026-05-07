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
package org.printflow.lite.core.util;

import java.util.Base64;
import java.util.StringTokenizer;

import org.printflow.lite.common.IUtility;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class HttpAuthenticationUtil implements IUtility {

    /** */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    /** */
    public static final String SCHEME_BASIC = "Basic";

    /**
     * The HTTP WWW-Authenticate response header advertises the HTTP
     * authentication methods (or challenges) that might be used to gain access
     * to a specific resource.
     */
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    /**
     * Realm used to describe the protected area or to indicate the scope of
     * protection. This could be a message like "Access to the staging site" or
     * similar, so that the user knows to which space they are trying to get
     * access to.
     */
    public static final String AUTHENTICATION_REALM = "realm";

    /**
     * Utility class.
     */
    private HttpAuthenticationUtil() {
    }

    /**
     * Gets userid and password from Basic Authorization.
     *
     * @param authorization
     *            Header with format "Basic base64(user:password)"
     * @return Array with userid (index 0) and password (index 1).
     */
    public static String[]
            getBasicAuthUserPassword(final String authorization) {

        final String[] array = new String[2];

        // Get encoded username and password
        final String encodedUserPassword =
                authorization.replaceFirst(SCHEME_BASIC + " ", "");

        // Decode username and password
        final String usernameAndPassword = new String(
                Base64.getDecoder().decode(encodedUserPassword.getBytes()));

        // Split username and password tokens
        final StringTokenizer tokenizer =
                new StringTokenizer(usernameAndPassword, ":");

        array[0] = tokenizer.nextToken();
        array[1] = tokenizer.nextToken();

        return array;
    }

    /**
     * Format type and realm, to be used in WWW-Authenticate header.
     *
     * @param type
     *            authentication type
     * @param realm
     *            Realm used to describe the protected area or to indicate the
     *            scope of protection.
     * @return input formatted as {@code <type> realm=<realm>}
     */
    public static String wwwAuthTypeRealm(final String type,
            final String realm) {

        return String.format("%s %s=\"%s\"", type,
                HttpAuthenticationUtil.AUTHENTICATION_REALM, realm);
    }

}
