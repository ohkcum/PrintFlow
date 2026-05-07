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
import java.nio.file.Path;

/**
 * File paths relative to the {@code server} directory <i>without</i> leading or
 * trailing {@link File#separatorChar} {@code '/'}.
 *
 * @author Rijk Ravestein
 *
 */
public enum ServerFilePathEnum {

    /** */
    SERVER_HOME(""),

    /** The relative path executable x64 binaries. */
    BIN_LINUX_X64("bin/linux-x64"),

    /** The relative path executable i686 binaries. */
    BIN_LINUX_I686("bin/linux-i686"),

    /**
     * The relative path of the client folder.
     */
    CLIENT("../client"),

    /**
     * The relative path of the custom template files.
     */
    CUSTOM_TEMPLATE("custom/template"),

    /**
     * The relative path of the CUPS custom properties files.
     */
    CUSTOM_CUPS("custom/cups"),

    /**
     * The relative path of the CUPS custom i18n XML files.
     */
    CUSTOM_CUPS_I18N("custom/cups/i18n"),

    /**
     * The relative path of the custom i18n properties files.
     */
    CUSTOM_I18N("custom/i18n"),

    /**
     * The relative path of the HTML injectable files.
     */
    CUSTOM_HTML("custom/html"),

    /**
     * The relative path of files used for Raw Print PDL creation.
     */
    CUSTOM_RAWPRINT("custom/rawprint"),

    /**
     * The relative path of Raw Print PPD files.
     */
    CUSTOM_RAWPRINT_PPD("custom/rawprint/ppd"),

    /**
     * The relative path of PDL transform files.
     */
    CUSTOM_RAWPRINT_TRANSFORMS("custom/rawprint/transforms"),

    /**
     * The relative path of the custom web files.
     */
    CUSTOM_WEB("custom/web"),

    /**
     * The relative path of the custom web themes.
     */
    CUSTOM_WEB_THEMES("custom/web/themes"),

    /**
     * The relative path of the data folder.
     */
    DATA("data"),

    /**
     * Documentation.
     */
    DOCS("docs"),

    /**
     * User Manual.
     */
    DOCS_MANUAL("docs/manual"),

    /**
     * Licenses.
     */
    DOCS_LICENSES("docs/licenses"),

    /**
     * Extensions.
     */
    EXT("ext"),

    /**
     * Examples.
     */
    EXAMPLES("examples"),

    /**
     * Data examples.
     */
    EXAMPLES_DATA("examples/data"),

    /**
     * Extension JAR files.
     */
    EXT_LIB("ext/lib"),

    /** */
    LIB("lib"),

    /**
     * SQL scripts.
     */
    LIB_SQL("lib/sql"),

    /**
     * Server jar files.
     */
    LIB_WEB("lib/web"),

    /**
     * Log files.
     */
    LOGS("logs");

    /** */
    private final String path;

    /**
     *
     * @param subdir
     *            Relative path in server directory.
     */
    ServerFilePathEnum(final String subdir) {
        this.path = subdir;
    }

    /**
     * @return Relative path in server directory.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @param serverHome
     *            Server directory path.
     * @return Absolute path in server directory.
     */
    public Path getPathAbsolute(final Path serverHome) {
        return Path.of(serverHome.toString(), this.getPath());
    }

}
