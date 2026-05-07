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
package org.printflow.lite.core.jpa.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.printflow.lite.core.config.ServerPropEnum;

/**
 * Hibernate connection pool parameters.
 * <p>
 * <a href="https://www.mchange.com/projects/c3p0/">c3p0 - JDBC3 Connection and
 * Statement Pooling</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum DbConnectionPoolEnum {
    /** */
    MIN_SIZE(ServerPropEnum.DB_CONNECTION_POOL_MIN,
            AvailableSettings.C3P0_MIN_SIZE),
    /** */
    MAX_SIZE(ServerPropEnum.DB_CONNECTION_POOL_MAX,
            AvailableSettings.C3P0_MAX_SIZE),
    /** */
    TIMEOUT_SECS(ServerPropEnum.DB_CONNECTION_IDLE_TIMEOUT_SECS,
            AvailableSettings.C3P0_TIMEOUT),
    /** */
    TIMEOUT_TEST_SECS(ServerPropEnum.DB_CONNECTION_IDLE_TIMEOUT_TEST_SECS,
            AvailableSettings.C3P0_IDLE_TEST_PERIOD),
    /** */
    STATEMENTS_CACHE(ServerPropEnum.DB_CONNECTION_STATEMENT_CACHE,
            AvailableSettings.C3P0_MAX_STATEMENTS);

    /** */
    private final ServerPropEnum serverProp;

    /**
     * c3p0 config key.
     */
    private final String c3p0Key;

    /**
     *
     * @param prop
     *            PrintFlowLite server property.
     * @param key
     *            The c3p0 hibernate key.
     */
    DbConnectionPoolEnum(final ServerPropEnum prop, final String key) {
        this.serverProp = prop;
        this.c3p0Key = key;
    }

    /**
     * @return server property.
     */
    public ServerPropEnum getServerProp() {
        return this.serverProp;
    }

    /**
     * @return c3p0 Hibernate key.
     */
    public String getC3p0Key() {
        return this.c3p0Key;
    }

    /**
     * Creates key value map from server properties.
     *
     * @param serverProps
     *            The server configuration.
     * @return Key value map.
     */
    public static Map<DbConnectionPoolEnum, String>
            createFromServerProps(final Properties serverProps) {

        final Map<DbConnectionPoolEnum, String> map = new HashMap<>();

        for (final DbConnectionPoolEnum val : DbConnectionPoolEnum.values()) {
            map.put(val, val.getServerProp().getProperty(serverProps));
        }
        return map;
    }
}
