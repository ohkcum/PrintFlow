/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server.api.request;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.dao.enums.AppLogLevelEnum;
import org.printflow.lite.core.dto.AbstractDto;
import org.printflow.lite.core.jpa.AccountVoucher;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.ServiceContext;

/**
 * Expires all non-redeemed {@link AccountVoucher} instances in a batch by
 * setting the expiry date to today 00:00.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqVoucherBatchExpire extends ApiRequestMixin {

    /** */
    private static class DtoReq extends AbstractDto {

        /** */
        private String batch;

        public String getBatch() {
            return batch;
        }

        @SuppressWarnings("unused")
        public void setBatch(final String batch) {
            this.batch = batch;
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws Exception {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final String batch = dtoReq.getBatch();

        final Date expiryToday =
                DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        final Integer nExpired = ServiceContext.getDaoContext()
                .getAccountVoucherDao().expireBatch(batch, expiryToday);

        if (nExpired == 0) {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_BATCH_EXPIRED_ZERO.key(), batch);
        } else if (nExpired == 1) {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_BATCH_EXPIRED_ONE.key(),
                    requestingUser, batch);
        } else {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_BATCH_EXPIRED_MANY.key(),
                    requestingUser, nExpired.toString(), batch);
        }

        if (nExpired > 0) {
            final String msgLog = this.getApiResultText();
            APPLOG_SERVICE.logMessage(AppLogLevelEnum.INFO, msgLog);
            AdminPublisher.instance().publish(PubTopicEnum.SYSTEM,
                    PubLevelEnum.INFO, msgLog);
        }
    }

}
