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
package org.printflow.lite.core.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Properties;

import org.printflow.lite.common.VersionInfo;
import org.printflow.lite.core.community.CommunityDictEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InstallProperties implements IServerDataFile {

    /** */
    private static final String KEY_DIR_LAYOUT_PFX = "dir.layout.";
    /** */
    private static final String KEY_DIR_LAYOUT_MAJOR =
            KEY_DIR_LAYOUT_PFX + "major";
    /** */
    private static final String KEY_DIR_LAYOUT_MINOR =
            KEY_DIR_LAYOUT_PFX + "minor";

    /** */
    private final Path serverHomePath;

    /** */
    private final File propsFile;
    /** */
    private final Properties props;

    /** */
    private boolean compatible;

    /** */
    private boolean updateNeeded;

    /** */
    private String msgIncompatible;

    /**
     * @param serverHome
     * @throws IOException
     */
    public InstallProperties(final Path serverHome) throws IOException {

        this.serverHomePath = serverHome;
        this.props = new Properties();
        this.propsFile = ServerDataFileNameEnum.INSTALL_PROPERTIES
                .getPathAbsolute(this.serverHomePath).toFile();

        this.checkDataDirLayout();
    }

    /**
     * @return The properties file.
     */
    public File getFile() {
        return this.propsFile;
    }

    /**
     * Writes the properties to file if needed.
     *
     * @return {@code true} if updated or created.
     * @throws IOException
     */
    public boolean updateIfNeeded() throws IOException {
        if (this.updateNeeded) {
            try (Writer writer = new FileWriter(this.propsFile)) {
                this.props.setProperty(KEY_DIR_LAYOUT_MAJOR,
                        VersionInfo.DIR_LAYOUT_VERSION_MAJOR);
                this.props.setProperty(KEY_DIR_LAYOUT_MINOR,
                        VersionInfo.DIR_LAYOUT_VERSION_MINOR);
                this.props.store(writer, this.getFileComments());
            }
        }
        return this.updateNeeded;
    }

    /**
     * @return {@code true} if the current {@link ServerFilePathEnum#DATA}
     *         layout is compatible with (or can be migrated to) the layout
     *         required by this server instance.
     */
    public boolean isCompatible() {
        return this.compatible;
    }

    /**
     * @return message in case {@link ServerFilePathEnum#DATA} layout is
     *         incompatible with the layout required by this server instance, or
     *         {@code null if compatible}.
     */
    public String getIncompatibleMsg() {
        return this.msgIncompatible;
    }

    /**
     * @return comment header text for the stored
     *         {@link ServerDataFileNameEnum#INSTALL_PROPERTIES}.
     */
    private String getFileComments() {

        final String line = "---------------------------"
                + "-------------------------------";

        return line + "\n " + CommunityDictEnum.PrintFlowLite.getWord()
                + " Installation Details - DO NOT EDIT\n" + line;
    }

    /**
     * Reads the properties from file.
     *
     * @throws IOException
     */
    private void read() throws IOException {
        try (InputStream istr = new java.io.FileInputStream(this.propsFile)) {
            this.props.load(istr);
        }
    }

    /**
     * Checks if the current {@link ServerFilePathEnum#DATA} layout is
     * compatible with (or can be migrated to) the one required by this server
     * instance.
     *
     * @throws IOException
     */
    private void checkDataDirLayout() throws IOException {

        if (this.propsFile.exists()) {

            this.read();

            final int nMajorCur = Integer
                    .parseInt(this.props.getProperty(KEY_DIR_LAYOUT_MAJOR));
            final int nMinorCur = Integer
                    .parseInt(this.props.getProperty(KEY_DIR_LAYOUT_MINOR));

            final int nMajorReq =
                    Integer.parseInt(VersionInfo.DIR_LAYOUT_VERSION_MAJOR);
            final int nMinorReq =
                    Integer.parseInt(VersionInfo.DIR_LAYOUT_VERSION_MINOR);

            if (nMajorCur > nMajorReq) {
                this.compatible = false;
            } else if (nMajorCur < nMajorReq) {
                this.compatible = true;
            } else {
                this.compatible = nMinorCur <= nMinorReq;
            }

            if (!this.compatible) {
                this.msgIncompatible =
                        String.format(
                                "Current layout version %d.%d of %s is "
                                        + "incompatible with "
                                        + "required version %d.%d",
                                nMajorCur, nMinorCur,
                                ServerFilePathEnum.DATA
                                        .getPathAbsolute(this.serverHomePath),
                                nMajorReq, nMinorReq);
            }
            this.updateNeeded =
                    nMajorCur != nMajorReq || nMinorCur != nMinorReq;
        } else {
            this.updateNeeded = true;
            this.compatible = true;
        }

    }

}
