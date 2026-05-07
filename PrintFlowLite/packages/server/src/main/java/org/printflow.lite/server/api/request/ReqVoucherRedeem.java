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

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.dao.enums.AppLogLevelEnum;
import org.printflow.lite.core.dto.AbstractDto;
import org.printflow.lite.core.dto.AccountVoucherRedeemDto;
import org.printflow.lite.core.jpa.AccountVoucher;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.AccountingService;
import org.printflow.lite.core.util.Messages;

/**
 * Redeems an {@link AccountVoucher}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqVoucherRedeem extends ApiRequestMixin {

    /** */
    private static class DtoReq extends AbstractDto {

        /** */
        private String cardNumber;

        public String getCardNumber() {
            return cardNumber;
        }

        @SuppressWarnings("unused")
        public void setCardNumber(final String cardNumber) {
            this.cardNumber = cardNumber;
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws Exception {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final String cardNumber = dtoReq.getCardNumber();

        /*
         * A blank card number is not considered a trial.
         */
        if (StringUtils.isBlank(cardNumber)) {
            this.setApiResult(ApiResultCodeEnum.WARN,
                    ReqMessageEnum.VOUCHER_USER_REDEEM_VOID);
            return;
        }

        final AccountVoucherRedeemDto dto = new AccountVoucherRedeemDto();

        dto.setCardNumber(cardNumber);
        dto.setRedeemDate(System.currentTimeMillis());
        dto.setUserId(requestingUser);

        final AccountingService.VoucherRedeemEnum redeemRsp =
                ACCOUNTING_SERVICE.redeemVoucher(dto);

        final ReqMessageEnum msgEnumLog;
        final PubLevelEnum pubLevel;

        switch (redeemRsp) {
        case OK:
            msgEnumLog = ReqMessageEnum.VOUCHER_REDEEM_OK;
            pubLevel = PubLevelEnum.INFO;
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_USER_REDEEM_OK);
            break;
        case INVALID:
            msgEnumLog = ReqMessageEnum.VOUCHER_REDEEM_INVALID;
            pubLevel = PubLevelEnum.WARN;
            this.setApiResult(ApiResultCodeEnum.ERROR,
                    ReqMessageEnum.VOUCHER_USER_REDEEM_NUMBER_INVALID);
            break;
        case USER_UNKNOWN:
            msgEnumLog = ReqMessageEnum.VOUCHER_REDEEM_USER_UNKNOWN;
            pubLevel = PubLevelEnum.WARN;
            this.setApiResult(ApiResultCodeEnum.ERROR,
                    ReqMessageEnum.VOUCHER_USER_REDEEM_USER_UNKNOWN.key(),
                    requestingUser);
            break;
        default:
            throw new SpException(String.format("%s [%s] not handled",
                    redeemRsp.getClass().getSimpleName(), redeemRsp));
        }

        final String msgLog = Messages.getMessage(getClass(), getLocale(),
                msgEnumLog.key(), requestingUser, cardNumber);

        if (pubLevel != PubLevelEnum.INFO) {
            APPLOG_SERVICE.logMessage(AppLogLevelEnum.WARN, msgLog);
        }
        AdminPublisher.instance().publish(PubTopicEnum.USER, pubLevel, msgLog);

    }

}
