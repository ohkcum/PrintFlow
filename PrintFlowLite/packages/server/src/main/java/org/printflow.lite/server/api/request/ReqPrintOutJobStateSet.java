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

import java.io.IOException;
import java.time.Instant;

import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.dao.DaoContext;
import org.printflow.lite.core.dao.DocLogDao;
import org.printflow.lite.core.dao.PrintOutDao;
import org.printflow.lite.core.dto.AbstractDto;
import org.printflow.lite.core.ipp.IppJobStateEnum;
import org.printflow.lite.core.ipp.IppPrinterEventEnum;
import org.printflow.lite.core.jpa.DocLog;
import org.printflow.lite.core.jpa.PrintOut;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrintOutJobStateSet extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqPrintOutJobStateSet.class);

    /** */
    private static final DocLogDao DOCLOG_DAO =
            ServiceContext.getDaoContext().getDocLogDao();

    /** */
    private static final PrintOutDao PRINTOUT_DAO =
            ServiceContext.getDaoContext().getPrintOutDao();

    /**
     * The request.
     */
    private static final class DtoReq extends AbstractDto {

        /** */
        private Long docLogId;
        /** */
        private IppJobStateEnum jobState;

        public Long getDocLogId() {
            return docLogId;
        }

        @SuppressWarnings("unused")
        public void setDocLogId(final Long id) {
            this.docLogId = id;
        }

        public IppJobStateEnum getJobState() {
            return jobState;
        }

        @SuppressWarnings("unused")
        public void setJobState(final IppJobStateEnum state) {
            this.jobState = state;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final DocLog docLog = DOCLOG_DAO.findById(dtoReq.getDocLogId());

        final boolean isPrintOut = docLog != null && docLog.getDocOut() != null
                && docLog.getDocOut().getPrintOut() != null;

        if (!isPrintOut) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "No proxy print job found.");
            return;
        }

        final PrintOut printOut = docLog.getDocOut().getPrintOut();

        final IppJobStateEnum ippJobState =
                IppJobStateEnum.asEnum(printOut.getCupsJobState());

        if (ippJobState.isEndState()) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "Already reached end state.");
            return;
        }

        final DaoContext daoContext = ServiceContext.getDaoContext();

        if (!daoContext.isTransactionActive()) {
            daoContext.beginTransaction();
        }

        boolean rollback = false;

        try {

            final Instant instant = Instant.now();

            rollback = true;

            PRINTOUT_DAO.updateCupsJob(printOut.getId(), dtoReq.getJobState(),
                    Long.valueOf(instant.getEpochSecond()).intValue());

            daoContext.commit();
            rollback = false;

            final String msg = String.format("PrintOut Job %d %s: %s [%s]",
                    printOut.getCupsJobId().intValue(),
                    IppPrinterEventEnum.IPP_STATE_CHANGED.uiText(getLocale()),
                    dtoReq.getJobState().uiText(getLocale()), requestingUser);

            SpInfo.instance().log(msg);

            AdminPublisher.instance().publish(PubTopicEnum.PROXY_PRINT,
                    PubLevelEnum.INFO, msg);

            this.setApiResultText(ApiResultCodeEnum.OK, msg);

        } finally {
            if (rollback) {
                daoContext.rollback();
            }
        }
    }

}
