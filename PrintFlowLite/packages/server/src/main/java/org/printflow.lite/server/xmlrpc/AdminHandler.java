/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2011-2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2011-2026 Datraverse B.V. <info@datraverse.com>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.server.xmlrpc;

import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.job.SpJobScheduler;
import org.printflow.lite.core.services.IppClientService;
import org.printflow.lite.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AdminHandler implements IXmlRpcHandler {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminHandler.class);

    /** */
    private static final IppClientService IPP_CLIENT_SERVICE =
            ServiceContext.getServiceFactory().getIppClientService();

    /**
     *
     * @return {@code 0} (zero) when successful.
     */
    public int cupsSubscriptionStart() {
        int rc = 1;
        try {
            if (IPP_CLIENT_SERVICE.startCUPSPushEventSubscription()) {
                SpJobScheduler.resumeCUPSPushEventRenewal();
            }
            rc = 0;
        } catch (Exception e) {
            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return rc;
    }

    /**
     *
     * @return {@code 0} (zero) when successful.
     */
    public int cupsSubscriptionStop() {
        int rc = 1;
        try {
            SpJobScheduler.pauseCUPSPushEventRenewal();
            IPP_CLIENT_SERVICE.stopCUPSEventSubscription();
            rc = 0;
        } catch (Exception e) {
            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return rc;
    }

}
