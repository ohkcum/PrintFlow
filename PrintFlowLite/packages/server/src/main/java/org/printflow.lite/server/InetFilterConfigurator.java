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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public final class InetFilterConfigurator extends AbstractFilterConfigurator {

    /** Utility class. */
    private InetFilterConfigurator() {
    }

    /**
     * Configures using {@link IConfigProp.Key} value.
     *
     * @param filter
     *            Filter to configure.
     * @throws ConfigException
     *             invalid value.
     */
    public static void configureAll(final InetAccessFilter filter)
            throws ConfigException {

        for (IConfigProp.Key key : ConfigManager.getInetFilterKeysConfig()) {
            configure(filter, key);
        }
    }

    /**
     * @param logger
     * @param logPfx
     * @param property
     * @param cidrLst
     * @param addrSet
     */
    private static void logConfigProp(final SpInfo logger, final String logPfx,
            final String property, final List<CidrChecker> cidrLst,
            final Set<String> addrSet) {

        // Echo interpreted values.
        final StringBuilder list = new StringBuilder();
        for (final String entry : addrSet) {
            list.append(list.length() > 0 ? ", " : "").append(entry);
        }
        for (final CidrChecker entry : cidrLst) {
            list.append(list.length() > 0 ? ", " : "").append(entry);
        }
        logger.log(String.format("%s - %s: %s", logPfx, property,
                list.toString()));
    }

    /**
     * Configures using {@link IConfigProp.Key} value.
     *
     * @param filter
     *            Filter to configure.
     * @param configKey
     *            Key to configure.
     * @throws ConfigException
     *             invalid value.
     */
    public static void configure(final InetAccessFilter filter,
            final IConfigProp.Key configKey) throws ConfigException {

        final ConfigManager cm = ConfigManager.instance();
        final SpInfo logger = SpInfo.instance();

        final String logPfx = "InetFilter";

        List<CidrChecker> cidrLst;
        Set<String> addrSet;

        switch (configKey) {

        case SYS_INETFILTER_ENABLE:
            filter.setEnabled(cm.isConfigValue(configKey));
            logger.log(String.format("%s - %s", logPfx,
                    filter.isEnabled() ? "\u2705 Enabled" : "\u274C Disabled"));
            break;

        case SYS_INETFILTER_BLACKLIST:
            cidrLst = new ArrayList<>();
            addrSet = new HashSet<>();

            AbstractFilterConfigurator.parseIPAddresses(
                    cm.getConfigValue(configKey), cidrLst, addrSet);

            if (addrSet.contains(InetUtils.IPV4_LOOP_BACK_ADDR)
                    || addrSet.contains(InetUtils.IPV6_LOOP_BACK_ADDR_FULL)) {
                throw new ConfigException(
                        "loop-back addresses can not be part of the list.");
            }

            filter.setBlackListAddr(addrSet);
            filter.setBlackListCidr(cidrLst);

            // Echo interpreted values.
            logConfigProp(logger, logPfx, "Blacklist", cidrLst, addrSet);

            break;

        case SYS_INETFILTER_WHITELIST:
            cidrLst = new ArrayList<>();
            addrSet = new HashSet<>();

            AbstractFilterConfigurator.parseIPAddresses(
                    cm.getConfigValue(configKey), cidrLst, addrSet);

            if (addrSet.contains(InetUtils.IPV4_LOOP_BACK_ADDR)
                    || addrSet.contains(InetUtils.IPV6_LOOP_BACK_ADDR_FULL)) {
                throw new ConfigException("loop-back addresses are obsolete, "
                        + "because implicitly applied.");
            }

            filter.setWhiteListAddr(addrSet);
            filter.setWhiteListCidr(cidrLst);

            // Echo interpreted values.
            logConfigProp(logger, logPfx, "Whitelist", cidrLst, addrSet);

            break;

        case SYS_INETFILTER_WHITELIST_EMPTY_ALLOW_ALL:
            filter.setWhitelistEmptyAllowAll(cm.isConfigValue(configKey));
            logger.log(String.format("%s - Whitelist: if empty, allow %s",
                    logPfx, filter.isWhitelistEmptyAllowAll() ? "all."
                            : "loop-back only."));
            break;

        case SYS_INETFILTER_WARN_INTERVAL_APPLOG_MINS:
            filter.setMsgIntervalApplogMins(
                    ConfigManager.instance().getConfigInt(configKey));
            break;

        case SYS_INETFILTER_WARN_INTERVAL_WEBAPP_SECS:
            filter.setMsgIntervalRealtimeSecs(
                    ConfigManager.instance().getConfigInt(configKey));
            break;

        default:
            throw new SpException(configKey.toString() + " NOT handled.");
        }
    }

}
