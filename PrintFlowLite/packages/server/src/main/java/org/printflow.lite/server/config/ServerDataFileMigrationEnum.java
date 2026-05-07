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

import java.util.ArrayList;
import java.util.List;

import org.printflow.lite.core.config.ServerDataFileNameEnum;
import org.printflow.lite.core.config.ServerFileNameEnum;
import org.printflow.lite.core.config.ServerFilePathEnum;

/**
 * Data files of previous versions that are migrated to current
 * {@link ServerFilePathEnum#DATA} location.
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerDataFileMigrationEnum {

    /** */
    LOG4J_PROPERTIES(ServerDataFileNameEnum.LOG4J_PROPERTIES, //
            // From previous installation
            ServerFileNameEnum.LOG4J_PROPERTIES_PRV, //
            // Default is lazy created from Java resource file.
            ServerFileNameEnum.FILE_NO_ACTION //
    ),
    /** */
    SERVER_PROPERTIES(ServerDataFileNameEnum.SERVER_PROPERTIES, //
            // From previous installation
            ServerFileNameEnum.SERVER_PROPERTIES_PRV, //
            // Default is lazy created from Java resource file.
            ServerFileNameEnum.FILE_NO_ACTION //
    ),
    /** */
    ADMIN_PROPERTIES(ServerDataFileNameEnum.ADMIN_PROPERTIES, //
            // From previous installation
            ServerFileNameEnum.ADMIN_PROPERTIES_PRV,
            // Default is lazy created by Java code upon first use.
            ServerFileNameEnum.FILE_NO_ACTION //
    ),
    /** */
    MEMBER_CARD(ServerDataFileNameEnum.MEMBER_CARD, //
            // From previous location
            ServerFileNameEnum.MEMBER_CARD_PRV, //
            // Card might not be present.
            ServerFileNameEnum.FILE_NO_ACTION //
    );

    /** First file in the varargs is target file to create. */
    private ServerDataFileNameEnum target;

    /**
     * Next files in the varargs are source file candidates (in order of
     * priority) to copy to target.
     */
    private final List<ServerFileNameEnum> candidateSources;

    /**
     * @param dataFileName
     * @param files
     *            First file is target file to create. Next files are source
     *            file candidates (in order of priority) to copy from if target
     *            does not exist.
     */
    ServerDataFileMigrationEnum(final ServerDataFileNameEnum dataFileName,
            final ServerFileNameEnum... files) {

        this.target = dataFileName;
        this.candidateSources = new ArrayList<>();

        for (ServerFileNameEnum serverFile : files) {
            this.candidateSources.add(serverFile);
        }
    }

    /**
     * @return target file.
     */
    public ServerDataFileNameEnum getTarget() {
        return this.target;
    }

    /**
     * @return source candidates in search order "first found, first applied".
     */
    public List<ServerFileNameEnum> getSourceCandidates() {
        return this.candidateSources;
    }

}
