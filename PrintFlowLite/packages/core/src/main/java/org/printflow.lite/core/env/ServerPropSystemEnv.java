/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2024 Datraverse B.V.
 * Authors: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2024 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.env;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.config.ServerPropEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unmodifiable view of the system environment with server properties.
 *
 * @author Rijk Ravestein
 *
 */
public final class ServerPropSystemEnv implements IUtility {

    /** Utility class. */
    private ServerPropSystemEnv() {
    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerPropSystemEnv.class);

    /** */
    private static final Map<String, ServerPropEnum> SERVER_PROP_ENUM_BY_KEY =
            new HashMap<>();

    static {
        init();
    }

    /**
     * Name of environment variable that holds the namespace. Its value is used
     * as prefix for all PrintFlowLite environment variables.
     * <p>
     * <b>Caution</b>: this value is used in shell script(s).
     * </p>
     */
    public static final String NAMESPACE = "PRINTFLOWLITE_NS";

    /**
     * Environment variable for container type.
     * <p>
     * <b>Caution</b>: this value is used in shell script(s).
     * </p>
     */
    public static final String V_CONTAINER = "CONTAINER";

    /**
     * Docker container.
     * <p>
     * <b>Caution</b>: this value is used in shell script(s).
     * </p>
     */
    public static final String CONTAINER_DOCKER = "DOCKER";

    /**
     * Prefix for system environment variable that wraps a server property
     * key/value pair as a single value separated by
     * {@link #VAR_KEY_VALUE_SPLIT}.
     */
    public static final String V_PFX_VAR = "SRV_";

    /**
     * Character that splits key/value.
     */
    public static final char VAR_KEY_VALUE_SPLIT = ':';

    /**
     * System environment.
     */
    private static Map<String, String> environment;

    /**
     * Prefix of PrintFlowLite environment variables.
     */
    private static String envVarPrefix;

    /**
     * Initialize static variables.
     */
    private static void init() {

        envVarPrefix = System.getenv(NAMESPACE);

        if (envVarPrefix == null) {
            return;
        }

        for (final ServerPropEnum val : ServerPropEnum.values()) {
            SERVER_PROP_ENUM_BY_KEY.put(val.key(), val);
        }

        final String envVarContainer = envVarPrefix.concat(V_CONTAINER);
        final String envVarPrefixVar = envVarPrefix.concat(V_PFX_VAR);

        environment = new TreeMap<>(); // ordered by key

        for (Entry<String, String> entry : System.getenv().entrySet()) {

            final String envKey = entry.getKey();
            final String envValue = entry.getValue();

            if (envKey.startsWith(envVarPrefixVar)) {

                final int iSplit = envValue.indexOf(VAR_KEY_VALUE_SPLIT);

                if (iSplit >= 0) {

                    final String keyPart =
                            StringUtils.strip(envValue.substring(0, iSplit));

                    if (SERVER_PROP_ENUM_BY_KEY.containsKey(keyPart)) {

                        final String valuePart = StringUtils
                                .strip(envValue.substring(iSplit + 1));

                        environment.put(keyPart, valuePart);

                    } else {
                        LOGGER.warn("[{}] [{}] key not found.", envKey,
                                keyPart);
                    }

                } else {
                    LOGGER.warn("[{}] [{}]} syntax error.", envKey, envValue);
                }

            } else if (envKey.equals(envVarContainer)) {

                if (!envValue.equals(CONTAINER_DOCKER)) {
                    LOGGER.warn("[{}] [{}] unknown, use [{}].", envKey,
                            envValue, CONTAINER_DOCKER);
                }

            } else if (envKey.startsWith(envVarPrefix)) {
                LOGGER.warn("[{}] syntax error: use [{}] prefix.", envKey,
                        envVarPrefixVar);
            }
        }

    }

    /**
     * @return PrintFlowLite environment or {@code null} if not present.
     */
    public static Map<String, String> getEnv() {
        return environment;
    }

    /**
     * @return {@code true} if PrintFlowLite environment is present.
     */
    public static boolean isPresent() {
        return environment != null && environment.size() > 0;
    }

    /**
     * @return {@code true} if PrintFlowLite runs in a Docker container.
     */
    public static boolean isDockerized() {
        return isPresent() && StringUtils
                .defaultString(System.getenv(envVarPrefix + V_CONTAINER))
                .equals(CONTAINER_DOCKER);
    }

    /**
     * Fills (overwrites) server properties from the system environment using
     * {@link ServerPropEnum} keys.
     *
     * @param props
     *            Server properties to be filled.
     * @param enumValues
     *            Array of {@link ServerPropEnum} values.
     */
    public static void fillServerProperties(final Properties props,
            final ServerPropEnum[] enumValues) {

        if (isPresent()) {
            for (final ServerPropEnum val : enumValues) {
                final String key = val.key();
                if (environment.containsKey(key)) {
                    props.put(key, environment.get(key));
                }
            }
        }
    }

}
