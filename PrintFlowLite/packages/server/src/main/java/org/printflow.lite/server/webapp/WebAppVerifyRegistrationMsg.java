/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.WebAppTypeEnum;
import org.printflow.lite.core.i18n.AdjectiveEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.server.WebAppParmEnum;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppVerifyRegistrationMsg extends AbstractWebAppMsg {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    public static final WebAppParmEnum PARM_STATUS = WebAppParmEnum.PFL_PARM_1;

    /** */
    public static final String PARM_STATUS_VERIFIED = "v";

    /** */
    public static final String PARM_STATUS_FAIL = "f";

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppVerifyRegistrationMsg(final PageParameters parameters) {
        super(parameters);
    }

    /**
     * @param adj
     * @return lower case
     */
    private String lowerCase(final AdjectiveEnum adj) {
        return adj.uiText(getLocale()).toLowerCase();
    }

    @Override
    protected WebAppTypeEnum getDisplayInfo(final PageParameters parameters,
            final WebAppTypeEnum webAppTypeAuth,
            final WebAppTypeEnum webAppTypeRequested,
            final MutableObject<String> messageObj,
            final MutableObject<String> messageCssObj,
            final MutableObject<String> remedyObj) {

        final String status = parameters.get(PARM_STATUS.parm()).toString();

        final WebAppTypeEnum webAppTypeLogin;
        if (status.equals(PARM_STATUS_VERIFIED)) {
            messageCssObj.setValue(MarkupHelper.CSS_TXT_INFO);
            messageObj.setValue(NounEnum.REGISTRATION.uiText(getLocale()) + " "
                    + this.lowerCase(AdjectiveEnum.COMPLETED));
            webAppTypeLogin = webAppTypeRequested;
            remedyObj
                    .setValue(PhraseEnum.LOGIN_TO_CONTINUE.uiText(getLocale()));
        } else {
            messageCssObj.setValue(MarkupHelper.CSS_TXT_WARN);
            messageObj.setValue(NounEnum.REGISTRATION.uiText(getLocale(), true)
                    + " " + this.lowerCase(AdjectiveEnum.FAILED));
            webAppTypeLogin = null;
            remedyObj.setValue(NounEnum.REASON.uiText(getLocale()) + ": "
                    + this.lowerCase(AdjectiveEnum.INVALID) + ", "
                    + this.lowerCase(AdjectiveEnum.EXPIRED) + "/"
                    + this.lowerCase(AdjectiveEnum.COMPLETED));
        }

        return webAppTypeLogin;
    }

}
