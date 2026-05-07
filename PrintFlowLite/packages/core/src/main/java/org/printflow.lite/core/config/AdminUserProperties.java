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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.crypto.CryptoUser;
import org.printflow.lite.core.dao.UserDao;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AdminUserProperties implements IServerDataFile {

    /** */
    private static final String PROP_ADMIN_PASSWORD = "admin.password";
    /** */
    private static final String ADMIN_PASSWORD_DEFAULT = "admin";

    /** */
    private final Path serverHomePath;
    /** */
    private final ServerDataFileNameEnum fileNameEnum;
    /**
     * The {@link ServerDataFileNameEnum#ADMIN_PROPERTIES} file.
     */
    private final File propsFile;

    /** */
    private final Properties props;

    /**
     * @param serverHome
     *            server home.
     */
    public AdminUserProperties(final Path serverHome) {
        this.serverHomePath = serverHome;
        this.fileNameEnum = ServerDataFileNameEnum.ADMIN_PROPERTIES;
        this.propsFile = this.fileNameEnum.getPathAbsolute(serverHome).toFile();
        this.props = new Properties();
        this.read();
    }

    /**
     * @return Plain default password.
     */
    public static String getDefaultPasswordPlain() {
        return ADMIN_PASSWORD_DEFAULT;
    }

    /**
     * @return HASH encrypted password
     */
    public String getPassword() {
        return this.props.getProperty(PROP_ADMIN_PASSWORD);
    }

    /**
     * Sets the password of the internal administrator.
     *
     * @param plainPassword
     *            The plain password as entered by the user.
     */
    public void store(final String plainPassword) {

        final String pw = CryptoUser.getHashedUserPassword(
                UserDao.INTERNAL_ADMIN_USERID, plainPassword);

        this.props.setProperty(PROP_ADMIN_PASSWORD, pw);

        try {
            // (1) store
            this.props.store(new FileOutputStream(this.propsFile),
                    "The admin password can be changed here. "
                            + "PrintFlowLite will convert it to a hash.");

            // (2) set file permissions
            this.fileNameEnum.applyPosixFilePermissions(this.serverHomePath);

        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Reads the properties files for the administrator. When a "raw" admin
     * password is encountered, the properties file is updated with the HASH
     * checksum of the password.
     */
    private void read() {

        if (this.propsFile.exists()) {
            try {
                this.props.load(new java.io.FileInputStream(this.propsFile));
            } catch (IOException e) {
                throw new SpException(this.propsFile + " is missing.", e);
            }

        } else {
            this.props.setProperty(PROP_ADMIN_PASSWORD, ADMIN_PASSWORD_DEFAULT);
            SpInfo.instance().log(String.format("Created %s",
                    this.fileNameEnum.getBaseName()));
        }

        final String pw = this.props.getProperty(PROP_ADMIN_PASSWORD);
        if (!pw.startsWith(CryptoUser.INTERNAL_USER_PW_CHECKSUM_PREFIX)) {
            store(pw);
        }
    }

}
