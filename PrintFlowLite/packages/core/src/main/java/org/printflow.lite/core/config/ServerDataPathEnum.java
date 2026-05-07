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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.printflow.lite.core.util.FilePermissionEnum;
import org.printflow.lite.core.util.FileSystemHelper;

/**
 * File paths relative to the {@link ServerFilePathEnum#DATA} directory
 * <i>without</i> leading or trailing {@link File#separatorChar} {@code '/'}.
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerDataPathEnum {

    /** Home. */
    DATA_HOME(""),

    /**
     * The relative path of database backups.
     */
    DATA_BACKUPS("backups", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the data configuration folder.
     */
    DATA_CONF("conf", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the temporary data folder.
     */
    DATA_TEMP(".tmp", FilePermissionEnum.MODE_700, false),

    /**
     * The relative path of the email outbox folder.
     */
    EMAIL_OUTBOX("email-outbox", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the default SafePages folder.
     */
    INTERNAL("internal", FilePermissionEnum.MODE_700, false),

    /**
     * The relative path of the default SafePages folder.
     */
    DERBY("internal/Derby", FilePermissionEnum.MODE_700, false),

    /**
     * The relative path of the default SafePages folder.
     */
    SAFEPAGES_DEFAULT("internal/safepages", FilePermissionEnum.MODE_700),

    /**
     * Public letterheads.
     */
    LETTERHEADS("internal/letterheads", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the print-jobtickets folder.
     */
    PRINT_JOBTICKETS("print-jobtickets", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the doc archive folder.
     */
    DOC_ARCHIVE("doc-archive", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the doc journal folder.
     */
    DOC_JOURNAL("doc-journal", FilePermissionEnum.MODE_700),

    /**
     * The relative path of the Atom Feeds folder.
     */
    FEEDS("feed", FilePermissionEnum.MODE_700);

    /** */
    private final String path;

    /** */
    private final FilePermissionEnum permissions;

    /** {@code true} if created at server startup. */
    private final boolean createAtStartup;

    /**
     * @param subdir
     *            Relative path in server directory.
     */
    ServerDataPathEnum(final String subdir) {
        this.path = subdir;
        this.permissions = null;
        this.createAtStartup = false;
    }

    /**
     * @param subdir
     *            Relative path in server directory.
     * @param perms
     *            permissions.
     */
    ServerDataPathEnum(final String subdir, final FilePermissionEnum perms) {
        this.path = subdir;
        this.permissions = perms;
        this.createAtStartup = true;
    }

    /**
     * @param subdir
     *            Relative path in server directory.
     * @param perms
     *            permissions.
     * @param create
     *            {@code true} if created at server startup.
     */
    ServerDataPathEnum(final String subdir, final FilePermissionEnum perms,
            final boolean create) {
        this.path = subdir;
        this.permissions = perms;
        this.createAtStartup = create;
    }

    /**
     * @return Relative path in {@link ServerFilePathEnum#DATA} directory.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @return {@code true} if permissions are specified.
     */
    public boolean hasPermissions() {
        return this.permissions != null;
    }

    /**
     * @return permissions
     */
    public FilePermissionEnum getPermissions() {
        return this.permissions;
    }

    /**
     * @param serverHome
     *            Server directory path.
     * @return Absolute path in server directory.
     */
    public Path getPathAbsolute(final Path serverHome) {
        return Path.of(serverHome.toString(), ServerFilePathEnum.DATA.getPath(),
                this.getPath());
    }

    /**
     * @return {@code true} if created at server startup.
     */
    public boolean createAtStartup() {
        return this.createAtStartup;
    }

    /**
     * @param serverHome
     *            Server directory path.
     * @return {@code true} if directory exists.
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

    /**
     * Creates a directory.
     *
     * @param serverHome
     * @throws IOException
     */
    public void createDirectory(final Path serverHome) throws IOException {
        FileSystemHelper.createDirectory(this.getPathAbsolute(serverHome),
                this.permissions);
    }

}
