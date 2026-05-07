/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.cometd;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.printflow.lite.common.dto.CometdConnectDto;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.util.DateUtil;
import org.printflow.lite.core.util.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Singleton CometD client to the PrintFlowLite server instance.
 *
 * @author Rijk Ravestein
 *
 */
public final class AdminPublisher extends CometdClientMixin {

    /**
     * The channel this <i>administrator</i> client <strong>publishes</strong>
     * (broadcasts) to. This is the channel where <i>user</i> clients
     * <strong>subscribe</strong> to.
     */
    public static final String CHANNEL_PUBLISH = "/admin";

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminPublisher.class);

    /**
     *
     */
    private ClientSession myClientSession = null;

    /**
     *
     */
    private AdminPublisher() {
        super();
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link AdminPublisher#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        /**
         * .
         */
        public static final AdminPublisher INSTANCE = new AdminPublisher();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static AdminPublisher instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes the CometD client session.
     *
     * @param serverPort
     *            The IP port of the server.
     * @param isSsl
     *            {@code true} when SSL port.
     */
    public void init(final int serverPort, final boolean isSsl) {

        final StringBuilder clientUrl = new StringBuilder();
        if (isSsl) {
            clientUrl.append(InetUtils.URL_PROTOCOL_HTTPS);
        } else {
            clientUrl.append(InetUtils.URL_PROTOCOL_HTTP);
        }
        // Important: use "localhost" in client URL.
        clientUrl.append("://").append(InetUtils.LOCAL_HOST).append(":")
                .append(serverPort).append("/cometd");

        /*
         * Create (and eventually setup) Jetty's HttpClient.
         */
        final HttpClient httpClient;

        if (isSsl) {

            final SslContextFactory.Client sslContextFactory =
                    new SslContextFactory.Client();

            /*
             * Since we connect to local host to our own server, we can simply
             * trust all.
             */
            sslContextFactory.setTrustAll(true);

            /*
             * Disable host name verification.
             */
            sslContextFactory.setEndpointIdentificationAlgorithm(null);

            final ClientConnector clientConnector = new ClientConnector();
            clientConnector.setSslContextFactory(sslContextFactory);

            httpClient = new HttpClient(
                    new HttpClientTransportDynamic(clientConnector));

        } else {
            httpClient = new HttpClient();
        }

        /*
         * Here setup Jetty's HttpClient
         */
        // httpClient.setMaxConnectionsPerAddress(2);

        try {
            httpClient.start();
        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);
        }

        /*
         * Prepare the transport
         */
        final Map<String, Object> options = new HashMap<String, Object>();

        final ClientTransport transport =
                new JettyHttpClientTransport(options, httpClient);

        //
        myClientSession = new BayeuxClient(clientUrl.toString(), transport);

        myClientSession.getChannel(Channel.META_CONNECT)
                .addListener(new ClientSessionChannel.MessageListener() {

                    @Override
                    public void onMessage(final ClientSessionChannel channel,
                            final Message message) {

                        if (message.isSuccessful()) {
                            LOGGER.debug("connected");
                        } else {
                            /*
                             * This also occurs when application is shutting
                             * down.
                             */
                            LOGGER.debug("disconnected");
                        }
                    }

                });

        myClientSession.getChannel(Channel.META_HANDSHAKE)
                .addListener(new ClientSessionChannel.MessageListener() {

                    @Override
                    public void onMessage(final ClientSessionChannel channel,
                            final Message message) {

                        if (message.isSuccessful()) {
                            LOGGER.debug("handshake OK");
                        } else {
                            /*
                             * Do NOT log an error, since an automatic Cometd
                             * retry can be successful.
                             */
                            LOGGER.debug("handshake not successful (yet)");
                        }
                    }
                });

        /*
         * Create the user credentials and pass as parameter to the handshake.
         *
         * We cannot use the internal administrator 'admin' as user because we
         * do not know the plain password. So, we use the UUID as user instead.
         * This makes the absence of a password acceptable (for now).
         */
        final Map<String, Object> authentication =
                new HashMap<String, Object>();

        authentication.put(CometdConnectDto.SERVER_MSG_ATTR_SHARED_TOKEN,
                SHARED_USER_ADMIN_TOKEN);

        final Map<String, Object> ext = new HashMap<String, Object>();
        ext.put(CometdConnectDto.SERVER_MSG_ATTR_AUTH, authentication);

        final Map<String, Object> template = new HashMap<String, Object>();
        template.put(Message.EXT_FIELD, ext);

        LOGGER.debug("starting handshake ...");
        myClientSession.handshake(template);
    }

    /**
     *
     */
    public void shutdown() {
        if (myClientSession != null) {
            LOGGER.debug("shutting down...");
            myClientSession.disconnect();
        }
    }

    /**
     * Publishes a message as realtime activity.
     *
     * @param topic
     *            The topic.
     * @param level
     *            The {@link PubLevelEnum}.
     * @param msg
     *            The message.
     */
    public void publish(final PubTopicEnum topic, final PubLevelEnum level,
            final String msg) {
        this.publishEx(topic, level, msg, Boolean.FALSE, true);
    }

    /**
     * Publishes a single pop-up message.
     *
     * @param topic
     *            The topic.
     * @param level
     *            The {@link PubLevelEnum}.
     * @param msg
     *            The message.
     */
    public void publishPopup(final PubTopicEnum topic, final PubLevelEnum level,
            final String msg) {
        this.publishEx(topic, level, msg, Boolean.TRUE, true);
    }

    /**
     * Publishes a real-time and pop-up message.
     *
     * @param topic
     *            The topic.
     * @param level
     *            The {@link PubLevelEnum}.
     * @param msg
     *            The message.
     */
    public void publishAndPopup(final PubTopicEnum topic,
            final PubLevelEnum level, final String msg) {
        this.publishEx(topic, level, msg, Boolean.FALSE, true);
        this.publishEx(topic, level, msg, Boolean.TRUE, false);
    }

    /**
     * Publishes a Rate Limiting info event and logs an info message.
     *
     * @param event
     *            Event.
     * @param topic
     *            Topic.
     * @param waitMsec
     *            Waiting time (milliseconds) for event.
     */
    public static void publish(final RateLimiterService.IEvent event,
            final PubTopicEnum topic, final long waitMsec) {
        publish(event, topic, waitMsec, true);
    }

    /**
     * Publishes a Rate Limiting info event and logs an info message.
     *
     * @param event
     *            Event.
     * @param topic
     *            Topic.
     * @param consumableAfterMsec
     *            time (milliseconds) till event is consumable.
     * @param isMsgWait
     *            {@code true} for "wait", {@code false} for "block" message.
     */
    public static void publish(final RateLimiterService.IEvent event,
            final PubTopicEnum topic, final long consumableAfterMsec,
            final boolean isMsgWait) {

        final String durationPfx;
        if (isMsgWait) {
            durationPfx = "wait";
        } else {
            durationPfx = "block";
        }
        final String msg = String.format("%s Rate Limiting for [%s] : %s %s",
                event.subject(), event.id(), durationPfx,
                DateUtil.formatDuration(consumableAfterMsec));

        instance().publish(topic, PubLevelEnum.INFO, msg);

        SpInfo.instance().log(msg);
    }

    /**
     * Publishes a message.
     *
     * @param topic
     *            The topic.
     * @param level
     *            The {@link PubLevelEnum}.
     * @param msg
     *            The message.
     * @param popup
     *            {@code true} if pop-up message.
     * @param publish
     *            if {@code true} message if written in admin log file.
     */
    private void publishEx(final PubTopicEnum topic, final PubLevelEnum level,
            final String msg, final Boolean popup, final boolean publish) {

        final Date now = new Date();

        if (publish) {
            AdminPublisherLogger.logInfo(topic, level, msg, now);
        }

        final String channelName =
                CHANNEL_PUBLISH + "/" + topic.getChannelTopic();

        final Map<String, Object> msgMap = new HashMap<String, Object>();

        final SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("HH:mm:ss");

        msgMap.put("time", dateFormat.format(now).toString());
        msgMap.put("topic", topic.getChannelTopic());
        msgMap.put("level", level.toString());
        msgMap.put("msg", msg);
        msgMap.put("popup", popup);

        try {

            final String json = new ObjectMapper().writeValueAsString(msgMap);

            myClientSession.getChannel(channelName).publish(json);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("message " + json + " published on channel "
                        + channelName);
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.error("Message [" + msg + "] could not be delivered.");
        }
    }

    /**
     *
     * @return {@code true} when up and running.
     */
    public static boolean isActive() {
        return instance().myClientSession != null;
    }
}
