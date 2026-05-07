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
import java.nio.file.Path;

import org.printflow.lite.core.util.FilePermissionEnum;
import org.printflow.lite.core.util.FileSystemHelper;

/**
 * File names and their {@link ServerFilePathEnum} location.
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerFileNameEnum {

    /** Legacy file path. */
    ADMIN_PROPERTIES_PRV(ServerFilePathEnum.SERVER_HOME, "admin.properties"),

    /** Legacy file path. */
    SERVER_PROPERTIES_PRV(ServerFilePathEnum.SERVER_HOME, "server.properties"),

    /** Legacy file path. */
    LOG4J_PROPERTIES_PRV(ServerFilePathEnum.LIB, "log4j.properties"),

    /** Template. */
    SERVER_PROPERTIES_TEMPLATE(ServerFilePathEnum.EXAMPLES_DATA,
            "server.properties.template"),
    /** Template. */
    LOG4J_PROPERTIES_TEMPLATE(ServerFilePathEnum.EXAMPLES_DATA,
            "log4j.properties.template"),

    /** Legacy file path. */
    MEMBER_CARD_PRV(ServerFilePathEnum.SERVER_HOME, "PrintFlowLite.membercard"),

    /** */
    SERVER_STARTED_TXT(ServerFilePathEnum.LOGS, "server.started.txt"),

    /** "no action needed" placeholder. */
    FILE_NO_ACTION();

    /** */
    private final ServerFilePathEnum path;
    /** */
    private final String basename;
    /** */
    private final FilePermissionEnum permissions;

    /** */
    ServerFileNameEnum() {
        this.path = null;
        this.basename = null;
        this.permissions = null;
    }

    /**
     * @param p
     *            File path.
     * @param name
     *            File name.
     */
    ServerFileNameEnum(final ServerFilePathEnum p, final String name) {
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
    ServerFileNameEnum(final ServerFilePathEnum p, final String name,
            final FilePermissionEnum perms) {
        this.path = p;
        this.basename = name;
        this.permissions = perms;
    }

    /**
     * @return File name or {@code null} if empty placeholder.
     */
    public String getBaseName() {
        return this.basename;
    }

    /**
     * @param serverHome
     *            Server directory path.
     * @return Absolute path in server directory or {@code null} if empty
     *         placeholder.
     */
    public Path getPathAbsolute(final Path serverHome) {
        if (this.isEmpty()) {
            return null;
        }
        return Path.of(serverHome.toString(), this.path.getPath(),
                this.basename);
    }

    /**
     * @return server path or {@code null} if empty placeholder..
     */
    public ServerFilePathEnum getPathEnum() {
        return this.path;
    }

    /**
     * @return {@code true} if empty placeholder.
     */
    public boolean isEmpty() {
        return this.path == null || this.basename == null;
    }

    /**
     * @param serverHome
     * @throws IOException
     */
    public void applyPosixFilePermissions(final Path serverHome)
            throws IOException {
        if (this.permissions != null) {
            FileSystemHelper.applyPosixFilePermissions(
                    this.getPathAbsolute(serverHome), this.permissions);
        }
    }

}
