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
import org.printflow.lite.core.jpa.AccountVoucher;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.ServiceContext;

/**
 * Deletes {@link AccountVoucher} instances whose expiry date is before the
 * current date (today).
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqVoucherDeleteExpired extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws Exception {

        final Date expiryToday =
                DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        final Integer nDeleted = ServiceContext.getDaoContext()
                .getAccountVoucherDao().deleteExpired(expiryToday);

        if (nDeleted == 0) {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_DELETED_EXPIRED_ZERO);
        } else if (nDeleted == 1) {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_DELETED_EXPIRED_ONE.key(),
                    requestingUser);
        } else {
            this.setApiResult(ApiResultCodeEnum.OK,
                    ReqMessageEnum.VOUCHER_DELETED_EXPIRED_MANY.key(),
                    requestingUser, nDeleted.toString());
        }

        if (nDeleted > 0) {
            final String msgLog = this.getApiResultText();
            APPLOG_SERVICE.logMessage(AppLogLevelEnum.INFO, msgLog);
            AdminPublisher.instance().publish(PubTopicEnum.SYSTEM,
                    PubLevelEnum.INFO, msgLog);
        }
    }

}
