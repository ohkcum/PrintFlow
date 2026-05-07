/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server.api.request;

import org.printflow.lite.core.ipp.IppSyntaxException;
import org.printflow.lite.core.ipp.client.IppConnectException;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.system.DnssdServiceCache;

/**
 * Re-initialize the CUPS printer cache.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterSync extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IppSyntaxException {

        try {
            DnssdServiceCache.clear();
            PROXY_PRINT_SERVICE.initPrinterCache();

            setApiResult(ApiResultCodeEnum.OK, "msg-printer-sync-ok");

        } catch (IppConnectException e) {

            setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-printer-connection-broken");
        }

    }
}
