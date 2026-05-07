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
package org.printflow.lite.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.DispatcherType;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.printflow.lite.common.SystemPropertyEnum;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.config.ConfigException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.ServerDataFileNameEnum;
import org.printflow.lite.core.config.ServerFileNameEnum;
import org.printflow.lite.core.config.ServerFilePathEnum;
import org.printflow.lite.core.config.ServerPropEnum;
import org.printflow.lite.core.config.SslCertInfo;
import org.printflow.lite.core.util.DeadlockedThreadsDetector;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.server.ext.papercut.ExtPaperCutSyncServlet;
import org.printflow.lite.server.feed.AtomFeedServlet;
import org.printflow.lite.server.restful.RestApplication;
import org.printflow.lite.server.restful.services.RestSystemService;
import org.printflow.lite.server.restful.services.RestTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty Web Server.
 *
 * @author Rijk Ravestein
 *
 */
public final class WebServer {

    /**
     * The logger.
     */
    private static Logger theLogger;

    /**
     * Log message from the {@link WebServerStartupHook}.
     */
    private static String theStartupHookLogMsg;

    /**
     * PrintFlowLite branded session cookie to avoid session conflict with other
     * Jetty powered Web App instances on same host that use default session
     * cookie.
     * <p>
     * Also see {@code init-param: browserCookieName = PFL_BAYEUX_BROWSER} of
     * {@code cometd} servlet in {@code web.xml}.
     * </p>
     */
    private static final String SERVER_SESSION_COOKIE = "PFL_JSESSIONID";

    /** */
    private static boolean developerEnv;

    /** */
    private static String serverHost;

    /** */
    private static int serverPort;

    /** */
    private static int serverPortSsl;

    /** */
    private static int serverPortSslLocal;

    /** */
    private static final List<String> SERVER_CONNECTOR_INFO = new ArrayList<>();

    /**
     * Number of acceptor threads.
     */
    private static int serverAcceptorThreads = 0;

    /** */
    private static DoSFilter doSFilter;

    /** */
    private static InetAccessFilter inetAccessFilter;

    /**
     * ThreadPool parameter information.
     *
     * <a href= "https://wiki.eclipse.org/Jetty/Howto/High_Load#Jetty_Tuning">
     * Jetty/Howto/High Load</a>
     */
    public static class ThreadPoolInfo {
        /**
         * Configure the number of threads according to the webapp. That is, how
         * many threads it needs in order to achieve the best performance.
         * Configure with mind to limiting memory usage maximum available.
         * Typically >50 and <500.
         */
        private static int maxThreads;

        /** */
        private static int minThreads;

        /**
         * <p>
         * It is very important to limit the task queue of Jetty. By default,
         * the queue is unbounded! As a result, if under high load in excess of
         * the processing power of the webapp, jetty will keep a lot of requests
         * on the queue. Even after the load has stopped, Jetty will appear to
         * have stopped responding to new requests as it still has lots of
         * requests on the queue to handle.
         * </p>
         *
         * <p>
         * For a high reliability system, it should reject the excess requests
         * immediately (fail fast) by using a queue with a bounded capability.
         * The capability (maximum queue length) should be calculated according
         * to the "no-response" time tolerable. For example, if the webapp can
         * handle 100 requests per second, and if you can allow it one minute to
         * recover from excessive high load, you can set the queue capability to
         * 60*100=6000. If it is set too low, it will reject requests too soon
         * and can't handle normal load spike.
         * </p>
         */
        private static int queueCapacity;

        /**
         * Maximum time a thread may be idle in ms.
         */
        private static int idleTimeoutMsec;

        /**
         * @return Max threads in the {@link QueuedThreadPool}.
         */
        public static int getMaxThreads() {
            return maxThreads;
        }

        /**
         * @return Min threads in the {@link QueuedThreadPool}.
         */
        public static int getMinThreads() {
            return minThreads;
        }

        /**
         * @return Queue Capacity of the {@link QueuedThreadPool}.
         */
        public static int getQueueCapacity() {
            return queueCapacity;
        }

        /**
         * @return Maximum time a thread may be idle in ms.
         */
        public static int getIdleTimeoutMsec() {
            return idleTimeoutMsec;
        }

        /**
         * @return Log message for Max threads in the {@link QueuedThreadPool}.
         */
        public static String logMaxThreads() {
            return String.format("%s [%s]",
                    ServerPropEnum.THREADPOOL_MAXTHREADS.key(),
                    ThreadPoolInfo.maxThreads);
        }

        /**
         * @return Log message for Min threads in the {@link QueuedThreadPool}.
         */
        public static String logMinThreads() {
            return String.format("%s [%s]",
                    ServerPropEnum.THREADPOOL_MINTHREADS.key(),
                    ThreadPoolInfo.minThreads);
        }

        /**
         * @return Log message for Queue Capacity of the
         *         {@link QueuedThreadPool}.
         */
        public static String logQueueCapacity() {
            return String.format("%s [%s]",
                    ServerPropEnum.THREADPOOL_QUEUE_CAPACITY.key(),
                    ThreadPoolInfo.queueCapacity);
        }

        /**
         * @return Log message for Maximum time a thread may be idle in ms.
         */
        public static String logIdleTimeoutMsec() {
            return String.format("%s [%s]",
                    ServerPropEnum.THREADPOOL_IDLE_TIMEOUT_MSEC.key(),
                    ThreadPoolInfo.idleTimeoutMsec);
        }
    }

    /** */
    private static boolean serverSslRedirect;

    /** */
    private static boolean webAppCustomI18n;

    /** */
    private static int sessionScavengeInterval;

    /** */
    private WebServer() {
    }

    /**
     * @return {@code true} when custom Web App i18n is to be applied.
     */
    public static boolean isWebAppCustomI18n() {
        return webAppCustomI18n;
    }

    /**
     * @return The server host.
     */
    public static String getServerHost() {
        return serverHost;
    }

    /**
     * @return The server port.
     */
    public static int getServerPort() {
        return serverPort;
    }

    /**
     * @return The server SSL port.
     */
    public static int getServerPortSsl() {
        return serverPortSsl;
    }

    /**
     * @return The server SSL port for local access.
     */
    public static int getServerPortSslLocal() {
        return serverPortSslLocal;
    }

    /**
     * @return Log message from the {@link WebServerStartupHook}.
     */
    public static String getStartupHookLogMsg() {
        return theStartupHookLogMsg;
    }

    /**
     * @return Log message with session scavenge in seconds.
     */
    public static String logSessionScavengeInterval() {
        return String.format("%s [%d]",
                ServerPropEnum.SESSION_SCAVENGE_INTERVAL_SEC.key(),
                sessionScavengeInterval);
    }

    /**
     * @return Number of server acceptor threads.
     */
    public static int getServerAcceptorThreads() {
        return serverAcceptorThreads;
    }

    /**
     * @return {@code true} if server access is SSL only.
     */
    public static boolean isSSLOnly() {
        return serverPortSsl > 0 && serverPort == 0;
    }

    /**
     * @return {@code true} if non-SSL port is redirected to SSL port.
     */
    public static boolean isSSLRedirect() {
        return serverSslRedirect;
    }

    /**
     * @return {@code true} if server runs in development environment.
     */
    public static boolean isDeveloperEnv() {
        return developerEnv;
    }

    /**
     * Creates the {@link SslCertInfo}.
     *
     * @param ksLocation
     *            The keystore location.
     * @param ksPassword
     *            The keystore password.
     * @return The {@link SslCertInfo}, or {@code null}. when alias is not
     *         found.
     */
    private static SslCertInfo createSslCertInfo(final String ksLocation,
            final String ksPassword) {

        final File file = new File(ksLocation);

        SslCertInfo certInfo = null;

        try (FileInputStream is = new FileInputStream(file);) {

            final KeyStore keystore =
                    KeyStore.getInstance(KeyStore.getDefaultType());

            keystore.load(is, ksPassword.toCharArray());

            final Enumeration<String> aliases = keystore.aliases();

            /*
             * Get X509 cert and alias with most recent "not after".
             */
            long minNotAfter = Long.MAX_VALUE;
            java.security.cert.X509Certificate minCertX509 = null;
            String minAlias = null;
            int nAliases = 0;

            while (aliases.hasMoreElements()) {

                final String alias = aliases.nextElement();

                final java.security.cert.Certificate cert =
                        keystore.getCertificate(alias);

                if (cert instanceof java.security.cert.X509Certificate) {

                    java.security.cert.X509Certificate certX509 =
                            (java.security.cert.X509Certificate) cert;

                    final long notAfter = certX509.getNotAfter().getTime();
                    if (notAfter < minNotAfter) {
                        minCertX509 = certX509;
                        minAlias = alias;
                    }

                    nAliases++;
                }
            }

            if (minCertX509 != null) {

                final Date creationDate = keystore.getCreationDate(minAlias);
                final Date notAfter = minCertX509.getNotAfter();

                String subjectCN = null;

                final LdapName lnSubject =
                        new LdapName(minCertX509.getSubjectDN().getName());
                for (final Rdn rdn : lnSubject.getRdns()) {
                    if (rdn.getType().equalsIgnoreCase("CN")) {
                        subjectCN = rdn.getValue().toString();
                        break;
                    }
                }

                final LdapName ln =
                        new LdapName(minCertX509.getIssuerDN().getName());
                for (final Rdn rdn : ln.getRdns()) {
                    if (rdn.getType().equalsIgnoreCase("CN")) {
                        final String issuerCN = rdn.getValue().toString();
                        certInfo = new SslCertInfo(issuerCN, subjectCN,
                                creationDate, notAfter, nAliases == 1);
                        break;
                    }
                }
            }

        } catch (KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException | InvalidNameException e) {
            theLogger.error(e.getMessage(), e);
            throw new SpException(e.getMessage(), e);
        }

        if (certInfo != null) {
            try {
                // Reopen to get the subject alt names.
                addCertSubjectAltNames(ksLocation, ksPassword, certInfo);
            } catch (Exception e) {
                theLogger.error(e.getMessage(), e);
                throw new SpException(e.getMessage(), e);
            }
        }

        return certInfo;
    }

    /**
     *
     * @param fileName
     * @param pw
     * @param certInfo
     * @throws Exception
     */
    private static void addCertSubjectAltNames(final String fileName,
            final String pw, final SslCertInfo certInfo) throws Exception {

        final char[] password = pw.toCharArray();

        final Set<String> altNamesSet = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(fileName);) {
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(fis, password);

            final Set<Object> subjAltNames =
                    Collections.list(ks.aliases()).stream().flatMap(alias -> {
                        try {
                            return ((X509Certificate) ks.getCertificate(alias))
                                    .getSubjectAlternativeNames().stream();
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    }).collect(Collectors.toSet());

            for (Object s : subjAltNames) {
                if (s instanceof List<?>) {
                    final List<?> lst = (List<?>) s;
                    if (lst.size() == 2) {
                        // [0] = Integer : 2 = DNS name, 7 = IP Address
                        // [1] = String : value
                        final String value = lst.get(1).toString();
                        altNamesSet.add(value);
                    }
                }
            }
        }
        certInfo.setSubjectAltNames(altNamesSet);
    }

    /**
     * @return {@code true} if Java 11 runtime (or higher).
     */
    private static boolean checkJava11() {
        try {
            // Pick a class method that was introduced in Java 11.
            "check".isBlank();
            return true;
        } catch (Exception e) {
            // no code intended.
        }

        final String msg = //
                "\n+===================================================+"
                        + "\n| PrintFlowLite NOT started: "
                        + "Java 11+ MUST be installed. |"
                        + "\n+========================"
                        + "===========================+";
        System.err.println(new Date().toString() + " : " + msg);
        theLogger.error(msg);
        return false;
    }

    /** */
    private static final int PORT_OFFSET = 1024;

    /**
     * @return {@code true} when ports are valid.
     */
    private static boolean checkPorts() {

        if (getServerPort() > PORT_OFFSET && getServerPortSsl() > PORT_OFFSET
                && getServerPortSslLocal() > PORT_OFFSET) {
            return true;
        }

        final String msg =
                "\n+========================================================+"
                        + "\n| PrintFlowLite NOT started: server ports MUST be GT "
                        + String.valueOf(PORT_OFFSET) + "     |"
                        + "\n+==============================="
                        + "=========================+";
        System.err.println(new Date().toString() + " : " + msg);
        theLogger.error(msg);
        return false;
    }

    /**
     * Add RESTfull servlet.
     *
     * @param context
     *            Web App context.
     */
    private static void initRESTful(final WebAppContext context) {

        final ServletHolder jerseyServlet = context.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class,
                RestApplication.SERVLET_URL_PATTERN);

        jerseyServlet.setInitParameter("javax.ws.rs.Application",
                RestApplication.class.getCanonicalName());
    }

    /**
     * Initializing action when started in development environment.
     */
    private static void initDevelopmenEnv() {

        RestSystemService.test();
        RestTestService.test();

        boolean createDeadlockTest = false;
        if (createDeadlockTest) {
            DeadlockedThreadsDetector.createDeadlockTest();
        }
    }

    /**
     * Creates SSL context factory to configure server-side connector.
     *
     * @return factory.
     */
    private static SslContextFactory.Server createSslContextFactory() {
        /*
         * SSL Context Factory for HTTPS and SPDY.
         *
         * SSL requires a certificate so we configure a factory for ssl contents
         * with information pointing to what keystore the ssl connection needs
         * to know about.
         *
         * Much more configuration is available the ssl context, including
         * things like choosing the particular certificate out of a keystore to
         * be used.
         */
        final SslContextFactory.Server sslContextFactory =
                new SslContextFactory.Server();

        // Mantis #562
        sslContextFactory.addExcludeCipherSuites(
                //
                // weak
                "TLS_RSA_WITH_RC4_128_MD5",
                // weak
                "TLS_RSA_WITH_RC4_128_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                // weak
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                // insecure
                "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"
        //
        );
        return sslContextFactory;
    }

    /**
     * Creates SSL context factory to configure server-side connector for
     * {@link ServerDataFileNameEnum#DEFAULT_SSL_KEYSTORE}.
     *
     * @param serverHomePath
     *            server home
     * @return factory.
     * @throws Exception
     */
    private static SslContextFactory.Server createSslContextFactoryDefault(
            final Path serverHomePath) throws Exception {

        final SslContextFactory.Server sslContextFactory =
                createSslContextFactory();

        final Properties propsPw = new Properties();
        try (InputStream istr = new java.io.FileInputStream(
                ServerDataFileNameEnum.DEFAULT_SSL_KEYSTORE_PW
                        .getPathAbsolute(serverHomePath).toFile());) {
            propsPw.load(istr);
        }

        final String ksPassword = propsPw.getProperty("password");
        sslContextFactory.setKeyStorePassword(ksPassword);

        final String ksLocation = ServerDataFileNameEnum.DEFAULT_SSL_KEYSTORE
                .getPathAbsolute(serverHomePath).toString();

        try (InputStream istr = new java.io.FileInputStream(ksLocation);) {
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(istr, ksPassword.toCharArray());
            sslContextFactory.setKeyStore(ks);
        }

        ConfigManager.setSslCertInfoDefault(
                createSslCertInfo(ksLocation, ksPassword));

        return sslContextFactory;
    }

    /**
     * Creates SSL context factory to configure server-side connector for
     * {@link ServerDataFileNameEnum#DEFAULT_SSL_KEYSTORE}.
     *
     * @param serverHomePath
     *            server home
     * @param propsServer
     *            Server properties from file.
     * @return factory.
     * @throws Exception
     */
    private static SslContextFactory.Server createSslContextFactoryCustom(
            final Path serverHomePath, final Properties propsServer)
            throws Exception {

        final SslContextFactory.Server sslContextFactory =
                createSslContextFactory();

        final String ksLocation = String.format("%s%c%s",
                serverHomePath.toString(), File.separatorChar, propsServer
                        .getProperty(ServerPropEnum.SERVER_SSL_KEYSTORE.key()));

        final String ksPassword = propsServer
                .getProperty(ServerPropEnum.SERVER_SSL_KEYSTORE_PASSWORD.key());

        final Resource keystore = Resource.newResource(ksLocation);
        sslContextFactory.setKeyStoreResource(keystore);

        // Step 1: KeyStore password.
        sslContextFactory.setKeyStorePassword(ksPassword);

        // Step 2: KeyManager password.
        final String kmPassword = propsServer
                .getProperty(ServerPropEnum.SERVER_SSL_KEY_PASSWORD.key());

        if (StringUtils.isNoneBlank(kmPassword)
                && !StringUtils.equals(ksPassword, kmPassword)) {
            sslContextFactory.setKeyManagerPassword(kmPassword);
        }

        ConfigManager.setSslCertInfoCustom(
                createSslCertInfo(ksLocation, ksPassword));

        return sslContextFactory;
    }

    /**
     * Configures {@link DoSFilter} using {@link IConfigProp.Key} value.
     *
     * @param configKey
     *            Key to configure.
     * @throws ConfigException
     *             invalid value.
     */
    public static void configureDoSFilter(final IConfigProp.Key configKey)
            throws ConfigException {
        DoSFilterConfigurator.configure(doSFilter, configKey);
    }

    /**
     * Configures {@link InetAccessFilter} using {@link IConfigProp.Key} value.
     *
     * @param configKey
     *            Key to configure.
     * @throws ConfigException
     *             invalid value.
     */
    public static void configureInetAccessFilter(
            final IConfigProp.Key configKey) throws ConfigException {
        InetFilterConfigurator.configure(inetAccessFilter, configKey);
    }

    /**
     * Adds a connector to the server.
     *
     * @param server
     * @param connector
     * @param customServerHost
     *            if {@code null}, all interfaces are implied.
     * @param port
     *            IP port
     * @param comment
     *            can be {@code null}
     */
    private static void addConnector(final Server server,
            final ServerConnector connector, final String customServerHost,
            final int port, final String comment) {

        if (customServerHost != null) {
            connector.setHost(customServerHost);
        }
        connector.setPort(port);
        connector.setIdleTimeout(ThreadPoolInfo.idleTimeoutMsec);
        server.addConnector(connector);
        serverAcceptorThreads += connector.getAcceptors();

        addServerConnectorInfo(connector, comment);
    }

    /**
     * Adds HTTP default connector to the server.
     *
     * @param server
     * @param httpConfig
     * @param customServerHost
     *            if {@code null}, all interfaces are implied.
     * @param port
     */
    private static void addConnectorHTTPDefault(final Server server,
            final HttpConfiguration httpConfig, final String customServerHost,
            final int port) {

        final ServerConnector connector = new ServerConnector(server,
                new HttpConnectionFactory(httpConfig));

        addConnector(server, connector, customServerHost, port, null);
    }

    /**
     * Create server connector for HTTP/1.1 and HTTP/2.
     *
     * @param server
     * @param sslContextFactory
     *            SslContextFactory with the keyStore information.
     * @param httpsConfig
     *            HTTP configuration object.
     * @param http2Config
     *            HTTP/2 configuration.
     * @return {@link }{@link ServerConnector} instance.
     */
    private static ServerConnector createServerSSLConnector(final Server server,
            final SslContextFactory.Server sslContextFactory,
            final HttpConfiguration httpsConfig,
            final HTTP2Configuration http2Config) {

        // The ConnectionFactory for HTTP/1.1.
        final HttpConnectionFactory http11 =
                new HttpConnectionFactory(httpsConfig);

        final ServerConnector connector;

        if (http2Config.isEnabled()) {

            // The ConnectionFactory for HTTP/2.
            final HTTP2ServerConnectionFactory h2 =
                    new HTTP2ServerConnectionFactory(httpsConfig);

            h2.setRateControlFactory(new HTTP2RateControl.Factory(http2Config));

            // The ALPN ConnectionFactory.
            final ALPNServerConnectionFactory alpn =
                    new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol(http11.getProtocol());

            // The ConnectionFactory for TLS.
            final SslConnectionFactory tls = new SslConnectionFactory(
                    sslContextFactory, alpn.getProtocol());

            connector = new ServerConnector(server, tls, alpn, h2, http11);

        } else {
            connector = new ServerConnector(server, sslContextFactory, http11);
        }

        return connector;
    }

    /**
     * Adds SSL connector to server.
     *
     * @param server
     * @param sslContextFactory
     * @param httpsConfig
     * @param customServerHost
     *            if {@code null}, all interfaces are implied.
     * @param port
     *            SSL port.
     * @param http2Configuration
     *            HTTP/2 configuration.
     * @param comment
     */
    private static void addConnectorSSL(final Server server,
            final SslContextFactory.Server sslContextFactory,
            final HttpConfiguration httpsConfig, final String customServerHost,
            final int port, final HTTP2Configuration http2Configuration,
            final String comment) {

        final ServerConnector connector = createServerSSLConnector(server,
                sslContextFactory, httpsConfig, http2Configuration);

        addConnector(server, connector, customServerHost, port, comment);
    }

    /**
     * @param conn
     *            connector
     * @param comment
     *            can be {@code null}
     */
    public static void addServerConnectorInfo(final ServerConnector conn,
            final String comment) {
        SERVER_CONNECTOR_INFO.add(
                String.format("connected to interface [%s] on port %d (%s) %s",
                        conn.getHost() == null ? "all" : conn.getHost(),
                        conn.getPort(), String.join(",", conn.getProtocols()),
                        StringUtils.defaultString(comment)));
    }

    /**
     * @return info string for each {@link ServerConnector}
     */
    public static List<String> getServerConnectorInfo() {
        return SERVER_CONNECTOR_INFO;
    }

    /**
     * Starts the Web Server.
     * <p>
     * References:
     * </p>
     * <ul>
     * <li>Jetty: <a href="See:
     * https://www.eclipse.org/jetty/documentation/current/using-annotations
     * .html">Working with Annotations</a></li>
     * </ul>
     *
     * @param args
     *            The arguments.
     * @throws Exception
     *             When unexpected things happen.
     */
    public static void main(final String[] args) throws Exception {
        /*
         * Before doing anything else: check presence of (lazy create) essential
         * data files, like log4j.properties.
         */
        theStartupHookLogMsg = WebServerStartupHook.run();

        // Now we know log4j.properties is present, create the logger.
        theLogger = LoggerFactory.getLogger(WebServer.class);
        // ... and other log4j dependent classes.
        doSFilter = new DoSFilter();
        inetAccessFilter = InetAccessFilter.instance();

        ConfigManager.initJavaUtilLogging();

        if (!checkJava11()) {
            return;
        }

        // (1) Load from file and apply system environment.
        final Properties propsServer = ConfigManager.createServerProperties();

        // (2) Check custom SSL keystore.
        final boolean hasCustomSslKeystore =
                ConfigManager.isSslCustomKeystorePresent();

        // (3) Check custom server host.
        serverHost = StringUtils
                .strip(ServerPropEnum.SERVER_HOST.getProperty(propsServer));
        final boolean hasCustomServerHost = StringUtils.isNotBlank(serverHost);

        // (4) Notify WebApp.
        WebApp.setServerProps(propsServer);
        WebApp.loadWebProperties();

        /*
         * Server ports.
         */
        serverPort = ServerPropEnum.SERVER_PORT.getPropertyInt(propsServer);
        serverPortSsl =
                ServerPropEnum.SERVER_SSL_PORT.getPropertyInt(propsServer);
        serverPortSslLocal =
                Integer.valueOf(ConfigManager.getServerSslPortLocal());

        if (!checkPorts()) {
            return;
        }

        /*
         * Check if ports are in use.
         */
        boolean portsInUse = false;

        for (final int port : new int[] { serverPort, serverPortSsl,
                serverPortSslLocal }) {
            if (InetUtils.isPortInUse(port)) {
                portsInUse = true;
                System.err.println(String.format("Port [%d] is in use.", port));
            }
        }
        if (portsInUse) {
            System.err.println(String.format("%s not started.",
                    CommunityDictEnum.PrintFlowLite.getWord()));
            System.exit(-1);
            return;
        }

        serverSslRedirect =
                !isSSLOnly() && ServerPropEnum.SERVER_HTML_REDIRECT_SSL
                        .getPropertyBoolean(propsServer);

        webAppCustomI18n = ServerPropEnum.WEBAPP_CUSTOM_I18N
                .getPropertyBoolean(propsServer);

        sessionScavengeInterval =
                Integer.parseInt(ServerPropEnum.SESSION_SCAVENGE_INTERVAL_SEC
                        .getProperty(propsServer));

        ThreadPoolInfo.queueCapacity =
                Integer.parseInt(ServerPropEnum.THREADPOOL_QUEUE_CAPACITY
                        .getProperty(propsServer));

        if (ThreadPoolInfo.queueCapacity <= 0) {
            System.err.println(String.format(
                    "%s not started: %s [%d] is invalid "
                            + "(capacity must be GT zero, "
                            + "and can't be unbounded).",
                    CommunityDictEnum.PrintFlowLite.getWord(),
                    ServerPropEnum.THREADPOOL_QUEUE_CAPACITY.key(),
                    ThreadPoolInfo.queueCapacity));
            System.exit(-1);
            return;
        }

        ThreadPoolInfo.maxThreads = Integer.parseInt(
                ServerPropEnum.THREADPOOL_MAXTHREADS.getProperty(propsServer));

        ThreadPoolInfo.minThreads = Integer.parseInt(
                ServerPropEnum.THREADPOOL_MINTHREADS.getProperty(propsServer));

        ThreadPoolInfo.idleTimeoutMsec =
                Integer.parseInt(ServerPropEnum.THREADPOOL_IDLE_TIMEOUT_MSEC
                        .getProperty(propsServer));

        final QueuedThreadPool threadPool;

        /*
         * https://wiki.eclipse.org/Jetty/Howto/High_Load#Jetty_Tuning
         *
         * The number of acceptors is calculated by Jetty based of number of
         * available CPU cores.
         */
        if (ThreadPoolInfo.queueCapacity < 0) {
            threadPool = new QueuedThreadPool(ThreadPoolInfo.maxThreads,
                    ThreadPoolInfo.minThreads, ThreadPoolInfo.idleTimeoutMsec);
        } else {
            threadPool = new QueuedThreadPool(ThreadPoolInfo.maxThreads,
                    ThreadPoolInfo.minThreads, ThreadPoolInfo.idleTimeoutMsec,
                    new ArrayBlockingQueue<>(ThreadPoolInfo.queueCapacity));
        }

        threadPool.setName("jetty-threadpool");

        final Server server = new Server(threadPool);
        // First thing to do.
        Runtime.getRuntime().addShutdownHook(new WebServerShutdownHook(server));

        /*
         * This is needed to enable the Jetty annotations.
         */
        final org.eclipse.jetty.webapp.Configurations annotateConfig =
                org.eclipse.jetty.webapp.Configurations
                        .setServerDefault(server);
        annotateConfig.add(//
                org.eclipse.jetty.webapp.JettyWebXmlConfiguration.class
                        .getName(),
                org.eclipse.jetty.annotations.AnnotationConfiguration.class
                        .getName());

        /*
         * HttpConfiguration is a collection of configuration information
         * appropriate for http and https.
         *
         * The default scheme for http is <code>http</code> of course, as the
         * default for secured http is <code>https</code> but we show setting
         * the scheme to show it can be done.
         *
         * The port for secured communication is also set here.
         */
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme(InetUtils.URL_PROTOCOL_HTTPS);
        httpConfig.setSecurePort(serverPortSsl);
        // Customize Requests for Proxy Forwarding.
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        if (!isSSLOnly()) {

            if (hasCustomServerHost) {
                addConnectorHTTPDefault(server, httpConfig, serverHost,
                        serverPort);
                addConnectorHTTPDefault(server, httpConfig,
                        InetUtils.LOCAL_HOST, serverPort);
            } else {
                addConnectorHTTPDefault(server, httpConfig, null, serverPort);
            }
        }

        final Path serverHomePath = ConfigManager.getServerHomePath();

        final SslContextFactory.Server sslContextFactoryDefault =
                createSslContextFactoryDefault(serverHomePath);

        /*
         * HTTPS Configuration
         *
         * A new HttpConfiguration object is needed for the next connector and
         * you can pass the old one as an argument to effectively clone the
         * contents.
         *
         * On this HttpConfiguration object we add a SecureRequestCustomizer
         * which is how a new connector is able to resolve the https connection
         * before handing control over to the Jetty Server.
         */
        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        // Customize Requests for Proxy Forwarding.
        httpsConfig.addCustomizer(new ForwardedRequestCustomizer());

        // HTTP/2 configuration properties.
        final HTTP2Configuration http2Configuration =
                new HTTP2Configuration(propsServer);
        /*
         * Default HTTPS connector.
         */
        final String commentSelfSigned = "(Self-signed SSL)";
        if (hasCustomServerHost) {
            if (!hasCustomSslKeystore) {
                addConnectorSSL(server, sslContextFactoryDefault, httpsConfig,
                        serverHost, serverPortSslLocal, http2Configuration,
                        commentSelfSigned);
            }
            addConnectorSSL(server, sslContextFactoryDefault, httpsConfig,
                    InetUtils.LOCAL_HOST, serverPortSslLocal,
                    http2Configuration, commentSelfSigned);
        } else {
            addConnectorSSL(server, sslContextFactoryDefault, httpsConfig, null,
                    serverPortSslLocal, http2Configuration, commentSelfSigned);
        }

        /*
         * Custom HTTPS connector?
         */
        if (hasCustomSslKeystore) {
            final SslContextFactory.Server sslContextFactoryCustom =
                    createSslContextFactoryCustom(serverHomePath, propsServer);
            addConnectorSSL(server, sslContextFactoryCustom, httpsConfig,
                    serverHost, serverPortSsl, http2Configuration,
                    "(External SSL)");
        }

        /*
         * Set filters/handlers
         */
        final WebAppContext webAppContext = new WebAppContext();

        webAppContext.addFilter(new FilterHolder(inetAccessFilter), "/*",
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        webAppContext.addFilter(new FilterHolder(doSFilter), "/*",
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        webAppContext.setServer(server);
        webAppContext.setContextPath("/");

        developerEnv = SystemPropertyEnum.PRINTFLOWLITE_WAR_FILE.getValue() == null;

        final String pathToWarFile;

        if (developerEnv) {
            pathToWarFile = "src/main/webapp";
        } else {
            pathToWarFile = Path.of(
                    ServerFilePathEnum.LIB.getPathAbsolute(serverHomePath)
                            .toString(),
                    SystemPropertyEnum.PRINTFLOWLITE_WAR_FILE.getValue()).toString();
        }

        webAppContext.setWar(pathToWarFile);

        /*
         * This is needed for scanning "discoverable" Jetty annotations. The
         * "/classes/.*" scan is needed when running in development (Eclipse).
         * The "/PrintFlowLite-server-*.jar$" scan in needed for production.
         */
        webAppContext.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/PrintFlowLite-server-[^/]*\\.jar$|.*/classes/.*");

        /*
         * Handlers ...
         */
        final ArrayList<Handler> handlerArrayList = new ArrayList<>();

        if (serverSslRedirect) {
            SERVER_CONNECTOR_INFO.add("redirect to SSL");
            handlerArrayList.add(new RedirectToSSLHandler());
        }
        handlerArrayList.add(webAppContext);

        final Handler[] handlerArray =
                handlerArrayList.toArray(new Handler[handlerArrayList.size()]);

        /*
         * Set cookies to HttpOnly.
         */
        webAppContext.getSessionHandler().getSessionCookieConfig()
                .setHttpOnly(true);

        // Override default session cookie.
        webAppContext.getSessionHandler()
                .setSessionCookie(SERVER_SESSION_COOKIE);

        /*
         * Set the handler(s).
         */
        final HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(handlerArray);

        server.setHandler(handlerList);

        /*
         * BASIC Authentication for Atom Feed and PaperCut User Syn/Auth
         * Interface.
         */
        final LoginService basicAuthLoginService = new BasicAuthLoginService(
                new String[] { AtomFeedServlet.ROLE_ALLOWED,
                        ExtPaperCutSyncServlet.ROLE_ALLOWED });

        server.addBean(basicAuthLoginService);

        /*
         * See web.xml:
         * <login-config><auth-method>BASIC</auth-method></login-config>
         */
        webAppContext.getSecurityHandler()
                .setLoginService(basicAuthLoginService);

        // Add RESTfull servlet.
        initRESTful(webAppContext);

        //
        final File serverStartedFile = ServerFileNameEnum.SERVER_STARTED_TXT
                .getPathAbsolute(serverHomePath).toFile();

        int status = 0;

        try (FileWriter writer = new FileWriter(serverStartedFile);) {
            /*
             * Writing the time we started in a file. This file is monitored by
             * the install script to see when the server has started.
             */

            final Date now = new Date();

            writer.write("#");
            writer.write(now.toString());
            writer.write("\n");
            writer.write(String.valueOf(now.getTime()));
            writer.write("\n");

            writer.flush();

            /*
             * Start the server: WebApp is initialized.
             */
            server.start();

            // ... after start() !
            ConfigManager.setDoSFilterStatistics(DoSFilterMonitor.instance());
            doSFilter.setListener(DoSFilterMonitor.instance());

            InetFilterConfigurator.configureAll(inetAccessFilter);
            DoSFilterConfigurator.configureAll(doSFilter);
            http2Configuration.log();

            server.getSessionIdManager().getSessionHouseKeeper()
                    .setIntervalSec(sessionScavengeInterval);

            if (WebApp.hasInitializeError()) {
                System.exit(1);
                return;
            }

            if (!developerEnv) {
                server.join();
            }

        } catch (Exception e) {
            theLogger.error(e.getMessage(), e);
            status = 1;
        }

        if (status == 0) {

            if (theLogger.isInfoEnabled()) {
                theLogger.info("server [" + server.getState() + "]");
            }

            if (developerEnv) {

                try {
                    initDevelopmenEnv();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                System.out
                        .println(" \n+========================================"
                                + "====================================+"
                                + "\n| You're running in development mode. "
                                + "Click in this console and press ENTER. |"
                                + "\n| This will call System.exit() so the "
                                + "shutdown routine is executed.          |"
                                + "\n+====================================="
                                + "=======================================+"
                                + "\n");
                try {

                    System.in.read();
                    System.exit(0);

                } catch (Exception e) {
                    theLogger.error(e.getMessage(), e);
                }
            }
        }
    }
}
