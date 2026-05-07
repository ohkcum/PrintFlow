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

import javax.mail.MessagingException;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.circuitbreaker.CircuitBreakerException;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.dao.DaoContext;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.EmailService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.email.EmailMsgParms;
import org.printflow.lite.core.totp.TOTPHelper;
import org.printflow.lite.core.totp.TOTPRecoveryDto;
import org.printflow.lite.lib.pgp.PGPBaseException;
import org.printflow.lite.server.session.SpSession;

/**
 * Sends the User's TOTP recovery code.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserTOTPSendRecoveryCode extends ApiRequestMixin {

    /** */
    protected static final EmailService EMAIL_SERVICE =
            ServiceContext.getServiceFactory().getEmailService();

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final User jpaUser =
                USER_DAO.findById(SpSession.get().getTOTPRequest().getDbKey());

        final String toAddress = USER_SERVICE.getPrimaryEmailAddress(jpaUser);
        if (toAddress == null) {
            this.setApiResult(ApiResultCodeEnum.WARN,
                    "msg-no-email-address-available");
            return;
        }

        final EmailMsgParms parms = new EmailMsgParms();
        parms.setToAddress(toAddress);

        final String subject = String.format("%s TOTP recovery code",
                CommunityDictEnum.PrintFlowLite.getWord());

        final TOTPRecoveryDto recoveryDto =
                TOTPHelper.generateRecoveryCode();

        parms.setSubject(subject);
        parms.setBodyInStationary(subject,
                "Your recovery code: ".concat(recoveryDto.getCode()),
                getLocale(), true);

        final DaoContext daoCtx = ServiceContext.getDaoContext();
        final boolean hasTrx = daoCtx.isTransactionActive();

        try {
            if (!hasTrx) {
                daoCtx.beginTransaction();
            }

            TOTPHelper.setRecoveryCodeDB(jpaUser, recoveryDto);

            EMAIL_SERVICE.sendEmail(parms);

            if (!hasTrx) {
                daoCtx.commit();
            }
        } catch (InterruptedException | CircuitBreakerException
                | MessagingException | IOException | PGPBaseException e) {
            throw new SpException(e.getMessage());
        } finally {
            if (!hasTrx) {
                daoCtx.rollback();
            }
        }

        this.setApiResultText(ApiResultCodeEnum.INFO,
                PhraseEnum.MAIL_SENT.uiText(getLocale()));
    }

}
