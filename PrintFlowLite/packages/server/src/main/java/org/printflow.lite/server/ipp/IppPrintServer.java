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
package org.printflow.lite.server.ipp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.enums.ReservedIppQueueEnum;
import org.printflow.lite.core.dao.helpers.IppQueueHelper;
import org.printflow.lite.core.ipp.IppProcessingException;
import org.printflow.lite.core.ipp.IppProcessingException.StateEnum;
import org.printflow.lite.core.ipp.attribute.IppAuthMethodEnum;
import org.printflow.lite.core.ipp.operation.AbstractIppOperation;
import org.printflow.lite.core.ipp.operation.IppOperationContext;
import org.printflow.lite.core.ipp.operation.IppOperationId;
import org.printflow.lite.core.jpa.IppQueue;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.print.server.DocContentPrintProcessor;
import org.printflow.lite.core.services.QueueService;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.services.RateLimiterService.IEvent;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.ServiceEntryPoint;
import org.printflow.lite.core.services.UserService;
import org.printflow.lite.core.services.helpers.IRateLimiterListener;
import org.printflow.lite.core.users.IExternalUserAuthenticator;
import org.printflow.lite.core.users.InternalUserAuthenticator;
import org.printflow.lite.core.util.HttpAuthenticationUtil;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.server.WebApp;
import org.printflow.lite.server.webapp.WebAppHelper;
import org.printflow.lite.server.webapp.WebAppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PrintFlowLite IPP Print Server handling all IPP requests.
 * <p>
 * IMPORTANT: This class is mounted to the {@link WebApp#MOUNT_PATH_PRINTERS}
 * context. in the {@link WebApp} class. Its subclass
 * {@link IppPrintServerHomePage} is the handler of the default web context.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class IppPrintServer extends WebPage
        implements ServiceEntryPoint, IRateLimiterListener {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String CONTENT_TYPE_PPD = "application/vnd.cups-ppd";

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppPrintServer.class);

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    public static void start() {
        SpInfo.instance().log("IPP Print Server started.");
    }

    /**
     *
     */
    private static class IppResourceStream extends AbstractResourceStream {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private final InputStream istr;

        /**
         *
         * @param istr
         *            Input stream.
         */
        IppResourceStream(final InputStream istr) {
            this.istr = istr;
        }

        @Override
        public void close() throws IOException {
            istr.close();
        }

        @Override
        public InputStream getInputStream()
                throws ResourceStreamNotFoundException {
            return istr;
        }

    }

    /**
     * Our own handler to control the .... streaming ...
     *
     * @author rijk
     *
     */
    private static class SpStreamRequestHandler
            extends ResourceStreamRequestHandler {

        /**
         * @param istr
         *            The {@link InputStream}.
         */
        SpStreamRequestHandler(final InputStream istr) {
            super(new IppResourceStream(istr));
        }
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public IppPrintServer(final PageParameters parameters) {

        final RequestCycle requestCycle = getRequestCycle();

        final HttpServletRequest request = (HttpServletRequest) requestCycle
                .getRequest().getContainerRequest();

        if (LOGGER.isDebugEnabled()) {
            logDebug(request, parameters);
        }

        final HttpServletResponse response = (HttpServletResponse) requestCycle
                .getResponse().getContainerResponse();

        final String contentTypeReq = request.getContentType();

        /*
         * Request for a PPD file. See Mantis #160, #650.
         */
        if (contentTypeReq == null
                && StringUtils.upperCase(request.getRequestURL().toString())
                        .endsWith(".PPD")) {
            this.handlePpdRequest(response);
            return;
        }

        /*
         * Redirect to /user page for content types other than IPP_CONTENT_TYPE.
         */
        if (contentTypeReq == null || !contentTypeReq
                .equalsIgnoreCase(IppOperationContext.CONTENT_TYPE_IPP)) {
            this.setResponsePage(WebAppUser.class);
            return;
        }

        /*
         * OK, we can handle the IPP request.
         */
        response.setContentType(IppOperationContext.CONTENT_TYPE_IPP);
        response.setStatus(HttpServletResponse.SC_OK);

        /*
         * NOTE: There is NO top level database transaction. Specialized methods
         * have their own database transaction.
         */
        ServiceContext.open();

        final String remoteAddr = WebAppHelper.getClientIP(request);

        /*
         * Get the Queue from the URL.
         */
        final IppPrintServerUrlParms serverPageParms =
                new IppPrintServerUrlParms(
                        Url.parse(request.getRequestURL().toString()));

        final String requestedQueueUrlPath = serverPageParms.getPrinter();

        /*
         * Basic Authentication?
         */
        final String ippAuthenticatedUser;

        final String auth =
                request.getHeader(HttpAuthenticationUtil.HEADER_AUTHORIZATION);

        if (auth == null) {
            ippAuthenticatedUser = null;
        } else {
            final String[] aUserPw =
                    HttpAuthenticationUtil.getBasicAuthUserPassword(auth);

            final String pubMsgFormat = "User \"%s\" print on queue \"%s\": %s";
            final String pubMsg;

            if (this.authenticateUser(aUserPw[0], aUserPw[1])) {
                ippAuthenticatedUser = aUserPw[0];

                pubMsg = String.format(pubMsgFormat, ippAuthenticatedUser,
                        requestedQueueUrlPath, "authenticated");
            } else {
                ippAuthenticatedUser = null;
                pubMsg = String.format(pubMsgFormat, aUserPw[0],
                        requestedQueueUrlPath, "authentication FAILED");
            }
            AdminPublisher.instance().publish(PubTopicEnum.IPP,
                    PubLevelEnum.INFO, pubMsg);
        }

        final boolean ippAuthenticated = ippAuthenticatedUser != null;
        IppAuthMethodEnum ippAuthMethod = IppAuthMethodEnum.NONE;

        try {
            /*
             * Reserved queue?
             */
            final ReservedIppQueueEnum reservedQueueEnum =
                    QUEUE_SERVICE.getReservedQueue(requestedQueueUrlPath);

            /*
             * Find queue object.
             */
            final IppQueue queue = ServiceContext.getDaoContext()
                    .getIppQueueDao().findByUrlPath(requestedQueueUrlPath);

            ippAuthMethod = QUEUE_SERVICE.getIppAuthMethod(queue);

            /*
             * Access allowed?
             */
            if (reservedQueueEnum != null
                    && !reservedQueueEnum.isDriverPrint()) {

                throw new IppProcessingException(StateEnum.UNAVAILABLE,
                        String.format("Queue [%s] is not for driver print.",
                                reservedQueueEnum.getUiText()));

            } else if (queue == null || queue.getDeleted()) {

                throw new IppProcessingException(StateEnum.UNAVAILABLE,
                        "Queue does not exist.");

            } else if (reservedQueueEnum != ReservedIppQueueEnum.IPP_PRINT_INTERNET
                    && StringUtils.isBlank(queue.getIpAllowed())
                    && InetUtils.isPublicAddress(remoteAddr)) {

                throw new IppProcessingException(StateEnum.UNAVAILABLE,
                        String.format(
                                "Queue [%s] is not accessible"
                                        + " from the Internet.",
                                IppQueueHelper.uiPath(queue)));
            } else {

                if (!QUEUE_SERVICE.hasClientIpAccessToQueue(queue,
                        serverPageParms.getPrinter(), remoteAddr)) {
                    throw new IppProcessingException(StateEnum.UNAVAILABLE,
                            String.format(
                                    "Queue [%s] is not allowed for IP address.",
                                    IppQueueHelper.uiPath(queue)));
                }
            }

            /*
             * Authenticated User ID associated with Internet Print or remote IP
             * address: null if not authenticated.
             */
            final String authenticatedUser;

            /*
             * If true, the authenticatedUser overrules the
             * "requesting-user-name" in IPP Requests.
             */
            final boolean isAuthUserIppRequester;

            if (reservedQueueEnum == ReservedIppQueueEnum.IPP_PRINT_INTERNET) {

                final User remoteInternetUser = USER_SERVICE
                        .findUserByNumberUuid(serverPageParms.getUserNumber(),
                                serverPageParms.getUserUuid());

                if (remoteInternetUser == null) {
                    throw new IppProcessingException(StateEnum.UNAVAILABLE,
                            "Print service not available for user/uuid.");
                }

                authenticatedUser = remoteInternetUser.getUserId();
                isAuthUserIppRequester = true;

            } else {

                isAuthUserIppRequester =
                        BooleanUtils.isNotTrue(queue.getTrusted());

                if (ippAuthMethod != IppAuthMethodEnum.REQUESTING_USER_NAME
                        && isAuthUserIppRequester) {

                    authenticatedUser = ippAuthenticatedUser;

                } else {

                    final String authUserByIP =
                            WebApp.getAuthUserByIpAddr(remoteAddr);

                    if (authUserByIP == null) {
                        authenticatedUser =
                                ConfigManager.getTrustedUserByIP(remoteAddr);
                    } else {
                        authenticatedUser = authUserByIP;
                    }
                }
            }

            /*
             * Handle the request.
             */
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            final IppOperationContext ippOperationContext =
                    new IppOperationContext();

            ippOperationContext.setRemoteAddr(remoteAddr);
            ippOperationContext.setRequestedQueueUrlPath(requestedQueueUrlPath);
            ippOperationContext
                    .setIppRoutingListener(WebApp.get().getPluginManager());
            ippOperationContext.setAuthMethod(ippAuthMethod);

            final InputStream istr;
            if ("chunked".equals(request.getHeader("Transfer-Encoding"))) {
                // TODO use IppChunkedInputStream like this?
                // istr = new IppChunkedInputStream(request.getInputStream());
                istr = request.getInputStream();
            } else {
                istr = request.getInputStream();
            }

            final IppOperationId ippOperationId = AbstractIppOperation.handle(
                    queue, istr, bos, authenticatedUser, isAuthUserIppRequester,
                    ippOperationContext);

            if (ippOperationId != null
                    && ippOperationId == IppOperationId.VALIDATE_JOB) {

                final String warnMsg;

                if (reservedQueueEnum != null
                        && !reservedQueueEnum.isDriverPrint()) {

                    warnMsg = new StringBuilder()
                            .append("Print to reserved queue [")
                            .append(requestedQueueUrlPath)
                            .append("] denied from ").append(remoteAddr)
                            .toString();

                } else if (queue == null || queue.getDeleted()) {

                    warnMsg = new StringBuilder()
                            .append("Print to unknown queue [")
                            .append(requestedQueueUrlPath)
                            .append("] denied from ").append(remoteAddr)
                            .toString();

                } else if (queue.getDisabled()) {

                    warnMsg = new StringBuilder()
                            .append("Print to disabled queue [")
                            .append(requestedQueueUrlPath)
                            .append("] denied from ").append(remoteAddr)
                            .toString();

                } else {

                    warnMsg = null;

                }

                if (warnMsg != null) {
                    AdminPublisher.instance().publish(PubTopicEnum.IPP,
                            PubLevelEnum.WARN, warnMsg);
                }

            }

            if (LOGGER.isTraceEnabled()) {
                logIppOutputTrace(bos);
            }

            // Finishing up.
            scheduleRequestHandlerAfterCurrent(requestCycle, bos.toByteArray());

        } catch (IOException | IppProcessingException e) {

            this.onRateLimiting(remoteAddr, e);

            // Dummy byte for unavailable service.
            scheduleRequestHandlerAfterCurrent(requestCycle, new byte[1]);

            final int httpStatus;

            if (e instanceof IOException) {
                httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            } else {
                httpStatus = this.handleException((IppProcessingException) e,
                        ippAuthMethod, ippAuthenticated, response);
            }
            response.setStatus(httpStatus);

        } finally {
            ServiceContext.close();
        }
    }

    /**
     * Handles a {@link IppProcessingException}.
     *
     * @param procEx
     *            {@link IppProcessingException}.
     * @param ippAuthMethod
     *            Authentication method.
     * @param ippAuthenticated
     *            {@code true} if successful user authentication, {@code false}
     *            if user was not authenticated.
     * @param response
     *            HTTP response.
     * @return HTTP status.
     */
    private int handleException(final IppProcessingException procEx,
            final IppAuthMethodEnum ippAuthMethod,
            final boolean ippAuthenticated,
            final HttpServletResponse response) {

        final int httpStatus;

        switch (procEx.getProcessingState()) {

        case CONTENT_ERROR:
            httpStatus = HttpServletResponse.SC_NOT_ACCEPTABLE;
            break;

        case UNAUTHORIZED:

            switch (ippAuthMethod) {

            case BASIC:

                if (ippAuthenticated) {

                    httpStatus = HttpServletResponse.SC_OK;

                } else {

                    httpStatus = HttpServletResponse.SC_UNAUTHORIZED;

                    response.setHeader(
                            HttpAuthenticationUtil.HEADER_WWW_AUTHENTICATE,
                            HttpAuthenticationUtil.wwwAuthTypeRealm(
                                    HttpAuthenticationUtil.SCHEME_BASIC,
                                    CommunityDictEnum.PrintFlowLite.getWord()));
                }
                break;

            default:
                /*
                 * Do NOT use HttpServletResponse.SC_UNAUTHORIZED, because this
                 * will make the IPP client try endlessly. SC_OK produces an
                 * end-state. Mantis #1181.
                 */
                httpStatus = HttpServletResponse.SC_OK;
                break;
            }
            break;

        case UNAVAILABLE:
            httpStatus = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            break;
        case INTERNAL_ERROR:
            // no break intended
        default:
            httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            break;
        }
        return httpStatus;
    }

    /**
     *
     * @param uid
     * @param userPassword
     * @return {@code true} if authenticated.
     */
    private boolean authenticateUser(final String uid,
            final String userPassword) {

        final boolean isAuth;
        /*
         * Read real user from database.
         */
        final User userDb = ServiceContext.getDaoContext().getUserDao()
                .findActiveUserByUserId(uid);

        if (userDb == null || !userDb.getPerson()) {

            isAuth = false;

        } else if (userDb.getInternal()) {

            isAuth = InternalUserAuthenticator.authenticate(userDb,
                    userPassword);
        } else {

            final IExternalUserAuthenticator userAuthenticator =
                    ConfigManager.instance().getUserAuthenticator();

            isAuth = userAuthenticator != null && userAuthenticator
                    .authenticate(uid, userPassword) != null;
        }
        return isAuth;
    }

    /**
     * Applies Rate Limiting.
     * <ul>
     * <li>If PrintIn Rate Limiting is <i>disabled</i>, a standard wait time is
     * applied to prevent continuous messaging if IPP client keeps retrying with
     * same result. See
     * {@link IConfigProp.Key#PRINT_IN_IPP_DEFAULT_WAIT_AFTER_FAILURE_MSEC}.
     * </li>
     * <li>If PrintIn Rate Limiting is <i>enabled</i>, no waiting time is
     * applied here because it is handled in
     * {@link DocContentPrintProcessor}.</li>
     * </ul>
     *
     * @param remoteAddr
     *            Client IP address.
     * @param e
     *            {@link Exception}
     */
    private void onRateLimiting(final String remoteAddr, final Exception e) {

        if (!ConfigManager.isPrintInRateLimitingEnabled()) {

            final long waitMsec = ConfigManager.instance().getConfigLong(
                    Key.PRINT_IN_IPP_DEFAULT_WAIT_AFTER_FAILURE_MSEC);

            if (waitMsec > 0) {
                this.onRateLimited(new RateLimiterService.IPEvent(remoteAddr,
                        e.getMessage()), waitMsec);
            }
        }
    }

    @Override
    public final void onRateLimited(final IEvent event, final long waitMsec) {

        AdminPublisher.publish(event, PubTopicEnum.IPP, waitMsec);

        try {
            Thread.sleep(waitMsec);
        } catch (InterruptedException e1) {
            // noop
        }
    }

    /**
     * Schedules the request handler to be executed after the current one.
     *
     * @param requestCycle
     *            The request cycle.
     * @param buf
     *            The buffer to handle.
     */
    private static void scheduleRequestHandlerAfterCurrent(
            final RequestCycle requestCycle, final byte[] buf) {

        final ResourceStreamRequestHandler handler =
                new SpStreamRequestHandler(new ByteArrayInputStream(buf));

        handler.setContentDisposition(ContentDisposition.INLINE);
        requestCycle.scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Handles a request for the PrintFlowLite.ppd file: the PPD file is returned
     * INLINE.
     *
     * @param response
     *            The response.
     */
    private void handlePpdRequest(final HttpServletResponse response) {

        response.setContentType(CONTENT_TYPE_PPD);
        response.setStatus(HttpServletResponse.SC_OK);

        final File file = ConfigManager.getPpdFile();

        if (!file.exists()) {
            throw new SpException(
                    "PPD file [" + file.getAbsolutePath() + "] does NOT exist");
        }

        final IResourceStream resourceStream = new FileResourceStream(file);
        final ResourceStreamRequestHandler handler =
                new ResourceStreamRequestHandler(resourceStream);

        handler.setContentDisposition(ContentDisposition.INLINE);
        WebAppHelper.setCacheDurationZero(handler);

        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Debug Log the request and parameters.
     *
     * @param request
     *            HTTP request.
     * @param parameters
     *            Page parameters.
     */
    private static void logDebug(final HttpServletRequest request,
            final PageParameters parameters) {

        final StringBuilder log = new StringBuilder();

        final String bar = "\n+-------------------------------------"
                + "-------------------------------------------+";

        log.append(bar);
        log.append("\n| Request [").append(request.getRequestURL().toString())
                .append("]\n|    From [")
                .append(WebAppHelper.getClientIP(request)).append("] Bytes [")
                .append(request.getContentLength()).append("]");
        log.append(bar).append("\n");

        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {

            final String name = headerNames.nextElement();
            final Enumeration<String> nameHeader = request.getHeaders(name);

            log.append("Header [").append(name).append("]:");

            while (nameHeader.hasMoreElements()) {
                log.append(" [").append(nameHeader.nextElement()).append("]");
            }
            log.append('\n');
        }

        for (final NamedPair namedPair : parameters.getAllNamed()) {
            log.append("Parameter [").append(namedPair.getKey()).append("] = [")
                    .append(namedPair.getValue()).append("]\n");
        }

        LOGGER.debug(log.toString());
    }

    /**
     * Writes a pretty printed byte trace of the raw output stream to the log
     * file.
     *
     * @param bos
     *            The output stream.
     */
    private static void logIppOutputTrace(final ByteArrayOutputStream bos) {

        final int width = 10;

        final StringBuilder msg = new StringBuilder(1024);

        int i = 0;
        for (byte b : bos.toByteArray()) {

            if (i % width == 0) {
                msg.append("\n");
            }
            msg.append(String.format("0x%02X ", b));
            i++;
        }
        LOGGER.trace(msg.toString());
    }

}
