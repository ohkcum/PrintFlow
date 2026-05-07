/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: {c} 2026 Datraverse B.V. <info@datraverse.com>
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

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.dao.enums.UserAttrEnum;
import org.printflow.lite.core.dto.AbstractDto;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.totp.TOTPHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserTOTPSet extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private boolean enabled;
        private String totp;

        public boolean isEnabled() {
            return enabled;
        }

        @SuppressWarnings("unused")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTotp() {
            return totp;
        }

        @SuppressWarnings("unused")
        public void setTotp(String totp) {
            this.totp = totp;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        // INVARIANT: TOTP code must be present.
        if (dtoReq.isEnabled() && StringUtils.isBlank(dtoReq.getTotp())) {
            this.setApiResult(ApiResultCodeEnum.WARN,
                    "msg-value-cannot-be-empty", "TOTP");
            return;
        }

        final User jpaUser;
        if (lockedUser == null) {
            jpaUser = USER_DAO.findById(this.getSessionUserDbKey());
        } else {
            jpaUser = lockedUser;
        }

        // INVARIANT: TOTP code must be valid.
        if (dtoReq.isEnabled()
                && !TOTPHelper.verifyCode(jpaUser, dtoReq.getTotp())) {
            this.setApiResult(ApiResultCodeEnum.WARN, "msg-invalid-input",
                    "TOTP");
            return;
        }

        USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.TOTP_ENABLE,
                dtoReq.isEnabled());

        this.setApiResult(ApiResultCodeEnum.OK, "msg-apply-ok");
    }

}
