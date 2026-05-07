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
package org.printflow.lite.server.restful;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.services.RateLimiterService.EndlessWaitException;
import org.printflow.lite.core.services.RateLimiterService.IEvent;
import org.printflow.lite.core.services.RateLimiterService.LimitEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.IRateLimiterListener;
import org.printflow.lite.core.util.HttpAuthenticationUtil;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestAuthFilter implements
        javax.ws.rs.container.ContainerRequestFilter, IRateLimiterListener {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestAuthFilter.class);

    /** */
    public static final String SHORT_NAME = "RESTFul API";

    /** */
    private static final RateLimiterService RATE_LIMITER_SERVICE =
            ServiceContext.getServiceFactory().getRateLimiterService();

    /**
     * Admin role for Basic Authentication.
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /** */
    @Context
    private ResourceInfo resourceInfo;

    /** */
    @Context
    private HttpServletRequest servletRequest;

    /**
     *
     * @param requestContext
     *            context.
     * @param status
     *            Response status.
     */
    private void abortWith(final ContainerRequestContext requestContext,
            final Response.Status status) {

        try {
            this.onRateLimiting(WebAppHelper.getClientIP(this.servletRequest));

            requestContext.abortWith(
                    Response.status(status).entity("Access denied.").build());

        } catch (EndlessWaitException e) {
            requestContext.abortWith(
                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("Access denied.").build());
        }
    }

    @Override
    public void filter(final ContainerRequestContext context) {

        if (!isRemoteAddressAllowed()) {
            this.abortWith(context, Response.Status.FORBIDDEN);
            return;
        }

        final Method method = resourceInfo.getResourceMethod();

        if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        if (method.isAnnotationPresent(DenyAll.class)) {
            this.abortWith(context, Response.Status.FORBIDDEN);
            return;
        }

        final boolean rolesAllowedPresent =
                method.isAnnotationPresent(RolesAllowed.class);

        if (!rolesAllowedPresent) {
            return;
        }

        // Fetch authorization header
        final MultivaluedMap<String, String> headers = context.getHeaders();

        final List<String> authorization = headers
                .get(HttpAuthenticationUtil.HEADER_AUTHORIZATION);

        if (authorization == null || authorization.isEmpty()) {
            this.abortWith(context, Response.Status.UNAUTHORIZED);
            return;
        }

        final String[] aUserPw = HttpAuthenticationUtil
                .getBasicAuthUserPassword(authorization.get(0));
        final String username = aUserPw[0];
        final String password = aUserPw[1];

        // Verify user access
        if (rolesAllowedPresent) {

            final RolesAllowed rolesAnnotation =
                    method.getAnnotation(RolesAllowed.class);

            final Set<String> rolesSet =
                    new HashSet<String>(Arrays.asList(rolesAnnotation.value()));

            if (!isBasicAuthValid(username, password, rolesSet)) {
                this.abortWith(context, Response.Status.UNAUTHORIZED);
                return;
            }
        }
    }

    /**
     * @return {@code true} if remote address is allowed.
     */
    private boolean isRemoteAddressAllowed() {

        final String clientAddress =
                WebAppHelper.getClientIP(this.servletRequest);

        final String cidrRanges = ConfigManager.instance()
                .getConfigValue(Key.API_RESTFUL_IP_ADDRESSES_ALLOWED);

        final boolean allowed = StringUtils.isBlank(cidrRanges)
                || InetUtils.isIpAddrInCidrRanges(cidrRanges, clientAddress);

        if (!allowed) {
            LOGGER.warn("Access denied for {}. Allowed CIDR ranges: {}",
                    clientAddress, cidrRanges);

            AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE,
                    PubLevelEnum.WARN,
                    String.format(SHORT_NAME + " denied for remote address %s.",
                            clientAddress));
        }
        return allowed;
    }

    /**
     * @param username
     *            User name.
     * @param pw
     *            User password.
     * @param rolesSet
     *            Roles.
     * @return {@code true} when allowed.
     */
    private boolean isBasicAuthValid(final String username, final String pw,
            final Set<String> rolesSet) {

        final ConfigManager cm = ConfigManager.instance();

        final boolean isAuthValid = rolesSet.contains(ROLE_ADMIN)
                && StringUtils.isNotBlank(username)
                && StringUtils.isNotBlank(pw)
                && cm.getConfigValue(Key.API_RESTFUL_AUTH_USERNAME)
                        .equals(username)
                && cm.getConfigValue(Key.API_RESTFUL_AUTH_PASSWORD).equals(pw);

        if (!isAuthValid) {
            final String clientAddress =
                    WebAppHelper.getClientIP(this.servletRequest);
            LOGGER.warn("User access denied for [{}]", clientAddress);

            AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE,
                    PubLevelEnum.WARN,
                    String.format(SHORT_NAME + " user access denied [%s].",
                            clientAddress));
        }
        return isAuthValid;
    }

    /**
     * @param clientIPAddress
     *            Client IP address.
     * @throws EndlessWaitException
     */
    private void onRateLimiting(final String clientIPAddress)
            throws EndlessWaitException {

        if (ConfigManager.isAPIRateLimitingEnabled()) {
            RATE_LIMITER_SERVICE.consumeOrWaitForEvent(
                    LimitEnum.API_FAILURE_BY_IP,
                    new RateLimiterService.IPEvent(clientIPAddress, SHORT_NAME),
                    this);
        }
    }

    @Override
    public void onRateLimited(final IEvent event, final long waitMsec) {
        AdminPublisher.publish(event, PubTopicEnum.WEB_SERVICE, waitMsec);
    }

}
