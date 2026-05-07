/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2024 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.printflow.lite.core.util.FilePermissionEnum;

/**
 * File names and their {@link ServerDataPathEnum} location.
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerDataFileNameEnum {

    /** */
    INSTALL_PROPERTIES(ServerDataPathEnum.DATA_HOME, "install.properties"),

    /** */
    ADMIN_PROPERTIES(ServerDataPathEnum.DATA_HOME, "admin.properties",
            FilePermissionEnum.MODE_600),

    /** */
    DEFAULT_SSL_KEYSTORE(ServerDataPathEnum.DATA_HOME, "default-ssl-keystore",
            FilePermissionEnum.MODE_600),
    /** */
    DEFAULT_SSL_KEYSTORE_PW(ServerDataPathEnum.DATA_HOME,
            "default-ssl-keystore.pw", FilePermissionEnum.MODE_600),

    /** */
    ENCRYPTION_PROPERTIES(ServerDataPathEnum.DATA_HOME, "encryption.properties",
            FilePermissionEnum.MODE_600),

    /** */
    INTERNAL_GROUPS_TXT(ServerDataPathEnum.DATA_CONF, "internal-groups.txt"),
    /** */
    INTERNAL_GROUPS_TXT_TMPL(ServerDataPathEnum.DATA_CONF,
            "internal-groups.txt.tmpl"),
    /** */
    USERNAME_ALIASES_TXT(ServerDataPathEnum.DATA_CONF, "username-aliases.txt"),
    /** */
    USERNAME_ALIASES_TXT_TMPL(ServerDataPathEnum.DATA_CONF,
            "username-aliases.txt.tmpl"),

    /** */
    JMXREMOTE_PROPERTIES(ServerDataPathEnum.DATA_HOME, "jmxremote.properties"),
    /**
     * Password file's read access MUST be restricted, otherwise Java throws an
     * exception and the JMX application won't start.
     */
    JMXREMOTE_PASSWORD(ServerDataPathEnum.DATA_HOME, "jmxremote.password",
            FilePermissionEnum.MODE_600),
    /** */
    JMXREMOTE_KS(ServerDataPathEnum.DATA_HOME, "jmxremote.ks",
            FilePermissionEnum.MODE_600),
    /** */
    JMXREMOTE_ACCESS(ServerDataPathEnum.DATA_HOME, "jmxremote.access"),

    /** */
    LOG4J_PROPERTIES(ServerDataPathEnum.DATA_HOME, "log4j.properties"),

    /** */
    MEMBER_CARD(ServerDataPathEnum.DATA_HOME, "PrintFlowLite.membercard"),

    /** */
    SERVER_PROPERTIES(ServerDataPathEnum.DATA_HOME, "server.properties",
            FilePermissionEnum.MODE_600);

    /** */
    private final ServerDataPathEnum path;
    /** */
    private final String basename;
    /** */
    private final FilePermissionEnum permissions;

    /**
     * @param p
     *            File path.
     * @param name
     *            File name.
     */
    ServerDataFileNameEnum(final ServerDataPathEnum p, final String name) {
        this.path = p;
        this.basename = name;
        this.permissions = null;
    }

    /**
     * @param p
     *            File path.
     * @param name
     *            File name.
     * @param perms
     *            file permissions.
     */
    ServerDataFileNameEnum(final ServerDataPathEnum p, final String name,
            final FilePermissionEnum perms) {
        this.path = p;
        this.basename = name;
        this.permissions = perms;
    }

    /**
     * @return File name.
     */
    public String getBaseName() {
        return this.basename;
    }

    /**
     * @return Relative path in {@link ServerFilePathEnum#SERVER_HOME}
     *         directory.
     */
    public Path getPathRelative() {
        return Path.of(ServerFilePathEnum.DATA.getPath(), this.path.getPath(),
                this.basename);
    }

    /**
     * @param serverHome
     *            Server directory path.
     * @return Absolute path in server directory.
     */
    public Path getPathAbsolute(final Path serverHome) {
        return Path.of(serverHome.toString(),
                this.getPathRelative().toString());
    }

    /**
     * @return server path.
     */
    public ServerDataPathEnum getPathEnum() {
        return this.path;
    }

    /**
     * @param serverHome
     *            Server directory path.
     * @return {@code true} if this file exists.
     */
    public boolean exists(final Path serverHome) {
        return this.getPathAbsolute(serverHome).toFile().exists();
    }

    /**
     * @param serverHome
     * @throws IOException
     */
    public void applyPosixFilePermissions(final Path serverHome)
            throws IOException {
        if (this.permissions != null) {
            Files.setPosixFilePermissions(this.getPathAbsolute(serverHome),
                    this.permissions.getPosixFilePermissions());
        }
    }

}
