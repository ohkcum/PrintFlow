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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AccountTrx extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public AccountTrx(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.encloseLabel("btn-txt-gdpr", "GDPR", ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_USER_GDPR_ENABLE));

        helper.addLabel("title",
                NounEnum.TRANSACTION.uiText(getLocale(), true));

        helper.addButton("button-back", HtmlButtonEnum.BACK);
        helper.addLabel("button-documents",
                NounEnum.DOCUMENT.uiText(getLocale(), true));
    }

}
