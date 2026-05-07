/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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

import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.services.RateLimiterService.LimitEnum;
import org.printflow.lite.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserRegistrationPermit extends ApiRequestMixin {

    /** */
    private static final RateLimiterService RATE_LIMITER_SERVICE =
            ServiceContext.getServiceFactory().getRateLimiterService();

    /** */
    private static final LimitEnum LIMIT_ENUM = LimitEnum.USER_REG_BY_IP;

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        if (ConfigManager.isUserRegRateLimitingEnabled()) {

            final RateLimiterService.IPEvent event =
                    new RateLimiterService.IPEvent(this.getClientIP(),
                            "User Registration");

            if (!RATE_LIMITER_SERVICE.consumeEvent(LIMIT_ENUM, event)) {

                AdminPublisher
                        .publish(
                                event, PubTopicEnum.USER, RATE_LIMITER_SERVICE
                                        .waitTimeForEvent(LIMIT_ENUM, event),
                                false);

                this.setApiResult(ApiResultCodeEnum.INFO,
                        "msg-user-reg-not-available");

                return;
            }
        }
        this.setApiResultOk();
    }
}
