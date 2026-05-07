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
package org.printflow.lite.server.feed;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.services.AtomFeedService;
import org.printflow.lite.core.services.RateLimiterService.EndlessWaitException;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.lib.feed.AtomFeedWriter;
import org.printflow.lite.lib.feed.FeedException;
import org.printflow.lite.server.BasicAuthServlet;

/**
 * Atom Feed.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "AtomFeedServlet",
        urlPatterns = { AtomFeedServlet.SERVLET_URL_PATTERN })
@ServletSecurity(@HttpConstraint(
        transportGuarantee = TransportGuarantee.CONFIDENTIAL,
        rolesAllowed = { AtomFeedServlet.ROLE_ALLOWED }))
public final class AtomFeedServlet extends BasicAuthServlet {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * Role for Basic Authentication.
     */
    public static final String ROLE_ALLOWED = "FeedReader";

    /**
     * Base path of custom web files (without leading or trailing '/').
     */
    public static final String PATH_BASE = "feed";

    /**
     * Path info of admin feed (with leading '/').
     */
    public static final String PATH_INFO_ADMIN = "/admin";

    /** */
    public static final String SERVLET_URL_PATTERN = "/" + PATH_BASE + "/*";

    /**
     * The full file path of the content.
     */
    public static final String CONTENT_HOME = ConfigManager.getServerHome();

    @Override
    public void doGet(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException {

        if (!ConfigManager.instance()
                .isConfigValue(Key.FEED_ATOM_ADMIN_ENABLE)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!StringUtils.defaultString(request.getPathInfo())
                .equals(PATH_INFO_ADMIN)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            if (!this.checkBasicAuthAccess(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } catch (EndlessWaitException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        try {
            response.setContentType("application/atom+xml");

            final AtomFeedService svc =
                    ServiceContext.getServiceFactory().getAtomFeedService();

            // svc.refreshAdminFeed(); // TEST

            final AtomFeedWriter writer = svc.getAdminFeedWriter(
                    new URI(request.getRequestURL().toString()),
                    response.getOutputStream());

            writer.process();

        } catch (FeedException | URISyntaxException e) {
            throw new ServletException(e.getMessage());
        }
    }

    @Override
    protected boolean isBasicAuthValid(final String username, final String pw) {

        final ConfigManager cm = ConfigManager.instance();

        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(pw)
                && cm.getConfigValue(Key.FEED_ATOM_ADMIN_USERNAME)
                        .equals(username)
                && cm.getConfigValue(Key.FEED_ATOM_ADMIN_PASSWORD).equals(pw);
    }

    @Override
    protected boolean isRemoteAddrAllowed(final String remoteAddr) {
        return true;
    }

    @Override
    protected PubTopicEnum getRateLimiterPubTopic() {
        return PubTopicEnum.FEED;
    }

    @Override
    protected String getRateLimiterSubject() {
        return ROLE_ALLOWED;
    }

}
