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
package org.printflow.lite.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.i18n.PrepositionEnum;
import org.printflow.lite.core.i18n.SystemModeEnum;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SystemModeChangeAddin extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public SystemModeChangeAddin(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("txt-mode", NounEnum.MODE);
        helper.addLabel("txt-from", PrepositionEnum.FROM_STATE);
        helper.addLabel("txt-to", PrepositionEnum.TO_STATE);

        final String[] cssColor = new String[] { MarkupHelper.CSS_TXT_WARN,
                MarkupHelper.CSS_TXT_VALID };
        final SystemModeEnum[] systemMode = new SystemModeEnum[] {
                SystemModeEnum.MAINTENANCE, SystemModeEnum.PRODUCTION };

        final int iCur;
        final int iChg;
        if (ConfigManager.getSystemMode() == SystemModeEnum.MAINTENANCE) {
            iCur = 0;
            iChg = 1;
        } else {
            iCur = 1;
            iChg = 0;
        }

        MarkupHelper.modifyLabelAttr(
                helper.addLabel("txt-current",
                        systemMode[iCur].uiText(getLocale())),
                MarkupHelper.ATTR_CLASS, cssColor[iCur]);

        MarkupHelper.modifyLabelAttr(
                helper.addLabel("txt-change",
                        systemMode[iChg].uiText(getLocale())),
                MarkupHelper.ATTR_CLASS, cssColor[iChg]);

        MarkupHelper.modifyLabelAttr(
                helper.addButton("btn-apply", HtmlButtonEnum.APPLY),
                MarkupHelper.ATTR_DATA_PrintFlowLite, systemMode[iChg].toString());
    }

}
