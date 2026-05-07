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
package org.printflow.lite.server.xmlrpc;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.crypto.OneTimeAuthToken;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.services.RateLimiterService.EndlessWaitException;
import org.printflow.lite.core.services.RateLimiterService.IEvent;
import org.printflow.lite.core.services.RateLimiterService.LimitEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.IRateLimiterListener;
import org.printflow.lite.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class OneTimeAuthHandler
        implements IXmlRpcHandler, IRateLimiterListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(OneTimeAuthHandler.class);

    /** */
    public static final String SHORT_NAME = "XML-RPC";

    /** */
    private static final RateLimiterService RATE_LIMITER_SERVICE =
            ServiceContext.getServiceFactory().getRateLimiterService();

    /**
     * @param ttpKey
     *            The Trusted Third-Party key.
     * @param userid
     *            The user id.
     * @return The one-time user authentication token or {@code null} when
     *         access is denied (because TTP Web Login is disabled or TTP Key is
     *         invalid).
     */
    public String createToken(final String ttpKey, final String userid) {

        final String clientIpAddress = SpXmlRpcServlet.getClientIpAddress();
        final ConfigManager cm = ConfigManager.instance();
        final AdminPublisher adminPub = AdminPublisher.instance();

        //
        if (!cm.isConfigValue(Key.WEB_LOGIN_TTP_ENABLE)) {

            final String msgKey = "weblogin-ttp-disabled";

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(Messages.getLogFileMessage(this.getClass(), msgKey,
                        userid, clientIpAddress));
            }
            adminPub.publish(PubTopicEnum.USER, PubLevelEnum.WARN,
                    Messages.getSystemMessage(this.getClass(), msgKey, userid,
                            clientIpAddress));

            this.onRateLimiting(clientIpAddress);
            return null;
        }

        //
        final String ttpKeyConfig =
                cm.getConfigValue(Key.WEB_LOGIN_TTP_API_KEY);

        if (StringUtils.isBlank(ttpKeyConfig)) {

            final String msgKey = "weblogin-ttp-key-missing";

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(Messages.getLogFileMessage(this.getClass(), msgKey,
                        userid, clientIpAddress));
            }
            adminPub.publish(PubTopicEnum.USER, PubLevelEnum.WARN,
                    Messages.getSystemMessage(this.getClass(), msgKey, userid,
                            clientIpAddress));

            this.onRateLimiting(clientIpAddress);
            return null;
        }
        //
        if (!ttpKeyConfig.equals(ttpKey)) {
            final String msgKey = "weblogin-ttp-key-mismatch";

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(Messages.getLogFileMessage(this.getClass(), msgKey,
                        userid, clientIpAddress));
            }
            adminPub.publish(PubTopicEnum.USER, PubLevelEnum.WARN,
                    Messages.getSystemMessage(this.getClass(), msgKey, userid,
                            clientIpAddress));

            this.onRateLimiting(clientIpAddress);
            return null;
        }

        //
        final String token = OneTimeAuthToken.createToken(userid);

        //
        final String msgKey = "weblogin-ttp-token-created";

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(Messages.getLogFileMessage(this.getClass(), msgKey,
                    userid, clientIpAddress));
        }

        adminPub.publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                Messages.getSystemMessage(this.getClass(), msgKey, userid,
                        clientIpAddress));
        //
        return token;
    }

    /**
     * @param clientIPAddress
     *            Client IP address.
     */
    private void onRateLimiting(final String clientIPAddress) {

        if (ConfigManager.isAPIRateLimitingEnabled()) {
            try {
                RATE_LIMITER_SERVICE.consumeOrWaitForEvent(
                        LimitEnum.API_FAILURE_BY_IP,
                        new RateLimiterService.IPEvent(clientIPAddress,
                                SHORT_NAME),
                        this);
            } catch (EndlessWaitException e) {
                // no code intended
            }
        }
    }

    @Override
    public void onRateLimited(final IEvent event, final long waitMsec) {
        AdminPublisher.publish(event, PubTopicEnum.WEB_SERVICE, waitMsec);
    }

}
