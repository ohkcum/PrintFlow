/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
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
package org.printflow.lite.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.printflow.lite.client.cometd.CommonMsgListener;
import org.printflow.lite.client.cometd.ConnectionMsgListener;
import org.printflow.lite.client.cometd.InitializerMsgListener;
import org.printflow.lite.client.cometd.UserEventMsgListener;
import org.printflow.lite.common.ConfigDefaults;
import org.printflow.lite.common.dto.ClientAppConnectDto;
import org.printflow.lite.common.dto.CometdConnectDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserEventClient implements CommonMsgListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserEventClient.class);

    /**
     * The API key for the XML-RPC calls.
     */
    private static final String API_KEY =
            "302d02141b58b57eeb13953634c2591d9f9f62625f16e6"
                    + "d90215008ad66af50e4c66ac9b06b5b11bc1b7e8898ecdff";

    private static final int XMLRPC_CONNECTION_TIMEOUT_MSEC = 5000;
    private static final int XMLRPC_REPLY_TIMEOUT_MSEC = 5000;

    private static final int BAYEUXCLIENT_WAITFOR_TIMEOUT_MSEC = 5000;

    /**
     * .
     */
    private volatile BayeuxClient bayeuxClient;

    /**
     *
     */
    private final UserEventMsgListener userEventMsgListener;

    /**
     * .
     */
    private String userId;

    /**
     * .
     */
    private String userPassword;

    /**
     * .
     */
    private String userAuthToken;

    /**
     * .
     */
    private final String adminPasskey;

    /**
     *
     */
    private final UserEventClientListener userEventClientListener;

    /**
     *
     */
    private XmlRpcClient xmlRpcClient;

    /**
     *
     */
    private ClientAppConnectDto connectInfo;

    /**
     * .
     */
    private final String serverHost;

    /**
     * .
     */
    private final int serverSslPort;

    /**
     *
     * @param serverHost
     *            The server host name.
     * @param serverPort
     *            The server SSL port.
     * @param user
     *            The user id.
     * @param adminPasskey
     *            The admin passkey.
     * @param userEventClientListener
     *            The {@link UserEventClientListener}.
     */
    public UserEventClient(final String serverHost, final String serverPort,
            final String user, final String adminPasskey,
            final UserEventClientListener userEventClientListener) {

        this.serverHost = serverHost;
        this.serverSslPort = Integer.parseInt(serverPort);

        this.userId = user;

        this.adminPasskey = adminPasskey;

        this.userEventClientListener = userEventClientListener;
        this.userEventMsgListener = new UserEventMsgListener(this);

        try {
            xmlRpcClient = createClient(this.serverHost, this.serverSslPort);
        } catch (NumberFormatException | MalformedURLException e) {
            throw new ClientAppException(e.getMessage());
        }

    }

    /**
     *
     * @param serverHost
     *            The server host name.
     * @param serverPort
     *            The server port.
     * @return The {@link XmlRpcClient}.
     * @throws MalformedURLException
     * @throws NumberFormatException
     *             When server port error.
     */
    private static XmlRpcClient createClient(final String serverHost,
            final int serverPort)
            throws NumberFormatException, MalformedURLException {

        final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        final URL url = new URL(ClientApp.SECURE_URL_PROTOCOL, serverHost,
                serverPort, "/xmlrpc");

        config.setServerURL(url);

        //
        config.setConnectionTimeout(XMLRPC_CONNECTION_TIMEOUT_MSEC);
        config.setReplyTimeout(XMLRPC_REPLY_TIMEOUT_MSEC);
        config.setEncoding(null); // UTF-8

        //
        final XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);

        return client;
    }

    /**
     * Gets the {@link ClientAppConnectDto} from the XML-RPC server.
     *
     * @param client
     *            The {@link XmlRpcClient}.
     * @return The {@link ClientAppConnectDto}.
     * @throws XmlRpcException
     * @throws IOException
     */
    private ClientAppConnectDto getConnectInfo(final XmlRpcClient client)
            throws XmlRpcException, IOException {

        /*
         * XML-RPC cannot handle null values.
         */
        final String passKey;
        if (this.adminPasskey == null) {
            passKey = "";
        } else {
            passKey = this.adminPasskey;
        }

        final String password;
        if (this.userPassword == null) {
            password = "";
        } else {
            password = this.userPassword;
        }

        final String authToken;
        if (this.userAuthToken == null) {
            authToken = "";
        } else {
            authToken = this.userAuthToken;
        }

        final String json =
                (String) client.execute("client.getConnectInfo", new Object[] {
                        API_KEY, this.userId, password, authToken, passKey });

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, ClientAppConnectDto.class);
    }

    /**
     * Notifies exit to the XML-RPC server.
     *
     * @param client
     *            The {@link XmlRpcClient}.
     * @throws XmlRpcException
     * @throws IOException
     */
    private void notifyExit(final XmlRpcClient client)
            throws XmlRpcException, IOException {

        /*
         * XML-RPC cannot handle null values.
         */
        final String authToken;
        if (this.userAuthToken == null) {
            authToken = "";
        } else {
            authToken = this.userAuthToken;
        }

        client.execute("client.notifyExit",
                new Object[] { API_KEY, this.userId, authToken });
    }

    /**
     * Creates the CometD handshake fields. Like the JSON string:
     *
     * <pre>
     * { ext : { authentication : { token : token, userToken : userToken } } }
     * </pre>
     *
     * @param sharedToken
     *            The shared authentication token.
     * @param userToken
     *            The user authentication token.
     * @return The handshake fields.
     */
    private static Map<String, Object> getHandshakeFields(
            final String sharedToken, final String userToken) {

        final Map<String, Object> handshakeFields = new HashMap<>();

        final Map<String, Object> ext = new HashMap<>();
        handshakeFields.put(CometdConnectDto.SERVER_MSG_EXT_FIELD, ext);

        final Map<String, Object> authentication = new HashMap<>();
        ext.put(CometdConnectDto.SERVER_MSG_ATTR_AUTH, authentication);

        authentication.put(CometdConnectDto.SERVER_MSG_ATTR_SHARED_TOKEN,
                sharedToken);

        authentication.put(CometdConnectDto.SERVER_MSG_ATTR_USER_TOKEN,
                userToken);

        return handshakeFields;
    }

    /**
     *
     */
    public void disconnect() {

        if (bayeuxClient == null) {
            return;
        }

        /*
         * Remove UserEvent listener (in-line with Mantis #328).
         */
        final ClientSessionChannel channel = bayeuxClient
                .getChannel(this.connectInfo.getCometd().getChannelSubscribe());

        if (channel != null) {
            channel.removeListener(this.userEventMsgListener);
            LOGGER.info("Removed UserEvent listener.");
        }

        /*
         * Stop CometD.
         */
        LOGGER.info("Stopping CometD.");
        this.bayeuxClient.disconnect(1000);

        try {
            this.notifyExit(this.xmlRpcClient);
        } catch (XmlRpcException | IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    @Override
    public void onConnectionClosed() {
        LOGGER.info("Connection closed.");
        this.userEventClientListener.onConnectionClosed();
    }

    @Override
    public void onConnectionEstablished() {
        LOGGER.info("Connection established.");
        this.userEventClientListener.onConnected(this.connectInfo);
        poll(System.currentTimeMillis());
    }

    @Override
    public void onConnectionBroken() {
        LOGGER.info("Connection broken.");
        this.userEventClientListener.onConnectionBroken();
    }

    @Override
    public void onError(final String message) {
        this.userEventClientListener.onError(message);
    }

    @Override
    public void onInitialize(final boolean isSuccessful) {

        if (isSuccessful) {

            bayeuxClient.batch(new Runnable() {

                @Override
                public void run() {

                    final ClientSessionChannel channel =
                            bayeuxClient.getChannel(connectInfo.getCometd()
                                    .getChannelSubscribe());
                    channel.addListener(userEventMsgListener);
                }
            });

        } else {
            // TODO
        }
    }

    /**
     * The long-poll.
     *
     * @param prevMsgTime
     *            The previous polling time as returned by the server, or
     *            {@code null} when this is a first time poll.
     */
    private void poll(final Long prevMsgTime) {

        final Map<String, Object> data = new HashMap<String, Object>();

        data.put("user", this.userId);
        data.put("unique-url-value", "");
        data.put("language", "nl");
        data.put("base64", Boolean.FALSE);
        data.put("webAppClient", Boolean.FALSE);

        if (prevMsgTime != null) {
            data.put("msg-prev-time", prevMsgTime);
        }

        bayeuxClient.getChannel(connectInfo.getCometd().getChannelPublish())
                .publish(data);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Poll for user [%s] ...", this.userId));
        }
    }

    @Override
    public void onPollInvitation(final Long prevMsgTime) {
        this.poll(prevMsgTime);
    }

    @Override
    public void onAccountMessage(final String message) {
        this.userEventClientListener.onUserMessage(message);
    }

    @Override
    public void onJobTicketMessage(final String message) {
        this.userEventClientListener.onUserMessage(message);
    }

    @Override
    public void onPrintIn() {
        this.userEventClientListener.onPrintIn();
    }

    @Override
    public void onPrintInExpired(final String message) {
        this.userEventClientListener.onPrintInExpired(message);
    }

    @Override
    public void onPrintOutMessage(final String message) {
        this.userEventClientListener.onUserMessage(message);
    }

    /**
     *
     */
    public void connect() {

        ClientAppConnectDto connectInfoTmp = null;

        while (connectInfoTmp == null) {

            try {
                connectInfoTmp = getConnectInfo(xmlRpcClient);
            } catch (XmlRpcException e) {
                connectInfoTmp = null;
            } catch (IOException e) {
                onError("A server error occurred.");
                return;
            }

            if (connectInfoTmp == null) {

                final int nSleepSecs = 5;

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "Connect to server, " + "retry after %d seconds...",
                            nSleepSecs));
                }

                try {
                    Thread.sleep(nSleepSecs * 1000L);
                } catch (InterruptedException e) {
                    //
                    return;
                }

            } else if (connectInfoTmp
                    .getStatus() == ClientAppConnectDto.Status.ERROR_AUTH) {

                connectInfoTmp = null;

                this.userPassword = "";

                final UserPasswordDialog dialog =
                        UserPasswordDialog.show(this.userId);

                if (dialog.isCancelled()) {
                    LOGGER.info("Login dialog canceled by user.");
                    System.exit(ClientApp.EXIT_CODE_OK);
                } else {
                    this.userId = dialog.getUserId();
                    this.userPassword = dialog.getUserPassword();
                }

            }
        }

        /*
         * According to Mantis #320.
         */
        this.disconnect();

        /*
         *
         */
        if (connectInfoTmp.getStatus() != ClientAppConnectDto.Status.OK) {
            onError(connectInfoTmp.getStatusMessage());
            return;
        }

        /*
         * Connect again...
         */
        this.connectInfo = connectInfoTmp;

        this.userAuthToken = this.connectInfo.getUserAuthToken();

        final String cometdUrl;

        try {
            cometdUrl = new URL(ClientApp.SECURE_URL_PROTOCOL, this.serverHost,
                    this.serverSslPort, connectInfo.getCometd().getUrlPath())
                            .toString();
        } catch (MalformedURLException e) {
            throw new ClientAppException(e.getMessage(), e);
        }

        final Map<String, Object> transportOptions = new HashMap<>();

        /*
         * The maximum number of milliseconds to wait before considering a
         * request to the Bayeux server failed.
         */
        transportOptions.put(JettyHttpClientTransport.MAX_NETWORK_DELAY_OPTION,
                connectInfo.getCometd().getMaxNetworkDelay());

        /*
         * Create (and eventually set up) Jetty's HttpClient. We use https and
         * accept self-signed certificate of PrintFlowLite server.
         */
        final SslContextFactory.Client sslContextFactory =
                new SslContextFactory.Client(true);

        final ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);

        final HttpClient httpClient =
                new HttpClient(new HttpClientTransportDynamic(clientConnector));

        try {
            httpClient.start();
        } catch (Exception e) {
            throw new ClientAppException(e.getMessage(), e);
        }

        final ClientTransport transport =
                new JettyHttpClientTransport(transportOptions, httpClient);

        bayeuxClient = new BayeuxClient(cometdUrl, transport);

        bayeuxClient.getChannel(Channel.META_HANDSHAKE)
                .addListener(new InitializerMsgListener(this));

        bayeuxClient.getChannel(Channel.META_CONNECT)
                .addListener(new ConnectionMsgListener(bayeuxClient, this));

        bayeuxClient.handshake(
                getHandshakeFields(this.connectInfo.getCometd().getAuthToken(),
                        this.connectInfo.getUserAuthToken()));

        while (true) {

            if (bayeuxClient.waitFor(BAYEUXCLIENT_WAITFOR_TIMEOUT_MSEC,
                    BayeuxClient.State.CONNECTED)) {
                LOGGER.info("Handshake successfull!");
                return;
            }

            LOGGER.error("Could not handshake with {}", cometdUrl);

            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(null,
                    "No connection to server. Do you want to retry?",
                    ConfigDefaults.APP_NAME, JOptionPane.YES_NO_OPTION)) {
                System.exit(ClientApp.EXIT_CODE_OK);
            }
        }
    }
}
