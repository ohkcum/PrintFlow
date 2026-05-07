/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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

import java.util.Date;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.concurrent.ReadWriteLockEnum;
import org.printflow.lite.core.dao.DaoContext;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.AppLogHelper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserRegistrationClean extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    @Override
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        String msgParm = null;
        PubLevelEnum level = PubLevelEnum.INFO;
        long nDeleted = 0;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {
            daoContext.beginTransaction();

            final Date expiryDate = ServiceContext.getServiceFactory()
                    .getUserService().getUserRegistrationExpiry(new Date());

            nDeleted = ServiceContext.getDaoContext().getUserDao()
                    .deleteRegistrationsExpired(expiryDate);

            if (nDeleted > 0) {
                msgParm = String.valueOf(nDeleted);
            }

            daoContext.commit();

        } catch (Exception e) {

            daoContext.rollback();

            level = PubLevelEnum.ERROR;
            msgParm = e.getMessage();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }

        try {

            if (msgParm != null) {

                String msg = null;

                if (level == PubLevelEnum.INFO) {
                    if (nDeleted == 1) {
                        msg = AppLogHelper.logInfo(getClass(),
                                "UserRegistrationClean.success.single");
                    } else {
                        msg = AppLogHelper.logInfo(getClass(),
                                "UserRegistrationClean.success.plural",
                                msgParm);
                    }

                } else {
                    msg = AppLogHelper.logError(getClass(),
                            "UserRegistrationClean.error", msgParm);
                }

                AdminPublisher.instance().publish(PubTopicEnum.DB, level, msg);
            }

        } catch (Exception e) {
            // no code intended
        }
    }

}
