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
package org.printflow.lite.core.config;

import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.common.ConfigDefaults;
import org.printflow.lite.common.SystemPropertyEnum;
import org.printflow.lite.core.jpa.tools.DatabaseTypeEnum;

/**
 * Server property keys.
 * <p>
 * Also see {@link SystemPropertyEnum}.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerPropEnum {

    /** */
    SERVER_HOST("server.host", ConfigDefaults.SERVER_HOST),
    /** */
    SERVER_PORT("server.port", ConfigDefaults.SERVER_PORT),
    /** */
    SERVER_SSL_PORT("server.ssl.port", ConfigDefaults.SERVER_SSL_PORT),
    /** */
    SERVER_SSL_PORT_LOCAL("server.ssl.port.local",
            ConfigDefaults.SERVER_SSL_PORT_LOCAL),

    /** */
    SERVER_HTTP2("server.http2", ServerPropEnum.TRUE),
    /** */
    SERVER_HTTP2_MAX_REQUESTS_PER_SECOND("server.http2.max-requests-per-sec",
            "128"),

    /** */
    SERVER_PRINT_PORT_RAW("server.print.port.raw", "9100"),

    /** */
    SERVER_HTML_REDIRECT_SSL("server.html.redirect.ssl", ServerPropEnum.FALSE),

    /** */
    SERVER_SSL_KEYSTORE("server.ssl.keystore"),
    /** */
    SERVER_SSL_KEYSTORE_PASSWORD("server.ssl.keystore-password"),
    /** */
    SERVER_SSL_KEY_PASSWORD("server.ssl.key-password"),

    /** */
    APP_DIR_TMP("app.dir.tmp"),
    /** */
    APP_DIR_SAFEPAGES("app.dir.safepages"),
    /** */
    APP_DIR_LETTERHEADS("app.dir.letterheads"),
    /** */
    APP_DIR_DOC_STORE_ARCHIVE("app.dir.doc.store.archive"),
    /** */
    APP_DIR_DOC_STORE_JOURNAL("app.dir.doc.store.journal"),

    /** */
    DB_TYPE("database.type", DatabaseTypeEnum.Internal.getPropertiesId()),

    /** The JDBC driver, like "org.postgresql.Driver". */
    DB_DRIVER("database.driver"),
    /** The hibernate dialect. */
    DB_HIBERNATE_DIALECT("database.hibernate.dialect"),
    /** */
    DB_URL("database.url"),
    /** */
    DB_USER("database.user"),
    /** */
    DB_PASS("database.password"),

    /**
     * Minimum number of JDBC connections in the pool.
     */
    DB_CONNECTION_POOL_MIN("database.connection.pool.min", "5"),
    /**
     * Maximum number of JDBC connections in the pool.
     */
    DB_CONNECTION_POOL_MAX("database.connection.pool.max", "200"),
    /**
     * When an idle connection is removed from the pool (in second). Hibernate
     * default: 0, never expire.
     */
    DB_CONNECTION_IDLE_TIMEOUT_SECS("database.connection.idle-timeout-secs",
            "600"),
    /**
     * Idle time in seconds before a connection is automatically validated.
     * (Hibernate default: 0).
     *
     * IMPORTANT: this value must be LESS than {@link #TIMEOUT_SECS}. If not,
     * the connections closed by the database will not be properly detected.
     */
    DB_CONNECTION_IDLE_TIMEOUT_TEST_SECS(
            "database.connection.idle-timeout-test-secs", "120"),
    /**
     * Number of prepared statements that will be cached. Increase performance.
     * Hibernate default: 0 , caching is disable.
     */
    DB_CONNECTION_STATEMENT_CACHE("database.connection.statement-cache", "50"),

    /** */
    CUPS_NOTIFIER("cups.notifier", "PrintFlowLite"),
    /** */
    CUPS_SERVER_PORT("cups.server.port", "631"),

    /** */
    OPENPGP_PUBLICKEY_FILE("pgp.publickey.file"),
    /** */
    OPENPGP_SECRETKEY_FILE("pgp.secretkey.file"),
    /** */
    OPENPGP_SECRETKEY_PASSPHRASE("pgp.secretkey.passphrase"),

    /** */
    SESSION_SCAVENGE_INTERVAL_SEC("server.session.scavenge.interval-sec",
            "600"),

    /** */
    START_CLEANUP_DOCLOG("start.cleanup-doclog", ServerPropEnum.TRUE),
    /** */
    START_CLEANUP_USERHOME("start.cleanup-userhome", ServerPropEnum.TRUE),
    /** */
    SYSTEM_CLEANUP_USERHOME_TEST("system.cleanup-userhome.test",
            ServerPropEnum.FALSE),

    /** */
    THREADPOOL_QUEUE_CAPACITY("server.threadpool.queue.capacity", "3000"),
    /** */
    THREADPOOL_MAXTHREADS("server.threadpool.maxthreads", "200"),
    /** */
    THREADPOOL_MINTHREADS("server.threadpool.minthreads", "20"),
    /** */
    THREADPOOL_IDLE_TIMEOUT_MSEC("server.threadpool.idle-timeout-msec",
            "30000"),

    /** */
    VISITOR_ORGANIZATION("visitor.organization", "Your Organization Name"),

    /** */
    WEBAPP_CUSTOM_I18N("webapp.custom.i18n", ServerPropEnum.FALSE),

    /** Undocumented ad-hoc property for testing purposes. */
    IPP_TRUST_IP_USER("ipp.trust.ip-user");

    /** */
    private static final String TRUE = BooleanUtils.TRUE;
    /** */
    private static final String FALSE = BooleanUtils.FALSE;

    /** */
    private final String propKey;

    /** */
    private final String propDefault;

    /**
     * @param k
     *            key
     */
    ServerPropEnum(final String k) {
        this.propKey = k;
        this.propDefault = null;
    }

    /**
     * @param k
     *            key
     * @param d
     *            default value
     */
    ServerPropEnum(final String k, final String d) {
        this.propKey = k;
        this.propDefault = d;
    }

    /**
     * @return Key
     */
    public String key() {
        return this.propKey;
    }

    /**
     * @return Default value. {@code null} if no default available.
     */
    public String defaultValue() {
        return this.propDefault;
    }

    /**
     * Gets the value from the property list or, if not present, the default
     * value of this enum, or {@code null} if no default available.
     *
     * @param props
     *            property list
     * @return value from properties or enum default.
     */
    public String getProperty(final Properties props) {
        return props.getProperty(this.key(), this.defaultValue());
    }

    /**
     * Gets server property value as boolean.
     *
     * @param props
     *            server properties.
     * @return boolean value.
     * @throws IllegalArgumentException
     *             if the property value does not contain a parsable boolean.
     */
    public boolean getPropertyBoolean(final Properties props) {
        final String val = this.getProperty(props);
        if (val != null && (val.equals(TRUE) || val.equals(FALSE))) {
            return Boolean.valueOf(val);
        }
        throw new IllegalArgumentException(
                String.format("%s [%s] is not a boolean. ", this.key(), val));
    }

    /**
     * Gets server property value as integer.
     *
     * @param props
     *            server properties.
     * @return int value.
     * @throws IllegalArgumentException
     *             if the property value does not contain a parsable integer.
     */
    public int getPropertyInt(final Properties props)
            throws IllegalArgumentException {
        final String val = this.getProperty(props);
        if (val == null || !StringUtils.isNumeric(val)) {
            throw new IllegalArgumentException(String
                    .format("%s [%s] is not an integer. ", this.key(), val));
        }
        return Integer.parseInt(val);
    }

}
