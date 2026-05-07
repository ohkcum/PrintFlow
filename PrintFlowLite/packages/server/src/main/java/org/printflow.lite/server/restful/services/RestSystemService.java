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
package org.printflow.lite.server.restful.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.server.WebServer;
import org.printflow.lite.server.restful.RestApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST system API.
 * <p>
 * Implementation of Jersey extended WADL support is <i>under construction</i>.
 * <p>
 *
 * @author Rijk Ravestein
 *
 */
@Path("/" + RestSystemService.PATH_MAIN)
public final class RestSystemService extends AbstractRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestSystemService.class);

    /** */
    public static final String PATH_MAIN = "system";

    /** */
    private static final String PATH_SUB_VERSION = "version";

    /**
     * @return Application version.
     */
    @GET
    @Path(PATH_SUB_VERSION)
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return ConfigManager.getAppNameVersionBuild();
    }

    /**
     * Tests the service by calling a simple GET of this RESTful service. The
     * result is logged.
     */
    public static void test() {

        final Client client = ServiceContext.getServiceFactory()
                .getRestClientService().createClient();

        final String serverHost;

        if (WebServer.getServerHost() == null) {
            serverHost = InetUtils.getServerHostName();
        } else {
            serverHost = WebServer.getServerHost();
        }

        final WebTarget[] webTargets = new WebTarget[] { //
                client.target(InetUtils.URL_PROTOCOL_HTTP + "://" + serverHost
                        + ":" + ConfigManager.getServerPort())
                        .path(RestApplication.RESTFUL_URL_PATH).path(PATH_MAIN)
                        .path(PATH_SUB_VERSION), //
                client.target(InetUtils.URL_PROTOCOL_HTTPS + "://" + serverHost
                        + ":" + ConfigManager.getServerSslPort())
                        .path(RestApplication.RESTFUL_URL_PATH).path(PATH_MAIN)
                        .path(PATH_SUB_VERSION) };

        for (final WebTarget webTarget : webTargets) {

            final Invocation.Builder invocationBuilder =
                    webTarget.request(MediaType.TEXT_PLAIN);

            try (Response response = invocationBuilder.get();) {
                final String version = response.readEntity(String.class);
                SpInfo.instance()
                        .log(String.format("%s test: GET %s -> %s [%s] [%s]",
                                RestSystemService.class.getSimpleName(),
                                webTarget.getUri().toString(),
                                response.getStatus(), response.getStatusInfo(),
                                version));

            } catch (ProcessingException e) {
                LOGGER.error(
                        webTarget.getUri().toString() + " : " + e.getMessage());
            }
        }
    }
}
