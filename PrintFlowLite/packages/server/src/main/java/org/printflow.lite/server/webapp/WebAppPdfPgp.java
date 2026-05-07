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
package org.printflow.lite.server.webapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.config.WebAppTypeEnum;
import org.printflow.lite.core.dao.enums.AppLogLevelEnum;
import org.printflow.lite.lib.pgp.PGPBaseException;
import org.printflow.lite.lib.pgp.PGPHelper;
import org.printflow.lite.lib.pgp.PGPSecretKeyInfo;
import org.printflow.lite.lib.pgp.pdf.PdfPgpVerifyUrl;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.LibreJsLicenseEnum;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.pages.MessageContent;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppPdfPgp extends AbstractWebAppPage {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppPdfPgp(final PageParameters parameters) {

        super(parameters);

        final String err;

        // Retrieve positional parameters.
        final String pgpMsgBody = parameters
                .get(PdfPgpVerifyUrl.URL_POSITION_PGP_MESSAGE).toString();

        if (ConfigManager.instance()
                .isConfigValue(Key.PDFPGP_VERIFICATION_ENABLE)
                && ConfigManager.instance()
                        .isConfigValue(Key.WEBAPP_PDFPGP_ENABLE)) {
            if (pgpMsgBody == null) {
                // err = "no verification data";
                err = null;
            } else {
                err = null;
            }
        } else {
            err = "Service is unsupported.";
        }

        if (err != null) {
            throw new RestartResponseException(
                    new MessageContent(AppLogLevelEnum.ERROR, err));
        }

        this.addOverlayBlockUserInput();

        final String appTitle = getWebAppTitle("PDF Verification");
        add(new Label("app-title", appTitle));

        final MarkupHelper helper = new MarkupHelper(this);

        // handlePgpMessage(helper, PdfPgpVerifyUrl.assemblePgpMsg(pgpMsgBody));
        // fileUploadMarkup();

        add(new FileUploadField("fileUpload"));

        helper.addTextInput("btn-reset", HtmlButtonEnum.RESET);
        helper.addTextInput("btn-verify", HtmlButtonEnum.VERIFY);
        //
        helper.addButton("btn-about", HtmlButtonEnum.ABOUT);
        this.addLibreJsLicensePanel("librjs-license-page");
    }

    /**
     * @param helper
     *            The mark-up helper.
     * @param pgpMessage
     *            PGP message.
     * @throws PGPBaseException
     *             When PGP error.
     */
    @SuppressWarnings("unused")
    private void handlePgpMessage(final MarkupHelper helper,
            final String pgpMessage) {

        final PGPSecretKeyInfo secKeyInfo =
                ConfigManager.instance().getPGPSecretKeyInfo();

        final InputStream istrEncrypted =
                new ByteArrayInputStream(pgpMessage.getBytes());

        final List<PGPPublicKey> signPublicKeyList = new ArrayList<>();
        signPublicKeyList.add(secKeyInfo.getPublicKey());

        final List<PGPSecretKeyInfo> secretKeyInfoList = new ArrayList<>();
        secretKeyInfoList.add(secKeyInfo);

        final OutputStream ostrClearContent = new ByteArrayOutputStream();

        final StringBuilder par = new StringBuilder();

        try {
            final PGPSignature sign = PGPHelper.instance()
                    .decryptOnePassSignature(istrEncrypted, signPublicKeyList,
                            secretKeyInfoList, ostrClearContent);

            par.append("Signed by ")
                    .append(secKeyInfo.getUids().get(0).toString());
            par.append("\nSigned on ").append(sign.getCreationTime())
                    .append(" (clock of signer's computer)");
            par.append("\nKey ID: ").append(secKeyInfo.formattedKeyID());
            par.append("\nKey fingerprint: ")
                    .append(secKeyInfo.formattedFingerPrint());

        } catch (PGPBaseException e) {
            par.append(e.getMessage());
        }

        add(new Label("payload-info", par.toString()));
    }

    @Override
    boolean isJqueryCoreRenderedByWicket() {
        return false;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.UNDEFINED;
    }

    @Override
    protected void appendWebAppTypeJsFiles(
            final List<Pair<String, LibreJsLicenseEnum>> list,
            final String nocache) {

        list.add(new ImmutablePair<>(
                String.format("%s%s", getSpecializedJsFileName(), nocache),
                PrintFlowLite_JS_LICENSE));
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.PrintFlowLite-pdfpgp.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.PrintFlowLite-pdfpgp.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        final EnumSet<JavaScriptLibrary> libs =
                EnumSet.noneOf(JavaScriptLibrary.class);
        return libs;
    }

}
