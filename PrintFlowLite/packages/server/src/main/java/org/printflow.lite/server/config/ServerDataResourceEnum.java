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
package org.printflow.lite.server.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.printflow.lite.core.config.ServerDataFileNameEnum;
import org.printflow.lite.core.config.ServerFileNameEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerDataResourceEnum {

    /** */
    JMX_PROPERTIES("setup/jmxremote.properties",
            ServerDataFileNameEnum.JMXREMOTE_PROPERTIES),
    /** */
    JMXREMOTE_ACCESS("setup/jmxremote.access",
            ServerDataFileNameEnum.JMXREMOTE_ACCESS),
    /** */
    LOG4J_PROPERTIES("setup/log4j.properties",
            ServerDataFileNameEnum.LOG4J_PROPERTIES,
            ServerFileNameEnum.LOG4J_PROPERTIES_TEMPLATE),
    /** */
    SERVER_PROPERTIES("setup/server.properties",
            ServerDataFileNameEnum.SERVER_PROPERTIES,
            ServerFileNameEnum.SERVER_PROPERTIES_TEMPLATE),

    /** */
    INTERNAL_GROUPS_TXT_TMPL("setup/internal-groups.txt.tmpl",
            ServerDataFileNameEnum.INTERNAL_GROUPS_TXT_TMPL),
    /** */
    USERNAME_ALIASES_TXT_TMPL("setup/username-aliases.txt.tmpl",
            ServerDataFileNameEnum.USERNAME_ALIASES_TXT_TMPL);

    /** */
    private final String resource;
    /** Main target. */
    private final ServerDataFileNameEnum target;
    /** Optional template target. */
    private final ServerFileNameEnum targetTmpl;

    /**
     * @param rsc
     *            resource.
     * @param trg
     *            target.
     */
    ServerDataResourceEnum(final String rsc, final ServerDataFileNameEnum trg) {
        this.resource = rsc;
        this.target = trg;
        this.targetTmpl = null;
    }

    /**
     * @param rsc
     *            resource.
     * @param trg
     *            target.
     * @param tmpl
     *            template target.
     */
    ServerDataResourceEnum(final String rsc, final ServerDataFileNameEnum trg,
            final ServerFileNameEnum tmpl) {
        this.resource = rsc;
        this.target = trg;
        this.targetTmpl = tmpl;
    }

    /**
     * @return target.
     */
    public ServerDataFileNameEnum getTarget() {
        return this.target;
    }

    /**
     * @return resource.
     */
    public String getResource() {
        return this.resource;
    }

    /**
     * Creates main and (optional) template file if they do not exist.
     *
     * @param serverHomePath
     * @return {@code true} if main file was lazy created.
     * @throws IOException
     */
    public boolean lazyCreate(final Path serverHomePath) throws IOException {

        if (this.targetTmpl != null) {
            final File targetFileTmpl =
                    this.targetTmpl.getPathAbsolute(serverHomePath).toFile();

            /*
             * File is present just after an install. Ad-hoc create from
             * resource if it was deleted by user.
             */
            if (!targetFileTmpl.exists()) {
                this.copyResourceToFile(targetFileTmpl);
            }
        }

        final File targetFile =
                this.target.getPathAbsolute(serverHomePath).toFile();
        if (targetFile.exists()) {
            return false;
        }
        this.copyResourceToFile(targetFile);
        return true;
    }

    /**
     * @param targetFile
     * @throws IOException
     */
    private void copyResourceToFile(final File targetFile) throws IOException {

        try (InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream(this.resource);
                FileOutputStream outputStream =
                        new FileOutputStream(targetFile);) {

            int byteWlk = inputStream.read();

            while (byteWlk >= 0) {
                outputStream.write(byteWlk);
                byteWlk = inputStream.read();
            }
            outputStream.flush();
            outputStream.close();
        }
    }

}
