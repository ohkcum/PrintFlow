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

import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.dao.enums.AppLogLevelEnum;
import org.printflow.lite.core.dto.AccountVoucherBatchDto;
import org.printflow.lite.core.jpa.AccountVoucher;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.json.JsonAbstractBase;
import org.printflow.lite.core.json.rpc.AbstractJsonRpcMethodResponse;

/**
 * Creates a batch of {@link AccountVoucher} instances.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqVoucherBatchCreate extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws Exception {

        final AccountVoucherBatchDto dto = JsonAbstractBase
                .create(AccountVoucherBatchDto.class, this.getParmValueDto());

        final AbstractJsonRpcMethodResponse rpcResponse =
                ACCOUNT_VOUCHER_SERVICE.createBatch(dto);

        final AppLogLevelEnum appLogLevel;
        final PubLevelEnum pubLevel;

        if (rpcResponse.isResult()) {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_BATCH_CREATED_OK.key(),
                    requestingUser, dto.getNumber().toString(),
                    dto.getBatchId());
            appLogLevel = AppLogLevelEnum.INFO;
            pubLevel = PubLevelEnum.INFO;
        } else {
            this.setApiResultText(rpcResponse);
            appLogLevel = AppLogLevelEnum.ERROR;
            pubLevel = PubLevelEnum.ERROR;
        }

        final String msgLog = this.getApiResultText();
        APPLOG_SERVICE.logMessage(appLogLevel, msgLog);
        AdminPublisher.instance().publish(PubTopicEnum.USER, pubLevel, msgLog);
    }

}
