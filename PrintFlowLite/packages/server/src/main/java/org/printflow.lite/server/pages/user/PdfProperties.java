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
package org.printflow.lite.server.pages.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.dao.enums.ACLOidEnum;
import org.printflow.lite.core.dao.enums.ACLPermissionEnum;
import org.printflow.lite.core.dto.UserIdDto;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.i18n.PrintOutNounEnum;
import org.printflow.lite.core.pdf.PdfColorEnum;
import org.printflow.lite.core.pdf.PdfLineWidthEnum;
import org.printflow.lite.core.services.AccessControlService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.pages.PasswordPanel;
import org.printflow.lite.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PdfProperties extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();
    /** */
    private static final Integer MAX_PDF_PASSWORD_LENGTH = 32;
    /** */
    private static final String PFL_CSS_CLASS_PDF_APPLY_SOURCE =
            "sp-pdf-apply-src";

    /**
     *
     * @param parameters
     *            The parms.
     */
    public PdfProperties(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.encloseLabel("pdf-ecoprint", "",
                ConfigManager.isEcoPrintEnabled());

        helper.addButton("pdf-rasterize", HtmlButtonEnum.RASTERIZE);
        //
        helper.addLabel("pdf-uri-links",
                NounEnum.LINK.uiText(getLocale(), true));
        helper.addTransparentModifyAttr("pdf-url-link-select-group-line",
                MarkupHelper.ATTR_LABEL, NounEnum.LINE.uiText(getLocale()));
        helper.addTransparentModifyAttr("pdf-url-link-select-group-color",
                MarkupHelper.ATTR_LABEL,
                PrintOutNounEnum.COLOR.uiText(getLocale()));

        this.addLinkWidths();
        this.addLinkColors();

        this.addPasswordPanels();

        //
        final UserIdDto user = SpSession.get().getUserIdDto();

        final List<ACLPermissionEnum> permissions =
                ACCESS_CONTROL_SERVICE.getPermission(user, ACLOidEnum.U_INBOX);

        helper.encloseLabel("button-pdf-download",
                HtmlButtonEnum.DOWNLOAD.uiText(getLocale()),
                permissions == null || ACCESS_CONTROL_SERVICE.hasPermission(
                        permissions, ACLPermissionEnum.DOWNLOAD));

        helper.encloseLabel("button-pdf-send",
                HtmlButtonEnum.SEND.uiText(getLocale()),
                permissions == null || ACCESS_CONTROL_SERVICE
                        .hasPermission(permissions, ACLPermissionEnum.SEND));

        helper.addButton("btn-back", HtmlButtonEnum.BACK);
        helper.addButton("btn-default", HtmlButtonEnum.DEFAULT);

        //
        helper.encloseLabel("pdf-pgp-signature",
                ACLPermissionEnum.SIGN.uiText(getLocale()),
                ConfigManager.isPdfPgpAvailable() && (permissions == null
                        || ACCESS_CONTROL_SERVICE.hasPermission(permissions,
                                ACLPermissionEnum.SIGN)));
        //
        final Integer privsLetterhead = ACCESS_CONTROL_SERVICE
                .getPrivileges(user, ACLOidEnum.U_LETTERHEAD);

        helper.encloseLabel("prompt-letterhead", localized("prompt-letterhead"),
                privsLetterhead == null || ACLPermissionEnum.READER
                        .isPresent(privsLetterhead.intValue()));
    }

    /** */
    private void addPasswordPanels() {

        final Map<String, String> mainPasswordMap = new HashedMap<>();
        mainPasswordMap.put(MarkupHelper.ATTR_CLASS,
                PFL_CSS_CLASS_PDF_APPLY_SOURCE);

        for (final String wid : new String[] { //
                "pdf-pw-user", "pdf-pw-owner" }) {
            this.add(PasswordPanel.createPopulate(wid + "-panel", wid,
                    MAX_PDF_PASSWORD_LENGTH, mainPasswordMap));
        }

        for (final String wid : new String[] { //
                "pdf-pw-user-c", "pdf-pw-owner-c" }) {
            this.add(PasswordPanel.createPopulate(wid + "-panel", wid,
                    MAX_PDF_PASSWORD_LENGTH));
        }
    }

    /**
     * Adds Link width.
     */
    private void addLinkWidths() {

        final List<PdfLineWidthEnum> widthList = new ArrayList<>();

        for (final PdfLineWidthEnum width : PdfLineWidthEnum.values()) {
            widthList.add(width);
        }

        add(new PropertyListView<PdfLineWidthEnum>(
                "pdf-links-width-option-select", widthList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PdfLineWidthEnum> item) {
                final PdfLineWidthEnum dto = item.getModel().getObject();
                final Label label =
                        new Label("option", dto.uiText(getLocale()));
                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.name());
                item.add(label);
            }
        });
    }

    /**
     * Adds Link colors.
     */
    private void addLinkColors() {

        final List<PdfColorEnum> locations = new ArrayList<>();

        for (final PdfColorEnum location : PdfColorEnum.values()) {
            locations.add(location);
        }

        add(new PropertyListView<PdfColorEnum>("pdf-links-color-option-select",
                locations) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PdfColorEnum> item) {
                final PdfColorEnum dto = item.getModel().getObject();
                final Label label =
                        new Label("option", dto.uiText(getLocale()));
                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.name());
                item.add(label);
            }
        });
    }

}
