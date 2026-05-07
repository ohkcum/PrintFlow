/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.system;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.system.SystemInfo.Command;

/**
 * Cached DNSSD service info.
 *
 * @author Rijk Ravestein
 *
 */
public final class DnssdServiceCache implements IUtility {

    /**
     * Browse for all service types registered on the LAN.
     */
    private static final String AVAHI_BROWSE_OPT_ALL = "-a";

    /**
     * Automatically resolve services found.
     */
    private static final String AVAHI_BROWSE_OPT_RESOLVE = "-r";

    /**
     * Make output easily parsable for usage in scripts. Fields are separated by
     * semicolons (;), service names are escaped. It is recommended to combine
     * this with --no-db-lookup.
     */
    private static final String AVAHI_BROWSE_OPT_PARSABLE = "-p";

    /** */
    private static final char AVAHI_BROWSE_OPT_PARSABLE_SEPARATOR = ';';

    /**
     * Don't lookup services types in service type database.
     */
    private static final String AVAHI_BROWSE_OPT_NO_DB_LOOKUP = "-k";

    /**
     * Terminate after dumping a more or less complete list.
     */
    private static final String AVAHI_BROWSE_OPT_TERMINATE = "-t";

    /**
     * Ignore case distinctions in patterns and input data, so that characters
     * that differ only in case match each other.
     */
    private static final String GREP_OPT_IGNORE_CASE = "-i";

    /**
     * Example: {@code "UUID=1c852a4d-b800-1f08-abcd-3863bbd6c2c3"}.
     */
    private static final String AVAHI_BROWSE_STDOUT_UUID = "UUID";

    /** */
    private static final int AVAHI_BROWSE_COL_SERVICE_NAME = 3;
    /** */
    private static final int AVAHI_BROWSE_COL_HOSTNAME = 6;
    /** */
    private static final int AVAHI_BROWSE_COL_IP_ADDRESS = 7;
    /** */
    private static final int AVAHI_BROWSE_COL_PROPS = 9;

    /** */
    private static final Object CACHE_MUTEX = new Object();

    /**
     * IP address by DNSSD service name.
     */
    private static Map<String, InetAddress> cacheIpByServiceName = null;

    /**
     * IP address by dnssd UUID.
     */
    private static Map<UUID, InetAddress> cacheIpByDnssdUUID = null;

    /**
     * Utility class.
     */
    private DnssdServiceCache() {
    }

    /**
     * Clears the cache so it is ready for lazy initialization.
     */
    public static void clear() {
        synchronized (CACHE_MUTEX) {
            cacheIpByServiceName = null;
            cacheIpByDnssdUUID = null;
        }
    }

    /**
     * @param deviceUri
     * @return {@code null} if not found.
     */
    public static InetAddress getInetAddress(final URI deviceUri) {
        synchronized (CACHE_MUTEX) {
            final String auth = deviceUri.getAuthority();
            for (final Entry<String, InetAddress> entry : getAddrByName()
                    .entrySet()) {
                if (auth.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    /**
     * Resolves UUID from column of dnssd row.
     *
     * @param column
     *            column
     * @return {@code null} if not found.
     */
    private static UUID resolveUUID(final String column) {

        final int idx = column.indexOf(AVAHI_BROWSE_STDOUT_UUID + "=");
        final UUID uuid;

        if (idx < 0) {
            uuid = null;
        } else {
            final String sub = column.substring(idx + 1);
            final String u =
                    StringUtils.split(StringUtils.split(sub, '\"')[0], '=')[1];
            uuid = UUID.fromString(u);
        }
        return uuid;
    }

    /**
     * Encodes a service name in URL format.
     *
     * @param plain
     * @return URl encoded service name.
     */
    private static String encodeAsURL(final String plain) {
        return URLEncoder.encode(plain, Charset.defaultCharset()).replace("+",
                "%20");
    }

    /**
     * Resolves service name from column of dnssd row to a string.
     *
     * @param column
     *            column. Example:
     *            {@code HP\032ENVY\0327640\032series\032\091D6C2C3\093 }
     * @return Example: {@code HP ENVY 7640 series [D6C2C3]}
     */
    private static String resolveServiceName(final String column) {
        /*
         * Make column parsable.
         */
        final String nameParsable = StringUtils
                .replace(StringUtils.replace(column, "\032", " "), "\0", "\\0");

        final StringBuilder conv = new StringBuilder();

        final int escapeLen = 3;
        for (final String part : StringUtils.split(nameParsable, '\\')) {
            if (part.length() < escapeLen) {
                conv.append(part);
            } else {
                final String pfx = part.substring(0, escapeLen);
                if (StringUtils.isNumeric(pfx)) {
                    conv.append((char) Integer.parseInt(pfx));
                    conv.append(part.substring(escapeLen));
                } else {
                    conv.append(part);
                }
            }
        }

        return conv.toString();
    }

    /**
     * @return IP address by DNSSD service name.
     */
    private static Map<String, InetAddress> getAddrByName() {
        if (cacheIpByServiceName == null) {
            createCache();
        }
        return cacheIpByServiceName;
    }

    /**
     * @return IP address by UUID.
     */
    private static Map<UUID, InetAddress> getAddrByUUID() {
        if (cacheIpByDnssdUUID == null) {
            createCache();
        }
        return cacheIpByDnssdUUID;
    }

    /**
     * Stores IP address and UUID of each DNSSD service (printer) into cache.
     */
    private static void createCache() {

        cacheIpByServiceName = new HashMap<>();
        cacheIpByDnssdUUID = new HashMap<>();

        if (!SystemInfo.isAvahiBrowseInstalled()) {
            return;
        }

        final String cmd = Command.AVAHI_BROWSE.cmdLineExt(AVAHI_BROWSE_OPT_ALL,
                AVAHI_BROWSE_OPT_RESOLVE, AVAHI_BROWSE_OPT_PARSABLE,
                AVAHI_BROWSE_OPT_NO_DB_LOOKUP, AVAHI_BROWSE_OPT_TERMINATE, "|",
                Command.GREP.cmd(), GREP_OPT_IGNORE_CASE,
                AVAHI_BROWSE_STDOUT_UUID);

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {

            if (exec.executeCommand() == 0) {

                try (Scanner scanner = new Scanner(exec.getStandardOutput())) {

                    while (scanner.hasNextLine()) {

                        final String line = scanner.nextLine();

                        final String[] columns = StringUtils.split(line,
                                AVAHI_BROWSE_OPT_PARSABLE_SEPARATOR);

                        final InetAddress hostAddr = InetAddress
                                .getByName(columns[AVAHI_BROWSE_COL_HOSTNAME]);

                        cacheIpByServiceName.put(
                                resolveServiceName(
                                        columns[AVAHI_BROWSE_COL_SERVICE_NAME]),
                                hostAddr);

                        final UUID uuid =
                                resolveUUID(columns[AVAHI_BROWSE_COL_PROPS]);
                        if (uuid != null) {
                            cacheIpByDnssdUUID.put(uuid, hostAddr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new SpException(e);
        }

        SpInfo.instance()
                .log(String.format("DNSSD cache filled with [%d] UUIDs.",
                        cacheIpByDnssdUUID.size()));
    }

    /**
     *
     */
    public static void test() {

        System.out.println("...");

        synchronized (CACHE_MUTEX) {

            for (Entry<String, InetAddress> entry : getAddrByName()
                    .entrySet()) {
                System.out.printf("%s | %s | %s | %s\n", entry.getKey(),
                        encodeAsURL(entry.getKey()),
                        entry.getValue().getHostName(),
                        entry.getValue().getHostAddress());
            }

            for (Entry<UUID, InetAddress> entry : getAddrByUUID().entrySet()) {
                System.out.printf("%s | %s | %s\n", entry.getKey(),
                        entry.getValue().getHostName(),
                        entry.getValue().getHostAddress());
            }
        }
    }
}
