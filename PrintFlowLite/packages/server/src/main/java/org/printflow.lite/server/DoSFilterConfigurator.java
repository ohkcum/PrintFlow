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
import java.util.StringTokenizer;

import org.eclipse.jetty.servlets.DoSFilter;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.config.ConfigException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.util.CidrChecker;
import org.printflow.lite.core.util.InetUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DoSFilterConfigurator extends AbstractFilterConfigurator {

    /** Utility class. */
    private DoSFilterConfigurator() {
    }

    /**
     * Validates a comma separated white list.
     *
     * @param whiteList
     * @throws ConfigException
     *             if list is has invalid syntax.
     */
    public static void validateWhiteList(final String whiteList)
            throws ConfigException {

        final StringTokenizer st = getIPAdressesTokenizer(whiteList);

        while (st.hasMoreTokens()) {

            final String tokenPlain = st.nextToken();

            if (CidrChecker.hasCidrFormat(tokenPlain)) {

                final CidrChecker chk = getCidrChecker(tokenPlain);

                if (chk == null) {
                    throw new ConfigException(
                            String.format("\"%s\" is invalid.", tokenPlain));
                } else if (chk.isIPv6()) {
                    throw new ConfigException(String.format(
                            "\"%s\" : IPv6 CIDRs are not supported.",
                            tokenPlain));
                }

            } else {

                try {
                    final String hostAddr =
                            InetAddress.getByName(stripIpv6Brackets(tokenPlain))
                                    .getHostAddress();

                    if (tokenPlain.equals(InetUtils.LOCAL_HOST)
                            || hostAddr.equals(InetUtils.IPV4_LOOP_BACK_ADDR)
                            || hostAddr.equals(
                                    InetUtils.IPV6_LOOP_BACK_ADDR_FULL)) {

                        throw new ConfigException(String.format(
                                "\"%s\" is obsolete (implicitly applied).",
                                tokenPlain));
                    }
                } catch (UnknownHostException e) {
                    throw new ConfigException(
                            String.format("\"%s\" is invalid.", tokenPlain));
                }

            }
        }
    }

    /**
     * @param whiteList
     * @return formatted for filtering.
     * @throws ConfigException
     *             invalid value.
     */
    public static String formatWhiteList(final String whiteList)
            throws ConfigException {

        final StringBuilder list = new StringBuilder();
        final StringTokenizer st = getIPAdressesTokenizer(whiteList);

        while (st.hasMoreTokens()) {
            if (list.length() > 0) {
                list.append(",");
            }
            final String tokenPlain = st.nextToken();

            if (CidrChecker.hasCidrFormat(tokenPlain)) {
                list.append(tokenPlain);
            } else {
                final String tokenStripped = stripIpv6Brackets(tokenPlain);

                try {
                    if (InetUtils.isIPv6Address(tokenStripped)) {
                        list.append(ipv6InBrackets(tokenStripped));
                    } else {
                        list.append(tokenStripped);
                    }
                } catch (final UnknownHostException e) {
                    throw new ConfigException(e.getMessage());
                }
            }
        }
        return list.toString();
    }

    /**
     * Configures {@link DoSFilter} using ALL relevant {@link IConfigProp.Key}
     * values.
     *
     * @param doSFilter
     *            Filter to configure.
     * @throws ConfigException
     *             invalid value encountered.
     */
    public static void configureAll(final DoSFilter doSFilter)
            throws ConfigException {

        for (IConfigProp.Key key : ConfigManager.getDoSFilterKeysConfig()) {
            configure(doSFilter, key);
        }
        // ------------------------------------------------------
        // Confirm Jetty defaults (for documentation purposes).
        // ------------------------------------------------------
        // Insert the DoSFilter headers into the response.
        doSFilter.setInsertHeaders(true);
        // If true then rate is tracked by IP+port (effectively connection).
        doSFilter.setRemotePort(false);
    }

    /**
     * Configures {@link DoSFilter} using {@link IConfigProp.Key} value.
     *
     * @param doSFilter
     *            Filter to configure.
     * @param configKey
     *            Key to configure.
     * @throws ConfigException
     *             invalid value.
     */
    public static void configure(final DoSFilter doSFilter,
            final IConfigProp.Key configKey) throws ConfigException {

        final ConfigManager cm = ConfigManager.instance();
        final SpInfo logger = SpInfo.instance();
        final String logPfx = " DoSFilter";

        switch (configKey) {

        case SYS_DOSFILTER_ENABLE:

            final boolean isEnabled = cm.isConfigValue(configKey);
            logger.log(String.format("%s - %s", logPfx,
                    isEnabled ? "\u2705 Enabled" : "\u274C Disabled"));
            doSFilter.setEnabled(isEnabled);
            break;

        case SYS_DOSFILTER_DELAY_MSEC:
            doSFilter.setDelayMs(cm.getConfigLong(configKey));
            logger.log(String.format("%s - DelayMs [%d]", logPfx,
                    doSFilter.getDelayMs()));
            break;

        case SYS_DOSFILTER_THROTTLE_MSEC:
            doSFilter.setThrottleMs(cm.getConfigLong(configKey));
            logger.log(String.format("%s - ThrottleMs [%d]", logPfx,
                    doSFilter.getThrottleMs()));
            break;

        case SYS_DOSFILTER_THROTTLED_REQUESTS:
            doSFilter.setThrottledRequests(cm.getConfigInt(configKey));
            logger.log(String.format("%s - ThrottledRequests [%d]", logPfx,
                    doSFilter.getThrottledRequests()));
            break;

        case SYS_DOSFILTER_TOO_MANY_CODE:
            doSFilter.setTooManyCode(cm.getConfigInt(configKey));
            logger.log(String.format("%s - TooManyCode [%d]", logPfx,
                    doSFilter.getTooManyCode()));
            break;

        case SYS_DOSFILTER_WHITELIST:

            final String whitelist =
                    formatWhiteList(cm.getConfigValue(configKey));

            validateWhiteList(whitelist); // throws ConfigException.

            doSFilter.clearWhitelist();

            if (!whitelist.isBlank()) {
                doSFilter.setWhitelist(whitelist);
            }
            /*
             * Standard whitelisted.
             */
            doSFilter.addWhitelistAddress(InetUtils.LOCAL_HOST);
            doSFilter.addWhitelistAddress(InetUtils.IPV4_LOOP_BACK_ADDR);
            /*
             * Since Jetty uses [ ] bracket style for incoming IPv6 addresses,
             * we use [] brackets as well, so Jetty can make the match.
             */
            doSFilter.addWhitelistAddress(
                    ipv6InBrackets(InetUtils.IPV6_LOOP_BACK_ADDR_FULL));

            logger.log(String.format("%s - Whitelist: %s", logPfx,
                    doSFilter.getWhitelist()));
            break;

        case SYS_DOSFILTER_MAX_IDLE_TRACKER_MSEC:
            doSFilter.setMaxIdleTrackerMs(cm.getConfigLong(configKey));
            logger.log(String.format("%s - MaxIdleTrackerMs [%d]", logPfx,
                    doSFilter.getMaxIdleTrackerMs()));
            break;

        case SYS_DOSFILTER_MAX_REQUEST_MSEC:
            doSFilter.setMaxRequestMs(cm.getConfigLong(configKey));
            logger.log(String.format("%s - MaxRequestMs [%d]", logPfx,
                    doSFilter.getMaxRequestMs()));
            break;

        case SYS_DOSFILTER_MAX_REQUESTS_PER_SEC:
            doSFilter.setMaxRequestsPerSec(cm.getConfigInt(configKey));
            logger.log(String.format("%s - MaxRequestsPerSec [%d]", logPfx,
                    doSFilter.getMaxRequestsPerSec()));
            break;

        default:
            throw new SpException(configKey.toString() + " NOT handled.");
        }
    }

}
