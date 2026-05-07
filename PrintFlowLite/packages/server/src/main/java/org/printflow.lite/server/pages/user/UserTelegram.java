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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.server.pages.user;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.enums.UserAttrEnum;
import org.printflow.lite.core.dto.UserIdDto;
import org.printflow.lite.core.i18n.LabelEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.UserService;
import org.printflow.lite.core.totp.TOTPHelper;
import org.printflow.lite.ext.telegram.TelegramHelper;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class UserTelegram extends AbstractUserPage {

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            Page parameters.
     */
    public UserTelegram(final PageParameters parameters) {
        super(parameters);

        final UserIdDto authUser = SpSession.get().getUserIdDto();

        final org.printflow.lite.core.jpa.User jpaUser = ServiceContext
                .getDaoContext().getUserDao().findById(authUser.getDbKey());

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("header", "Telegram Configuration");
        helper.addModifyLabelAttr("telegram-id", MarkupHelper.ATTR_VALUE,
                USER_SERVICE.getUserAttrValue(jpaUser,
                        UserAttrEnum.EXT_TELEGRAM_ID));

        helper.addLabel("step-1", helper.localized("step-1", TelegramHelper
                .userNameFormatted(TelegramHelper.MY_ID_BOT_USERNAME)));
        helper.addLabel("step-3", helper.localized("step-3",
                TelegramHelper.userNameFormatted(ConfigManager.instance()
                        .getConfigValue(Key.EXT_TELEGRAM_BOT_USERNAME))));

        helper.addButton("btn-telegram-apply", HtmlButtonEnum.APPLY);
        helper.addButton("btn-telegram-test", HtmlButtonEnum.SEND);
        helper.addButton("btn-telegram-back", HtmlButtonEnum.BACK);
        helper.encloseLabel("btn-telegram-2-step",
                LabelEnum.TWO_STEP_VERIFICATION.uiText(getLocale()),
                TOTPHelper.isTOTPEnabled());
    }

}
