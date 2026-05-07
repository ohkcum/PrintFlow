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
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.community.MemberCard;
import org.printflow.lite.core.concurrent.ReadWriteLockEnum;
import org.printflow.lite.core.dao.DaoContext;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.AppLogHelper;
import org.printflow.lite.core.util.Messages;
import org.slf4j.LoggerFactory;

/**
 * Checks the state of the Membership Card registration.
 *
 * @author Rijk Ravestein
 *
 */
public final class MemberCardCheckJob extends AbstractJob {

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
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

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        final DaoContext daoContext = ServiceContext.getDaoContext();

        daoContext.beginTransaction();

        boolean committed = false;

        try {
            MemberCard.instance().init();

            daoContext.commit();
            committed = true;

            msg = Messages.getMessage(getClass(), "MemberCardCheckJob.success",
                    new String[] { CommunityDictEnum.MEMBERSHIP.getWord(),
                            MemberCard.instance().getCommunityNotice() });

        } catch (Exception e) {

            level = PubLevelEnum.ERROR;

            msg = AppLogHelper.logError(getClass(), "MemberCardCheckJob.error",
                    CommunityDictEnum.MEMBERSHIP.getWord(), e.getMessage());

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

        } finally {
            if (!committed) {
                daoContext.rollback();
            }
        }
        AdminPublisher.instance().publish(PubTopicEnum.MEMBERSHIP, level, msg);
    }

}