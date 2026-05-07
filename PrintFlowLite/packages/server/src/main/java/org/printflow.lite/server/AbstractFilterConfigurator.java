/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.config.ConfigException;
import org.printflow.lite.core.util.CidrChecker;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractFilterConfigurator {

    /**
     * Parses address list.
     *
     * @param addrList
     *            Command-separated list if IP addresses and/or CIDRs.
     * @param cidr
     * @param addr
     * @throws ConfigException
     *             if list
     */
    protected static void parseIPAddresses(final String addrList,
            final List<CidrChecker> cidr, final Set<String> addr)
            throws ConfigException {

        final StringTokenizer st = getIPAdressesTokenizer(addrList);

        while (st.hasMoreTokens()) {
            final String tokenPlain = st.nextToken();

            if (CidrChecker.hasCidrFormat(tokenPlain)) {
                final CidrChecker chk = getCidrChecker(tokenPlain);
                if (chk == null) {
                    throw new ConfigException(
                            String.format("%s - invalid", tokenPlain));
                }
                cidr.add(chk);
            } else {
                try {
                    /*
                     * Use getHostAddress() to get canonical (full IPv6)
                     * address.
                     */
                    addr.add(
                            InetAddress.getByName(stripIpv6Brackets(tokenPlain))
                                    .getHostAddress());
                } catch (final UnknownHostException e) {
                    throw new ConfigException(e.getMessage());
                }
            }
        }
    }

    /**
     * Creates a CIDR checker.
     *
     * @param cidr
     * @return {@code null} if cidr is invalid.
     */
    protected static CidrChecker getCidrChecker(final String cidr) {
        CidrChecker chk = null;
        try {
            chk = new CidrChecker(cidr);
        } catch (Exception e) {
            // no code intended
        }
        return chk;
    }

    /**
     * @param list
     *            Comma-separated list op IP addresses/CIDRs.
     * @return IP addresses/CIDRs tokens.
     */
    protected static StringTokenizer getIPAdressesTokenizer(final String list) {

        return new StringTokenizer(StringUtils.replace(list, " ", ""), ",");
    }

    /**
     * @param ipv6Addr
     *            Plain IPv6 address.
     * @return address within brackets.
     */
    protected static String ipv6InBrackets(final String ipv6Addr) {
        return "[" + ipv6Addr + "]";
    }

    /**
     * @param ipv6Addr
     *            IPv6 address.
     * @return address stripped from brackets.
     */
    protected static String stripIpv6Brackets(final String ipv6Addr) {
        return StringUtils.stripEnd(StringUtils.stripStart(ipv6Addr, "["), "]");
    }

}
