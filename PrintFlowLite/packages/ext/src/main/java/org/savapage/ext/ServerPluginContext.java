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

import java.io.File;
import java.net.URI;

import org.printflow.lite.ext.rest.RestClient;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface ServerPluginContext {

    /**
     * @return The directory where plug-ins are installed.
     */
    File getPluginHome();

    /**
     * Checks if user is member of group.
     *
     * @param groupName
     *            Name of group.
     * @param userId
     *            User id.
     * @return {@code true} When user is member of group.
     */
    boolean isUserInGroup(String groupName, String userId);

    /**
     * @param uri
     *            Target URI.
     * @return A new {@link RestClient}.
     */
    RestClient createRestClient(URI uri);

    /**
     * @param uri
     *            Target URI.
     * @param username
     *            Basic Auth user.
     * @param password
     *            Basic Auth password.
     * @return A new {@link RestClient}.
     */
    RestClient createRestClient(URI uri, String username, String password);

}
