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
package org.printflow.lite.core.util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.system.DnssdServiceCache;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsPrinterUriHelper implements IUtility {

    /** */
    private static final String SCHEME_HP = "hp";

    /** */
    private static final String SCHEME_DNSSD = "dnssd";

    /** */
    private static final String HP_QUERY_PARM_IP = "ip";

    /** */
    private static final String HP_QUERY_PARM_HOSTNAME = "hostname";

    /** */
    private static final String SCHEME_SOCKET = "socket";

    /** Utility class. */
    private CupsPrinterUriHelper() {
    }

    /**
     *
     * @param uri
     *            The URI.
     * @return The query key/values.
     * @throws UnsupportedEncodingException
     *             When URL decode error.
     */
    private static Map<String, String> splitQuery(final URI uri)
            throws UnsupportedEncodingException {

        final Map<String, String> queryPairs = new HashMap<>();

        final String query = uri.getQuery();
        final String[] pairs = query.split("&");

        for (final String pair : pairs) {
            final int idx = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return queryPairs;
    }

    /**
     * Gets the host part of the CUPS printer device URI.
     *
     * @param deviceUri
     *            The CUPS printer device URI.
     * @return The host part of the URI, or {@code null} when inapplicable.
     */
    public static String resolveHost(final URI deviceUri) {

        final String scheme = deviceUri.getScheme();

        if (scheme == null) {
            return null;
        }

        final String schemeLower = scheme.toLowerCase();

        if (schemeLower.equals(SCHEME_HP)) {

            try {
                String host = splitQuery(deviceUri).get(HP_QUERY_PARM_IP);
                if (host == null) {
                    host = splitQuery(deviceUri).get(HP_QUERY_PARM_HOSTNAME);
                }
                return host;
            } catch (UnsupportedEncodingException e) {
                return null;
            }

        } else if (schemeLower.equals(SCHEME_DNSSD)) {
            final InetAddress addr =
                    DnssdServiceCache.getInetAddress(deviceUri);
            if (addr != null) {
                return addr.getHostName();
            }
            return null;

        } else if (schemeLower.equals(SCHEME_SOCKET)) {
            return deviceUri.getHost();

        } else if (schemeLower.equals(InetUtils.URL_PROTOCOL_IPP)
                || schemeLower.equals(InetUtils.URL_PROTOCOL_IPPS)) {
            String host = deviceUri.getHost();
            if (host == null) {
                final InetAddress addr =
                        DnssdServiceCache.getInetAddress(deviceUri);
                if (addr != null) {
                    host = addr.getHostName();
                }
            }
            return host;
        }

        if (deviceUri.getRawSchemeSpecificPart() != null) {
            try {
                // Recurse.
                return resolveHost(
                        new URI(deviceUri.getRawSchemeSpecificPart()));
            } catch (URISyntaxException e) {
                return null;
            }
        }

        return null;
    }

}
