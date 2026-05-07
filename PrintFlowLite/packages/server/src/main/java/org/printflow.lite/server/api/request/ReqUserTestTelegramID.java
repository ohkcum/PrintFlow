/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
import java.util.Objects;

import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.community.MemberCard;
import org.printflow.lite.core.dao.enums.UserAttrEnum;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.ext.telegram.TelegramHelper;

/**
 * Sets User Telegram ID.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserTestTelegramID extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        if (!TelegramHelper.isMessagingEnabled()) {
            this.setApiResultText(ApiResultCodeEnum.ERROR,
                    "Telegram Messaging is disabled.");
            return;
        }

        final User jpaUser;
        if (lockedUser == null) {
            jpaUser = USER_DAO.findById(this.getSessionUserDbKey());
        } else {
            jpaUser = lockedUser;
        }

        final String telegramID = USER_SERVICE.getUserAttrValue(jpaUser,
                UserAttrEnum.EXT_TELEGRAM_ID);

        if (telegramID == null) {
            this.setApiResultText(ApiResultCodeEnum.ERROR, "No Telegram ID.");
            return;
        }

        if (TelegramHelper.sendMessage(telegramID, String.format("%s : %s [%s]",
                CommunityDictEnum.PrintFlowLite.getWord(),
                Objects.toString(MemberCard.instance().getMemberOrganization(),
                        "-"),
                MemberCard.instance().getStatusUserText(getLocale())))) {
            this.setApiResult(ApiResultCodeEnum.OK, "msg-apply-ok");
        } else {
            this.setApiResult(ApiResultCodeEnum.ERROR, "msg-action-failed");
        }

    }

}
