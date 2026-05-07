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
package org.printflow.lite.server.api.request;

import java.io.IOException;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.i18n.AdverbEnum;
import org.printflow.lite.core.job.SpJobScheduler;
import org.printflow.lite.core.job.SpJobType;
import org.printflow.lite.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqAtomFeedRefresh extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        try {

            if (!ConfigManager.instance()
                    .isConfigValue(Key.FEED_ATOM_ADMIN_ENABLE)) {
                throw new SpException("Atom Feed - "
                        + AdverbEnum.DISABLED.uiText(getLocale()));
            }

            SpJobScheduler.instance().scheduleOneShotJob(SpJobType.ATOM_FEED,
                    1);

            setApiResult(ApiResultCodeEnum.OK, "msg-refresh-ok");

        } catch (Exception e) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-single-parm",
                    e.getMessage());
        }
    }

}
