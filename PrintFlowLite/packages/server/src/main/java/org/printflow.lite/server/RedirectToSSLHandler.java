/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2011-2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.util.URIUtil;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.ipp.operation.IppOperationContext;
import org.printflow.lite.server.xmlrpc.SpXmlRpcServlet;

/**
 * Redirect all traffic to SSL, except
 * {@link IppOperationContext.CONTENT_TYPE_IPP},
 * {@link SpXmlRpcServlet.URL_PATTERN_BASE} and
 * {@link WebApp.PATH_IPP_PRINTER_ICONS}.
 */
public final class RedirectToSSLHandler extends SecuredRedirectHandler {

    /**
     * The redirect code to send in response.
     */
    private final int redirectCode = HttpServletResponse.SC_MOVED_TEMPORARILY;

    @Override
    public void handle(final String target,
            final org.eclipse.jetty.server.Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws IOException, ServletException {

        final String contentTypeReq = request.getContentType();

        /*
         * For now, take /xmlrpc as it is, do not redirect. Reason: C++ modules
         * are not prepared for SSL yet.
         */
        if (request.getPathInfo()
                .startsWith(SpXmlRpcServlet.URL_PATTERN_BASE)) {
            return;
        }

        /*
         * PrintFlowLite Printer Icons used by IPP Clients.
         */
        if (request.getPathInfo().startsWith(WebApp.PATH_IPP_PRINTER_ICONS)) {
            return;
        }

        /*
         * Take IPP traffic as it is, do not redirect.
         */
        if (contentTypeReq != null && contentTypeReq
                .equalsIgnoreCase(IppOperationContext.CONTENT_TYPE_IPP)) {
            return;
        }

        this.handleRedirect(target, baseRequest, request, response);
    }

    /**
     * Handles the redirection request.
     * <p>
     * Note: This method is copied from
     * {@link SecuredRedirectHandler#handle(String, Request, HttpServletRequest, HttpServletResponse)}
     * and adapted to create redirection URI dependent on .
     *
     * @param target
     * @param baseRequest
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    public void handleRedirect(final String target, final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws IOException, ServletException {

        final HttpChannel channel = baseRequest.getHttpChannel();

        if (baseRequest.isSecure() || channel == null) {
            // Nothing to do here.
            super.handle(target, baseRequest, request, response);
            return;
        }

        baseRequest.setHandled(true);

        final HttpConfiguration httpConfig = channel.getHttpConfiguration();

        if (httpConfig == null) {
            response.sendError(HttpStatus.FORBIDDEN_403,
                    "Missing HttpConfiguration");
            return;
        }

        final int securePort;
        final String serverName = baseRequest.getServerName();

        if (ConfigManager.getSslCertInfoDefault().getSubjectAltNames()
                .contains(serverName)) {
            securePort = WebServer.getServerPortSslLocal();
        } else {
            securePort = WebServer.getServerPortSsl();
        }

        if (securePort > 0) {
            final String secureScheme = httpConfig.getSecureScheme();
            final String url = URIUtil.newURI(secureScheme,
                    baseRequest.getServerName(), securePort,
                    baseRequest.getRequestURI(), baseRequest.getQueryString());
            response.setContentLength(0);
            baseRequest.getResponse().sendRedirect(redirectCode, url, true);
        } else {
            response.sendError(HttpStatus.FORBIDDEN_403,
                    "HttpConfiguration.securePort not configured");
        }
    }

}
