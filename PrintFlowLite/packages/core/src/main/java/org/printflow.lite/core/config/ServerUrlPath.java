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

import org.printflow.lite.common.IUtility;

/**
 * Server URL path constants <i>without</i> leading or trailing {@code '/'}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ServerUrlPath implements IUtility {

    /**
     * Utility class.
     */
    private ServerUrlPath() {
    }

    /**
     * Custom web files.
     */
    public static final String CUSTOM_WEB = "custom/web";

    /**
     * Custom web themes.
     */
    public static final String CUSTOM_WEB_THEMES = CUSTOM_WEB + "/themes";

    /**
     * Documentation.
     */
    public static final String DOCS = "docs";

    /** */
    public static final String LANDING_PAGE_HTML_FILE = "index.html";

    /**
     * User Manual home.
     */
    public static final String DOCS_MANUAL = DOCS + "/manual";

    /**
     * User Manual landing page.
     */
    public static final String DOCS_MANUAL_INDEX_HTML =
            DOCS_MANUAL + "/" + LANDING_PAGE_HTML_FILE;

    /**
     * Licenses home.
     */
    public static final String DOCS_LICENSES = DOCS + "/licenses";

    /**
     * Licenses landing page.
     */
    public static final String DOCS_LICENSES_INDEX_HTML =
            DOCS_LICENSES + "/" + LANDING_PAGE_HTML_FILE;

    /**
     * LibreJS license info injector.
     */
    public static final String LIBREJS = "LibreJS";

    /**
     * XML-RPC.
     */
    public static final String XMLRPC = "xmlrpc";

}
