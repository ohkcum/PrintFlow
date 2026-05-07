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
package org.printflow.lite.core.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.services.IppClientService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.AppLogHelper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsPushEventSubsRenewal extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final IppClientService ippClientSvc =
                ServiceContext.getServiceFactory().getIppClientService();

        final AdminPublisher publisher = AdminPublisher.instance();

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        try {
            if (ippClientSvc.startCUPSPushEventSubscription()) {
                /*
                 * Success is not logged in the AppLog, but echoed to the Admin
                 * Dashboard.
                 */
                msg = localizeSysMsg("CupsSubsRenew.success");
            }

        } catch (Exception e) {

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

            level = PubLevelEnum.ERROR;
            msg = AppLogHelper.logError(getClass(), "CupsSubsRenew.error",
                    e.getMessage());
        }

        if (msg != null) {
            publisher.publish(PubTopicEnum.CUPS, level, msg);
        }

    }

}
