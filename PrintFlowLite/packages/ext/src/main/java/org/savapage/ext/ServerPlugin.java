/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.ext;

import java.util.Properties;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface ServerPlugin {

    /**
     *
     * @return The unique ID.
     */
    String getId();

    /**
     *
     * @return The user friendly name.
     */
    String getName();

    /**
     * Notifies the start of the plug-in.
     *
     * @throws ServerPluginException
     *             When error occurs.
     */
    void onStart() throws ServerPluginException;

    /**
     * Notifies the stop of the plug-in.
     *
     * @throws ServerPluginException
     *             When error occurs.
     */
    void onStop() throws ServerPluginException;

    /**
     * Notifies plug-in initialization.
     *
     * @param pluginId
     *            The unique ID.
     * @param pluginName
     *            The user friendly name.
     * @param live
     *            {@code true} when live, {@code false} when in test mode.
     * @param online
     *            {@code true} when plug-in is online, {@code false} when
     *            offline.
     * @param props
     *            The plug-in {@link Properties}.
     * @param context
     *            The plug-in context.
     *
     * @throws ServerPluginException
     *             When notification failed.
     */
    void onInit(String pluginId, String pluginName, boolean live,
            boolean online, Properties props, ServerPluginContext context)
            throws ServerPluginException;
}
