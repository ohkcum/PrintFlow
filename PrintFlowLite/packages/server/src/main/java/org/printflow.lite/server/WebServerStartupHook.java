/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.printflow.lite.common.SystemPropertyEnum;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.cli.AbstractApp;
import org.printflow.lite.core.cli.AppSSLKeystore;
import org.printflow.lite.core.config.InstallProperties;
import org.printflow.lite.core.config.ServerDataFileNameEnum;
import org.printflow.lite.core.config.ServerDataPathEnum;
import org.printflow.lite.core.config.ServerFileNameEnum;
import org.printflow.lite.core.config.ServerFilePathEnum;
import org.printflow.lite.core.jmx.JmxRemoteProperties;
import org.printflow.lite.server.config.ServerDataFileMigrationEnum;
import org.printflow.lite.server.config.ServerDataResourceEnum;

/**
 * Actions at server startup.
 *
 * @author Rijk Ravestein
 *
 */
public final class WebServerStartupHook {

    /** */
    private final Path serverHomePath;
    /** */
    private final StringBuilder log;

    /** */
    private WebServerStartupHook() {
        this.log = new StringBuilder();
        this.serverHomePath =
                Path.of(SystemPropertyEnum.PRINTFLOWLITE_SERVER_HOME.getValue());
    }

    /**
     * Creates directories and/or (re) apply permissions for all
     * {@link ServerDataPathEnum}.
     *
     * @throws IOException
     */
    private void lazyDataDirs() throws IOException {

        for (final ServerDataPathEnum val : ServerDataPathEnum.values()) {

            if (val.hasPermissions()) {

                if (val.exists(this.serverHomePath)) {
                    val.applyPosixFilePermissions(this.serverHomePath);
                } else if (val.createAtStartup()) {
                    val.createDirectory(this.serverHomePath);
                    this.log.append("\n mkdir ")
                            .append(val.getPathAbsolute(this.serverHomePath));
                }
            }
        }
    }

    /**
     * Lazy creates data files from /resource and (re) apply permissions for all
     * {@link ServerDataFileNameEnum}.
     *
     * @throws IOException
     */
    private void lazyDataFiles() throws IOException {

        // Lazy create from /resource
        for (final ServerDataResourceEnum rc : ServerDataResourceEnum
                .values()) {

            if (rc.lazyCreate(serverHomePath)) {
                this.log.append("\nCopied ").append(
                        rc.getTarget().getPathAbsolute(this.serverHomePath));
                this.log.append("\n  From resource ").append(rc.getResource());
            }
        }

        // Custom creates
        if (!ServerDataFileNameEnum.JMXREMOTE_PASSWORD
                .exists(this.serverHomePath)) {
            final int pwLength = 24;
            JmxRemoteProperties.setAdminPassword(
                    RandomStringUtils.randomAlphanumeric(pwLength));
            this.log.append("\nCreated ")
                    .append(ServerDataFileNameEnum.JMXREMOTE_PASSWORD
                            .getPathAbsolute(this.serverHomePath));
        }

        // set file permissions for all
        for (final ServerDataFileNameEnum val : ServerDataFileNameEnum
                .values()) {
            if (val.exists(this.serverHomePath)) {
                val.applyPosixFilePermissions(this.serverHomePath);
            }
        }
    }

    /**
     * Move data files from location of previous installation.
     *
     * @throws IOException
     */
    private void lazyDataMove() throws IOException {

        for (final ServerDataFileMigrationEnum val : ServerDataFileMigrationEnum
                .values()) {
            this.lazyDataMove(val);
        }
    }

    /**
     * @param lazyMigrate
     * @return {@code true} if file was created.
     * @throws IOException
     */
    private boolean lazyDataMove(final ServerDataFileMigrationEnum lazyMigrate)
            throws IOException {

        final File targetFile = lazyMigrate.getTarget()
                .getPathAbsolute(this.serverHomePath).toFile();

        if (targetFile.exists()) {
            return false;
        }

        boolean targetMustExist = true;

        for (final ServerFileNameEnum candidateSource : lazyMigrate
                .getSourceCandidates()) {

            /// End-of-search?
            if (candidateSource.isEmpty()) {
                targetMustExist = false;
                break;
            }

            final File sourceFile = candidateSource
                    .getPathAbsolute(this.serverHomePath).toFile();

            if (sourceFile.exists()) {

                // (1) "copy and delete", do NOT moveFile() because the
                // target file might be on another file system.

                // Directory of target file is created if it does not exist.
                FileUtils.copyFile(sourceFile, targetFile);

                FileUtils.delete(sourceFile);

                // (2) set file permissions
                lazyMigrate.getTarget()
                        .applyPosixFilePermissions(this.serverHomePath);

                this.log.append("\n Moved ")
                        .append(sourceFile.getAbsolutePath());
                this.log.append("\n    To ")
                        .append(targetFile.getAbsolutePath());

                break;
            }
        }

        if (targetMustExist && !targetFile.exists()) {
            throw new IOException(
                    String.format("[%s] could not be created or moved.",
                            targetFile.getAbsolutePath()));
        }
        return true;
    }

    /**
     * Creates {@link AppSSLKeystore} if not present.
     *
     * @throws Exception
     */
    private void lazySSLKeystore() throws Exception {

        final Path pathKeystore = ServerDataFileNameEnum.DEFAULT_SSL_KEYSTORE
                .getPathAbsolute(this.serverHomePath);
        final Path pathPassword = ServerDataFileNameEnum.DEFAULT_SSL_KEYSTORE_PW
                .getPathAbsolute(this.serverHomePath);

        final boolean existKeystore = pathKeystore.toFile().exists();
        final boolean existPassword = pathPassword.toFile().exists();

        if (existKeystore && existPassword) {
            return;
        }

        final int iRet = AppSSLKeystore.mainAsServer();

        if (iRet == AbstractApp.EXIT_CODE_OK) {

            this.log.append("\nCreated ").append(pathKeystore);

            if (!existPassword) {
                this.log.append("\nCreated ").append(pathPassword);
            }

            ServerDataFileNameEnum.DEFAULT_SSL_KEYSTORE
                    .applyPosixFilePermissions(this.serverHomePath);
            ServerDataFileNameEnum.DEFAULT_SSL_KEYSTORE_PW
                    .applyPosixFilePermissions(this.serverHomePath);

        } else {
            throw new SpException(
                    String.format("Unexpected return code [%d] from %s", iRet,
                            AppSSLKeystore.class.getSimpleName()));
        }
    }

    /**
     * Executes startup actions.
     *
     * @return action log
     * @throws Exception
     */
    private String exec() throws Exception {

        final InstallProperties installProps =
                new InstallProperties(this.serverHomePath);

        if (!installProps.isCompatible()) {
            throw new SpException(installProps.getIncompatibleMsg());
        }

        this.log.append("Checked ").append(
                ServerFilePathEnum.DATA.getPathAbsolute(this.serverHomePath));

        // (1) Create directories + (re) apply permissions
        this.lazyDataDirs();

        // (2) Move data files from locations of previous installation.
        this.lazyDataMove();

        // (3) Create data files from /resource + apply permissions
        this.lazyDataFiles();

        // At this point log4j.properties is present, so ...

        // (4) SSL password file + keystore.
        this.lazySSLKeystore();

        // (5) Update installation properties.
        if (installProps.isCompatible()) {
            if (installProps.updateIfNeeded()) {
                this.log.append("\nCreated ")
                        .append(installProps.getFile().getAbsolutePath());
            }
        }

        return this.log.toString();
    }

    /**
     * Runs startup actions.
     *
     * @return action log (empty if no actions are executed).
     * @throws IOException
     */
    public static String run() throws Exception {
        final WebServerStartupHook startupHook = new WebServerStartupHook();
        return startupHook.exec();
    }

}
