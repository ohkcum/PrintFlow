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
package org.printflow.lite.common;

/**
 * Configuration Defaults.
 *
 * @author Rijk Ravestein
 *
 */
public final class ConfigDefaults {

    /**
     * The single word (tm) application name: DO NOT CHANGE THIS NAME.
     */
    public static final String APP_NAME = "PrintFlowLite";

    /**
     * The default host is {@code null}: bind to all interfaces.
     */
    public static final String SERVER_HOST = null;

    /**
     * The default port.
     */
    public static final String SERVER_PORT = "8631";

    /**
     * The main SSL port.
     */
    public static final String SERVER_SSL_PORT = "8632";

    /**
     * The extra SSL port.
     */
    public static final String SERVER_SSL_PORT_LOCAL = "8633";

    /**
     * Prevent instantiation.
     */
    private ConfigDefaults() {
    }

}
