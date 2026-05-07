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
import org.printflow.lite.core.dao.enums.ACLOidEnum;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.pages.PasswordPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageConfigProp extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final int MAX_INPUT_LENGTH = 512;

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public PageConfigProp(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_CONFIG_EDITOR, RequiredPermission.EDIT);

        final MarkupHelper helper = new MarkupHelper(this);

        MarkupHelper.modifyLabelAttr(helper.addModifyLabelAttr("input-text", "",
                MarkupHelper.ATTR_MAXLENGTH, String.valueOf(MAX_INPUT_LENGTH)),
                MarkupHelper.ATTR_ID, "config-prop-value");

        helper.addModifyLabelAttr("input-textarea", "", MarkupHelper.ATTR_ID,
                "config-prop-value-multiline");

        this.add(PasswordPanel.createPopulate("password-panel",
                "config-prop-value-password", MAX_INPUT_LENGTH));

        helper.addButton("btn-ok", HtmlButtonEnum.OK);
        helper.addButton("btn-cancel", HtmlButtonEnum.CANCEL);
    }
}
