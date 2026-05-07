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
package org.printflow.lite.server.pages.printsite;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.enums.ACLOidEnum;
import org.printflow.lite.core.i18n.AdjectiveEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.services.helpers.account.UserAccountContextFactory;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.CommunityStatusFooterPanel;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class Main extends AbstractPrintSitePage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public Main(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("txt-activate-reader",
                PhraseEnum.ACTIVATE_CARD_READER.uiText(getLocale()));

        helper.addLabel("txt-swipe-card",
                PhraseEnum.SWIPE_CARD.uiText(getLocale()));

        MarkupHelper.modifyLabelAttr(helper.addLabel("search-userid", ""),
                MarkupHelper.ATTR_PLACEHOLDER,
                NounEnum.USER.uiText(getLocale()).toLowerCase());

        helper.addLabel("btn-dashboard",
                ACLOidEnum.A_DASHBOARD.uiText(getLocale()));

        helper.encloseLabel("btn-register",
                HtmlButtonEnum.REGISTER.uiText(getLocale()), ConfigManager
                        .instance().isConfigValue(Key.INTERNAL_USERS_ENABLE));

        helper.addLabel("btn-details", NounEnum.USER);

        helper.addLabel("btn-pending-jobs",
                AdjectiveEnum.PENDING.uiText(getLocale()));

        helper.addLabel("btn-hold-jobs",
                NounEnum.DOCUMENT.uiText(getLocale(), true));

        final boolean isUserAccountPaperCut =
                UserAccountContextFactory.hasContextPaperCut();

        helper.encloseLabel("btn-transactions-sp",
                NounEnum.TRANSACTION.uiText(getLocale(), true),
                !isUserAccountPaperCut);

        helper.encloseLabel("btn-transactions-pc",
                NounEnum.TRANSACTION.uiText(getLocale(), true),
                isUserAccountPaperCut);

        helper.addButton("btn-logout", HtmlButtonEnum.LOGOUT);

        helper.addButton("header-user-delete", HtmlButtonEnum.DELETE);
        helper.addLabel("warn-user-delete", PhraseEnum.USER_DELETE_WARNING);
        helper.addButton("btn-user-delete-yes", HtmlButtonEnum.YES);
        helper.addButton("btn-user-delete-no", HtmlButtonEnum.NO);

        add(new CommunityStatusFooterPanel("community-status-footer-panel",
                true));
    }

}
