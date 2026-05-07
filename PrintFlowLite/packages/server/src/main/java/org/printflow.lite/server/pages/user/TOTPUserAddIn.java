/*
 * This file is part of the PrintFlowLite project <https://PrintFlowLite.org>.
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
package org.printflow.lite.server.pages.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.community.MemberCard;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.crypto.CryptoUser;
import org.printflow.lite.core.dao.DaoContext;
import org.printflow.lite.core.dao.UserDao;
import org.printflow.lite.core.dao.enums.UserAttrEnum;
import org.printflow.lite.core.i18n.AdverbEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.UserService;
import org.printflow.lite.core.util.QRCodeException;
import org.printflow.lite.core.util.QRCodeHelper;
import org.printflow.lite.ext.telegram.TelegramHelper;
import org.printflow.lite.lib.totp.TOTPAuthenticator;
import org.printflow.lite.server.pages.AbstractAuthPage;
import org.printflow.lite.server.pages.CopyToClipBoardPanel;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.pages.TOTPInputPanel;
import org.printflow.lite.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TOTPUserAddIn extends AbstractAuthPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final String WID_TOTP_ENABLE = "totp-enable";
    /** */
    private static final String WID_TOTP_ENABLE_TXT = "totp-enable-txt";
    /** */
    private static final String WID_TOTP_VERIFICATION_LABEL =
            "totp-verification-label";
    /** */
    private static final String WID_TOTP_TELEGRAM_ENABLE_SPAN =
            "totp-telegram-enable-span";
    /** */
    private static final String WID_TOTP_TELEGRAM_ENABLE =
            "totp-telegram-enable";

    /** */
    private static final String WID_QR_CODE_SPAN = "qr-code-span";
    /** */
    private static final String WID_QR_CODE = "qr-code";
    /** */
    private static final String WID_QR_CODE_URI = "qr-code-uri";

    /** */
    private static final String WID_TOTP_SECRET = "TOTP-secret";
    /** */
    private static final String WID_TOTP_SECRET_PANEL = "TOTP-secret-panel";

    /** */
    private static final String WID_TOTP_INPUT_PANEL = "totp-input-panel";
    /** */
    private static final String WID_TOTP_INPUT_HTML_ID =
            "sp-user-totp-enable-verify-code";

    /** */
    private static final int QR_CODE_WIDTH = 200;

    /**
     * @param parms
     *            Parameters.
     */
    public TOTPUserAddIn(final PageParameters parms) {

        super(parms);

        final MarkupHelper helper = new MarkupHelper(this);

        final org.printflow.lite.core.jpa.User jpaUser =
                USER_DAO.findById(SpSession.get().getUserDbKey());

        String secretKey = USER_SERVICE.getUserAttrValue(jpaUser,
                UserAttrEnum.TOTP_SECRET);

        final TOTPAuthenticator.Builder builder;
        final boolean totpEnabled;
        if (secretKey == null) {
            builder = TOTPAuthenticator.buildKey();
            secretKey = builder.getKey();

            final DaoContext ctx = ServiceContext.getDaoContext();
            final boolean doTransaction = !ctx.isTransactionActive();
            try {
                if (doTransaction) {
                    ctx.beginTransaction();
                }
                totpEnabled = false;
                USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.TOTP_ENABLE,
                        totpEnabled);
                USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.TOTP_SECRET,
                        CryptoUser.encryptUserAttr(jpaUser.getId(), secretKey));
                if (doTransaction) {
                    ctx.commit();
                }
            } finally {
                if (doTransaction) {
                    ctx.rollback();
                }
            }
        } else {
            totpEnabled = USER_SERVICE.isUserAttrValue(jpaUser,
                    UserAttrEnum.TOTP_ENABLE);
            builder = new TOTPAuthenticator.Builder(secretKey);
        }

        final TOTPAuthenticator auth = builder.build();

        final ConfigManager cm = ConfigManager.instance();

        String issuer = cm.getConfigValue(Key.USER_TOTP_ISSUER);
        if (StringUtils.isBlank(issuer)) {
            issuer = MemberCard.instance().getMemberOrganization();
        }
        if (StringUtils.isBlank(issuer)) {
            issuer = CommunityDictEnum.PrintFlowLite.getWord();
        }

        final String uri = auth.getURI(issuer, jpaUser.getUserId());

        MarkupHelper.modifyComponentAttr(
                MarkupHelper.modifyComponentAttr(
                        helper.addTransparant(WID_QR_CODE_URI),
                        MarkupHelper.ATTR_HREF, uri),
                MarkupHelper.ATTR_TITLE, StringUtils.abbreviate(uri, 32));

        CopyToClipBoardPanel.addLabelAndClipBoardCopy(this, WID_TOTP_SECRET,
                WID_TOTP_SECRET_PANEL, secretKey);

        try {
            helper.addModifyImagePngBase64Attr(WID_QR_CODE,
                    QRCodeHelper.createImagePngBase64(uri, QR_CODE_WIDTH));
        } catch (QRCodeException e) {
            throw new IllegalArgumentException(e);
        }

        helper.addLabel(WID_TOTP_ENABLE_TXT, AdverbEnum.ENABLED);
        helper.addCheckbox(WID_TOTP_ENABLE, totpEnabled);

        helper.addLabel(WID_TOTP_VERIFICATION_LABEL,
                NounEnum.VERIFICATION.uiText(getLocale()));

        this.add(new TOTPInputPanel(WID_TOTP_INPUT_PANEL,
                WID_TOTP_INPUT_HTML_ID, TOTPInputPanel.ToolTip.AUTH_APP));

        //
        final boolean telegram = TelegramHelper.isTOTPEnabled()
                && StringUtils.isNotBlank(USER_SERVICE.getUserAttrValue(jpaUser,
                        UserAttrEnum.EXT_TELEGRAM_ID));

        final boolean telegramEnabled;

        if (telegram) {
            telegramEnabled = USER_SERVICE.isUserAttrValue(jpaUser,
                    UserAttrEnum.EXT_TELEGRAM_TOTP_ENABLE);

            helper.addCheckbox(WID_TOTP_TELEGRAM_ENABLE, telegramEnabled);

            helper.addTransparantInvisible(WID_TOTP_TELEGRAM_ENABLE_SPAN,
                    !totpEnabled);

        } else {
            telegramEnabled = false;
            helper.discloseLabel(WID_TOTP_TELEGRAM_ENABLE);
        }

        //
        helper.addTransparantInvisible(WID_QR_CODE_SPAN,
                !totpEnabled || telegramEnabled);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
