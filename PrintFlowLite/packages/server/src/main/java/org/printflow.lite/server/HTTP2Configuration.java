/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2026 Datraverse B.V. <info@datraverse.com>
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

import java.util.Properties;

import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.config.ServerPropEnum;

/**
 * HTTP/2 configuration properties.
 *
 * @author Rijk Ravestein
 *
 */
public final class HTTP2Configuration {

    /** */
    private final boolean enabled;
    /** */
    private final int maxRequestsPerSec;

    /**
     * @param propsServer
     *            server properties, see {@link ServerPropEnum}.
     */
    public HTTP2Configuration(final Properties propsServer) {
        this.enabled =
                ServerPropEnum.SERVER_HTTP2.getPropertyBoolean(propsServer);
        this.maxRequestsPerSec =
                ServerPropEnum.SERVER_HTTP2_MAX_REQUESTS_PER_SECOND
                        .getPropertyInt(propsServer);
    }

    /**
     * @return {@code true} if HTTP/2 is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return Max requests per second. If exceeded ERR_CONNECTION_CLOSED.
     */
    public int getMaxRequestsPerSec() {
        return maxRequestsPerSec;
    }

    /**
     * Log an INFO message.
     */
    public void log() {
        final SpInfo logger = SpInfo.instance();
        final String logPfx = "    HTTP/2";

        if (this.enabled) {
            logger.log(String.format("%s - ✅ Enabled", logPfx));
            logger.log(String.format("%s - MaxRequestsPerSec [%d]", logPfx,
                    this.maxRequestsPerSec));
        } else {
            logger.log(String.format("%s - ❌ Disabled", logPfx));
        }
    }

}
