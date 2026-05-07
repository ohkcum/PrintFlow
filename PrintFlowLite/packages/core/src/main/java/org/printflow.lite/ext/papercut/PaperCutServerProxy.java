/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.ext.papercut;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcHttpTransportException;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.circuitbreaker.CircuitBreaker;
import org.printflow.lite.core.circuitbreaker.CircuitBreakerException;
import org.printflow.lite.core.circuitbreaker.CircuitBreakerOperation;
import org.printflow.lite.core.circuitbreaker.CircuitNonTrippingException;
import org.printflow.lite.core.circuitbreaker.CircuitTrippingException;
import org.printflow.lite.core.config.CircuitBreakerEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.users.CommonUser;
import org.printflow.lite.core.users.CommonUserGroup;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.core.util.RetryException;
import org.printflow.lite.core.util.RetryExecutor;
import org.printflow.lite.core.util.RetryTimeoutException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutServerProxy {

    /**
     * .
     */
    private final XmlRpcClient xmlRpcClient;

    /**
     * .
     */
    private final String authToken;

    /**
     * {@code true} if a {@link CircuitBreaker} is used to signal PaperCut
     * connection status.
     */
    private boolean useCircuitBreaker;

    /**
     * PaperCut: "Batching in groups of 1000 ensures efficient transfer and
     * processing".
     */
    private static final int API_BATCHSIZE = 1000;

    /**
     * Reserved User Groups.
     */
    private static final Set<String> RESERVED_USER_GROUPS =
            new HashSet<>(Arrays.asList("!!All Users!!", "!!Internal Users!!"));

    /**
     * .
     */
    private static final CircuitBreaker CIRCUIT_BREAKER = ConfigManager
            .getCircuitBreaker(CircuitBreakerEnum.PAPERCUT_CONNECTION);

    /** */
    private enum XMLMethod {
        /** */
        ADD_NEW_SHARED_ACCOUNT("api.addNewSharedAccount"),
        /** */
        ADJUST_SHARED_ACCOUNT_ACCOUNT_BALANCE(
                "api.adjustSharedAccountAccountBalance"),
        /** */
        ADJUST_USER_ACCOUNT_BALANCE("api.adjustUserAccountBalance"),
        /** */
        ADJUST_USER_ACCOUNT_BALANCE_IF_AVAILABLE(
                "api.adjustUserAccountBalanceIfAvailable"),
        /** */
        GET_GROUP_MEMBERS("api.getGroupMembers"),
        /** */
        GET_USER_ACCOUNT_BALANCE("api.getUserAccountBalance"),
        /** */
        GET_USER_PROPERTIES("api.getUserProperties"),
        /** */
        IS_GROUP_EXISTS("api.isGroupExists"),
        /** */
        IS_SHARED_ACCOUNT_EXISTS("api.isSharedAccountExists"),
        /** */
        IS_USER_EXISTS("api.isUserExists"),
        /** */
        LIST_USER_GROUPS("api.listUserGroups"),
        /** */
        LIST_USER_ACCOUNTS("api.listUserAccounts");

        /** */
        private final String methodName;

        /**
         * @param name
         *            The method name.
         */
        XMLMethod(final String name) {
            this.methodName = name;
        }

        /**
         * @return The method name.
         */
        public String methodName() {
            return this.methodName;
        }

    }

    /**
     * .
     */
    private enum UserProperty {

        /**
         * The user's balance, unformatted, e.g. "1234.56".
         */
        BALANCE("balance"),
        /**
         * {@code true} if the user's printing is disabled, otherwise false.
         */
        DISABLED_PRINT("disabled-print"),
        /** */
        EMAIL("email"),
        /** */
        FULL_NAME("full-name"),
        /** */
        INTERNAL("internal"),
        /** */
        NOTES("notes"),
        /** */
        OFFICE("office"),
        /** */
        PRIMARY_CARD_NUMBER("primary-card-number"),
        /** */
        PRINT_JOB_COUNT("print-stats.job-count"),
        /** */
        PRINT_PAGE_COUNT("print-stats.page-count"),
        /**
         * {@code true} if this user's printing is restricted, false if they are
         * unrestricted.
         */
        RESTRICTED("restricted");

        /** */
        private final String propName;

        /**
         * @param name
         *            The property name.
         */
        UserProperty(final String name) {
            this.propName = name;
        }

        /**
         * @return The property name.
         */
        public String getPropName() {
            return this.propName;
        }
    }

    /**
     * A {@link CircuitBreakerOperation} wrapper for the
     * {@link PaperCutProxy#execute(String, Vector))} method.
     *
     */
    private static class PaperCutCircuitBreakerOperation
            implements CircuitBreakerOperation {

        /** */
        private final PaperCutServerProxy paperCutProxy;
        /** */
        private final XMLMethod xmlMethod;
        /** */
        private final Vector<Object> parameters;

        /**
         *
         * @param serverProxy
         * @param method
         *            {@link XMLMethod}
         * @param parms
         *            parameters
         */
        PaperCutCircuitBreakerOperation(final PaperCutServerProxy serverProxy,
                final XMLMethod method, final Vector<Object> parms) {
            this.paperCutProxy = serverProxy;
            this.xmlMethod = method;
            this.parameters = parms;
        }

        @Override
        public Object execute(final CircuitBreaker circuitBreaker) {

            try {
                return this.paperCutProxy.execute(this.xmlMethod,
                        this.parameters);
            } catch (PaperCutException e) {
                throw new CircuitNonTrippingException(e.getMessage(), e);
            } catch (PaperCutConnectException e) {
                throw new CircuitTrippingException(e.getMessage(), e);
            }
        }

    };

    /**
     * The constructor.
     *
     * @param apiClient
     *            The {@link XmlRpcClient}.
     * @param authToken
     *            The authentication token as a string. All RPC calls must pass
     *            through an authentication token. At the current time this is
     *            simply the built-in "admin" user's password.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     */
    private PaperCutServerProxy(final XmlRpcClient apiClient,
            final String authToken, final boolean useCircuitBreaker) {
        this.authToken = authToken;
        this.xmlRpcClient = apiClient;
        this.useCircuitBreaker = useCircuitBreaker;
    }

    /**
     * Creates a {@link PaperCutServerProxy} instance from the application
     * configuration.
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutServerProxy} instance.
     */
    public static PaperCutServerProxy create(final ConfigManager cm,
            final boolean useCircuitBreaker) {

        return PaperCutServerProxy.create(
                cm.getConfigValue(Key.PAPERCUT_SERVER_HOST),
                cm.getConfigInt(Key.PAPERCUT_SERVER_PORT),
                cm.getConfigValue(Key.PAPERCUT_XMLRPC_URL_PATH),
                cm.getConfigValue(Key.PAPERCUT_SERVER_AUTH_TOKEN),
                useCircuitBreaker);
    }

    /**
     * Creates a {@link PaperCutServerProxy} instance.
     *
     * @param server
     *            The name or IP address of the server hosting the Application
     *            Server. The server should be configured to allow XML-RPC
     *            connections from the host running this proxy class.
     * @param port
     *            The port the Application Server is listening on. This is port
     *            9191 on a default install.
     * @param urlPath
     *            The URL path. E.g. {@code /rpc/api/xmlrpc}
     * @param authToken
     *            The authentication token as a string.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutServerProxy} instance.
     */
    public static PaperCutServerProxy create(final String server,
            final int port, final String urlPath, final String authToken,
            final boolean useCircuitBreaker) {

        final XmlRpcClient xmlRpcClient;

        try {

            final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

            final URL serverUrl =
                    new URL(InetUtils.URL_PROTOCOL_HTTP, server, port, urlPath);

            config.setServerURL(serverUrl);

            xmlRpcClient = new XmlRpcClient();
            xmlRpcClient.setConfig(config);

        } catch (MalformedURLException e) {
            throw new SpException("Invalid server name supplied");
        }

        return new PaperCutServerProxy(xmlRpcClient, authToken,
                useCircuitBreaker);
    }

    /**
     * Executes XML-RPC method.
     *
     * @param xmlMethod
     * @param parameters
     * @return result object
     * @throws PaperCutException
     */
    private Object execute(final XMLMethod xmlMethod,
            final Vector<Object> parameters) throws PaperCutException {

        Object result = null;

        try {
            result = this.xmlRpcClient.execute(xmlMethod.methodName(),
                    parameters);

        } catch (XmlRpcHttpTransportException e) {
            /*
             * If request could not be processed by the server, e.g. because the
             * URL path is invalid.
             */
            throw new PaperCutConnectException(e.getMessage(), e);

        } catch (XmlRpcClientException e) {
            /*
             * The invocation failed on the client side(for example if
             * communication with the server failed, e.g. because host cannot be
             * found).
             */
            throw new PaperCutConnectException(e.getMessage(), e);

        } catch (XmlRpcException e) {

            if (e.linkedException instanceof UnknownHostException) {
                throw new PaperCutConnectException(
                        String.format("Unknown host [%s]",
                                e.linkedException.getMessage()),
                        e);
            }

            if (e.linkedException instanceof ConnectException) {
                throw new PaperCutConnectException(
                        String.format("%s", e.linkedException.getMessage()), e);
            }

            /*
             * The invocation failed on the remote side (for example, an
             * exception was thrown within the server)
             */
            String msg = e.getMessage();
            /*
             * Format message so it's a little cleaner. Remove any class
             * definitions.
             */
            msg = msg.replaceAll("\\w+(?:\\.+\\w+)+:\\s?", "");
            throw new PaperCutException(msg, e);
        }
        return result;
    }

    /**
     * Calls the XML-RPC method on the server.
     *
     * @param xmlMethod
     *            The {@link XMLMethod} to execute.
     * @param parameters
     *            The parameters to the method.
     * @return The value returned from the method.
     * @throws PaperCutException
     *             If PaperCut Server encountered an error during execution of
     *             the RPC.
     */
    private Object call(final XMLMethod xmlMethod,
            final Vector<Object> parameters) throws PaperCutException {

        if (!this.useCircuitBreaker) {
            return this.execute(xmlMethod, parameters);
        }

        final CircuitBreakerOperation operation =
                new PaperCutCircuitBreakerOperation(this, xmlMethod,
                        parameters);

        try {

            return CIRCUIT_BREAKER.execute(operation);

        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (CircuitNonTrippingException e) {
            throw new PaperCutException(e.getMessage(), e);
        }

    }

    /**
     * Creates a {@link Vector} to be used in {@link #call(String, Vector)} with
     * {@link #authToken} added.
     *
     * @return {@link Vector}
     */
    private Vector<Object> createParams() {
        final Vector<Object> params = new Vector<Object>();
        params.add(this.authToken);
        return params;
    }

    /**
     * Tests if a user exists.
     *
     * @param username
     *            The user name to test.
     * @return {@code true} if the user exists in the system, otherwise
     *         {@code false}.
     */
    public boolean isUserExists(final String username) {

        final Vector<Object> params = this.createParams();
        params.add(username);

        Boolean exist;
        try {
            exist = (Boolean) this.call(XMLMethod.IS_USER_EXISTS, params);
        } catch (PaperCutException e) {
            exist = Boolean.FALSE;
        }
        return exist.booleanValue();
    }

    /**
     * Is supposed to test if user is member of a group. However, this method is
     * NOT supported and throws an {@link UnsupportedOperationException.}
     *
     * @param userName
     *            user name
     * @param groupName
     *            group name
     * @return n/a
     * @throws UnsupportedOperationException
     */
    public boolean isGroupMemberExists(final String userName,
            final String groupName) {
        throw new UnsupportedOperationException("No API available");
    }

    /**
     * Tests if a group exists.
     *
     * @param groupname
     *            The group name to test.
     * @return {@code true} if the group exists in the system, otherwise
     *         {@code false}.
     */
    public boolean isGroupExists(final String groupname) {

        final Vector<Object> params = this.createParams();
        params.add(groupname);

        Boolean exist;
        try {
            exist = (Boolean) this.call(XMLMethod.IS_GROUP_EXISTS, params);
        } catch (PaperCutException e) {
            exist = Boolean.FALSE;
        }
        return exist.booleanValue();
    }

    /**
     * Test to see if a shared account exists.
     *
     * @param accountName
     *            The name of the shared account.
     * @return Return true if the shared account exists, else false.
     */
    public boolean isSharedAccountExists(final String accountName) {

        final Vector<Object> params = this.createParams();

        params.add(accountName);

        Boolean exist;

        try {
            exist = (Boolean) this.call(XMLMethod.IS_SHARED_ACCOUNT_EXISTS,
                    params);
        } catch (PaperCutException e) {
            exist = Boolean.FALSE;
        }

        return exist.booleanValue();
    }

    /**
     * Connects to PaperCut.
     *
     * @throws PaperCutConnectException
     *             If connect fails.
     */
    public void connect() {
        this.isUserExists("some-dummy-user");
    }

    /**
     * Waits till connected to PaperCut. This method tries to connect after a
     * delay, every "interval", till connected or a timeout occurs.
     *
     * @param delay
     *            The delay (milliseconds) before the first attempt is made.
     * @param interval
     *            The attempt interval (milliseconds).
     * @param timeout
     *            The timeout (milliseconds).
     * @throws Exception
     *             if error.
     * @throws RetryTimeoutException
     *             if timeout.
     */
    public void connect(final long delay, final long interval,
            final long timeout) throws RetryTimeoutException, Exception {

        final PaperCutServerProxy proxy = this;

        final RetryExecutor exec = new RetryExecutor() {

            private int nAttemps = 0;

            @Override
            protected void attempt() throws RetryException, Exception {
                try {
                    proxy.connect();
                    if (nAttemps > 0) {
                        SpInfo.instance().log("Connected to PaperCut.");
                    }
                } catch (PaperCutConnectException e) {
                    if (nAttemps == 0) {
                        SpInfo.instance().log("Connecting to PaperCut...");
                    }
                    nAttemps++;
                    throw new RetryException(e);
                }
            }
        };

        final boolean savedBreakerUsage = this.isUseCircuitBreaker();

        this.setUseCircuitBreaker(false);

        try {
            exec.execute(delay, interval, timeout);
        } finally {
            this.setUseCircuitBreaker(savedBreakerUsage);
        }
    }

    /**
     * Disconnects from PaperCut.
     */
    public void disconnect() {
        // no code intended
    }

    /**
     * Tests the PaperCut server connection.
     * <p>
     * Note: No {@link CircuitBreakerOperation} is used.
     * </p>
     *
     * @return {@code null} If connection succeeded, otherwise a string with the
     *         error message.
     */
    public String testConnection() {

        String error = null;

        try {
            final Vector<Object> params = this.createParams();
            params.add("some-dummy-user");

            this.execute(XMLMethod.IS_USER_EXISTS, params);

        } catch (Exception e) {
            /*
             * Make sure a non-null error message is produced.
             */
            error = "" + e.getMessage();
        }

        return error;
    }

    /**
     * Gets the {@link PaperCutUser}.
     *
     * @param userName
     *            The unique name of the user.
     * @return The {@link PaperCutUser} or {@code null} if the user does not
     *         exist.
     */
    public PaperCutUser getUser(final String userName) {

        final Map<UserProperty, String> props = this.getUserProps(userName);
        final PaperCutUser user;

        if (props == null) {
            user = null;
        } else {
            user = new PaperCutUser();

            user.setInternal(
                    Boolean.parseBoolean(props.get(UserProperty.INTERNAL)));

            user.setDisabledPrint(Boolean
                    .parseBoolean(props.get(UserProperty.DISABLED_PRINT)));

            user.setRestricted(
                    Boolean.parseBoolean(props.get(UserProperty.RESTRICTED)));

            user.setFullName(props.get(UserProperty.FULL_NAME));
            user.setEmail(props.get(UserProperty.EMAIL));

            user.setBalance(
                    Double.parseDouble(props.get(UserProperty.BALANCE)));

            user.setEnabled(true);

            user.setCardNumber(props.get(UserProperty.PRIMARY_CARD_NUMBER));
        }

        return user;
    }

    /**
     * @param groupName
     * @return {@link CommonUserGroup}
     */
    public CommonUserGroup getUserGroup(final String groupName) {
        /*
         * No API available, use "name" for "full name".
         */
        return new CommonUserGroup(groupName, groupName);
    }

    /**
     * Gets the {@link CommonUser}.
     *
     * @param userName
     *            The unique name of the user.
     * @return The {@link CommonUser} or {@code null} if the user does not
     *         exist.
     */
    public CommonUser getCommonUser(final String userName) {

        final PaperCutUser pcUser = this.getUser(userName);
        final CommonUser commonUser;

        if (pcUser == null) {
            commonUser = null;
        } else {

            commonUser = new CommonUser();

            commonUser.setUserName(userName);
            commonUser.setIdNumber(null);
            commonUser.setCardNumber(pcUser.getCardNumber());
            commonUser.setEmail(pcUser.getEmail());
            commonUser.setEnabled(pcUser.isEnabled());
            commonUser.setFullName(pcUser.getFullName());
        }
        return commonUser;
    }

    /**
     * Gets all user properties.
     *
     * @param userName
     *            The name of the user.
     * @return The property values or {@code null} if the user does not exist.
     */
    private Map<UserProperty, String> getUserProps(final String userName) {
        return this.getUserProps(userName, UserProperty.values());
    }

    /**
     * Gets multiple user properties at once (to save multiple calls).
     *
     * @param userName
     *            The name of the user.
     * @param userProperties
     *            The names of the properties to get.
     * @return The property values or {@code null} if the user does not exist.
     */
    private Map<UserProperty, String> getUserProps(final String userName,
            final UserProperty[] userProperties) {

        final Vector<String> propertyNames = new Vector<>();

        for (final UserProperty prop : userProperties) {
            propertyNames.add(prop.getPropName());
        }

        final Vector<Object> params = this.createParams();

        params.add(userName);
        params.add(propertyNames);

        Object[] propertyValues;

        try {
            propertyValues =
                    (Object[]) this.call(XMLMethod.GET_USER_PROPERTIES, params);
        } catch (PaperCutException e) {
            return null;
        }

        final EnumMap<UserProperty, String> enumMap =
                new EnumMap<UserProperty, String>(UserProperty.class);

        int i = 0;
        for (final Object value : propertyValues) {
            enumMap.put(userProperties[i++], value.toString());
        }
        return enumMap;
    }

    /**
     * Composes a single account name from a top and sub account name.
     * <p>
     * A '\' to denote a subaccount, e.g.: 'top\sub'.
     * </p>
     *
     * @param topAccountName
     *            The top account name.
     * @param subAccountName
     *            The sub account name.
     * @return The composes name.
     */
    public String composeSharedAccountName(final String topAccountName,
            final String subAccountName) {

        final StringBuilder accountName = new StringBuilder(topAccountName);

        if (subAccountName != null) {
            accountName.append('\\').append(subAccountName);
        }
        return accountName.toString();
    }

    /**
     * Adjust a shared account's account balance by an adjustment amount. An
     * adjustment bay be positive (add to the account) or negative (subtract
     * from the account).
     *
     * @param topAccountName
     *            The full name of the top shared account to adjust.
     * @param subAccountName
     *            The full name of the sub shared account to adjust (can be
     *            {@code null}.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to associated with the transaction.
     *            This may be a null string.
     * @throws PaperCutException
     *             If the accountName does not exist.
     */
    public void adjustSharedAccountAccountBalance(final String topAccountName,
            final String subAccountName, final double adjustment,
            final String comment) throws PaperCutException {

        final Vector<Object> params = this.createParams();
        params.add(composeSharedAccountName(topAccountName, subAccountName));
        params.add(adjustment);
        params.add(StringUtils.trimToEmpty(comment));
        this.call(XMLMethod.ADJUST_SHARED_ACCOUNT_ACCOUNT_BALANCE, params);
    }

    /**
     * Adjust a user's account balance by an adjustment amount. An adjustment
     * may be positive (add to the user's account) or negative (subtract from
     * the account).
     *
     * @param username
     *            The username associated with the user who's account is to be
     *            adjusted.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to be associated with the transaction.
     *            This may be a null string.
     * @param accountName
     *            Optional name of the user's personal account. If {@code null}
     *            or empty, the built-in default account is used. If multiple
     *            personal accounts is enabled the account name must be
     *            provided.
     * @throws PaperCutException
     *             If the user (account) does not exist.
     */
    public void adjustUserAccountBalance(final String username,
            final double adjustment, final String comment,
            final String accountName) throws PaperCutException {
        this.adjustUserAccountBalanceApi(XMLMethod.ADJUST_USER_ACCOUNT_BALANCE,
                username, adjustment, comment, accountName);
    }

    /**
     * Adjust a user's account balance if there is enough credit available. An
     * adjustment may be positive (add to the user's account) or negative
     * (subtract from the account).
     *
     * @param username
     *            The username associated with the user who's account is to be
     *            adjusted.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to be associated with the transaction.
     *            This may be a null string.
     * @param accountName
     *            Optional name of the user's personal account. If {@code null}
     *            or empty, the built-in default account is used. If multiple
     *            personal accounts is enabled the account name must be
     *            provided.
     * @return {@code false} if balance was not available.
     * @throws PaperCutException
     *             If the user (account) does not exist.
     */
    public boolean adjustUserAccountBalanceIfAvailable(final String username,
            final double adjustment, final String comment,
            final String accountName) throws PaperCutException {
        return this.adjustUserAccountBalanceApi(
                XMLMethod.ADJUST_USER_ACCOUNT_BALANCE_IF_AVAILABLE, username,
                adjustment, comment, accountName);
    }

    /**
     * @param xmlMethod
     *            {@link XMLMethod}
     * @param username
     * @param adjustment
     * @param comment
     * @param accountName
     * @throws PaperCutException
     *
     * @return {@code false} if balance was not adjusted.
     */
    public boolean adjustUserAccountBalanceApi(final XMLMethod xmlMethod,
            final String username, final double adjustment,
            final String comment, final String accountName)
            throws PaperCutException {

        final Vector<Object> params = this.createParams();

        params.add(username);
        params.add(adjustment);
        params.add(StringUtils.trimToEmpty(comment));
        params.add(StringUtils.trimToEmpty(accountName));

        final Object ret = this.call(xmlMethod, params);

        if (ret instanceof Boolean) {
            return ((Boolean) ret).booleanValue();
        }
        throw new PaperCutException("Unexpected reurn value.");
    }

    /**
     * Gets a user's account balance.
     *
     * @param username
     *            The username associated with the user who's account balance is
     *            to be retrieved.
     * @param scale
     *            The scale of the return value.
     * @return The {@link BigDecimal} balance.
     * @throws PaperCutException
     *             If the user (account) does not exist.
     */
    public BigDecimal getUserAccountBalance(final String username,
            final int scale) throws PaperCutException {

        final Vector<Object> params = this.createParams();
        params.add(username);

        final Double balance =
                (Double) this.call(XMLMethod.GET_USER_ACCOUNT_BALANCE, params);

        return PaperCutDb.getAmountBigBecimal(balance.doubleValue(), scale);
    }

    /**
     * Creates a new shared account with the given name.
     *
     * @param topAccountName
     *            The full name of the top shared account to adjust.
     * @param subAccountName
     *            The full name of the sub shared account to adjust (can be
     *            {@code null}.
     * @throws PaperCutException
     *             If the account already exists.
     */
    public void addNewSharedAccount(final String topAccountName,
            final String subAccountName) throws PaperCutException {
        final Vector<Object> params = this.createParams();
        params.add(composeSharedAccountName(topAccountName, subAccountName));
        this.call(XMLMethod.ADD_NEW_SHARED_ACCOUNT, params);
    }

    // api.listUserGroups
    public SortedSet<CommonUserGroup> listUserGroups()
            throws PaperCutException {

        final SortedSet<CommonUserGroup> sortedSet =
                CommonUserGroup.createSortedSet();
        int offset = 0;
        int count = this.listUserGroups(sortedSet, API_BATCHSIZE, offset);

        while (count == API_BATCHSIZE) {
            offset += count;
            count = this.listUserGroups(sortedSet, API_BATCHSIZE, offset);
        }

        return sortedSet;
    }

    /**
     * Adds user groups to {@link SortedSet}.
     *
     * @param sortedSet
     *            {@link SortedSet} to add user groups to.
     * @param batchsize
     * @param offset
     * @return number of instances put on the list.
     * @throws PaperCutException
     */
    private int listUserGroups(final SortedSet<CommonUserGroup> sortedSet,
            final int batchsize, final int offset) throws PaperCutException {

        final Vector<Object> params = this.createParams();

        params.add(offset); // #1
        params.add(batchsize); // #2

        int count = 0;
        for (final Object obj : (Object[]) this.call(XMLMethod.LIST_USER_GROUPS,
                params)) {

            final String groupName = obj.toString();

            if (RESERVED_USER_GROUPS.contains(groupName)) {
                continue;
            }

            final CommonUserGroup commonUserGroup =
                    this.getUserGroup(groupName);

            if (commonUserGroup == null) {
                throw new PaperCutException(
                        "Details of User Group [" + groupName + "] not found.");
            }
            sortedSet.add(commonUserGroup);

            count++;
        }
        return count;
    }

    /**
     * List all members (sorted by username) of a group starting at offset and
     * ending at limit.
     *
     * @param groupName
     *            group name.
     * @return {@link SortedSet}.
     * @throws PaperCutException
     */
    public SortedSet<CommonUser> getGroupMembers(final String groupName)
            throws PaperCutException {

        final SortedSet<CommonUser> sortedSet = CommonUser.createSortedSet();

        if (this.isGroupExists(groupName)) {

            int offset = 0;
            int count = this.getGroupMembers(sortedSet, groupName,
                    API_BATCHSIZE, offset);

            while (count == API_BATCHSIZE) {
                offset += count;
                count = this.getGroupMembers(sortedSet, groupName,
                        API_BATCHSIZE, offset);
            }
        }
        return sortedSet;
    }

    /**
     * Adds user accounts to {@link SortedSet}.
     *
     * @param sortedSet
     *            {@link SortedSet} to add user accounts to.
     * @param groupName
     *            group name.
     * @param batchsize
     * @param offset
     * @return number of instances put on the list.
     * @throws PaperCutException
     */
    private int getGroupMembers(final SortedSet<CommonUser> sortedSet,
            final String groupName, final int batchsize, final int offset)
            throws PaperCutException {

        final Vector<Object> params = this.createParams();

        params.add(groupName); // #1
        params.add(offset); // #2
        params.add(batchsize); // #3

        int count = 0;
        for (final Object obj : (Object[]) this
                .call(XMLMethod.GET_GROUP_MEMBERS, params)) {

            final String userName = obj.toString();
            final CommonUser commonUser = this.getCommonUser(userName);

            if (commonUser == null) {
                throw new PaperCutException(
                        "Details of User [" + userName + "] not found.");
            }
            sortedSet.add(commonUser);

            count++;
        }
        return count;
    }

    /**
     * List all user accounts (sorted by username) starting at offset and ending
     * at limit.
     *
     * @return {@link SortedSet}.
     * @throws PaperCutException
     */
    public SortedSet<CommonUser> listUserAccounts() throws PaperCutException {

        final SortedSet<CommonUser> sortedSet = CommonUser.createSortedSet();

        int offset = 0;
        int count = this.listUserAccounts(sortedSet, API_BATCHSIZE, offset);

        while (count == API_BATCHSIZE) {
            offset += count;
            count = this.listUserAccounts(sortedSet, API_BATCHSIZE, offset);
        }
        return sortedSet;
    }

    /**
     * Adds user accounts to {@link SortedSet}.
     *
     * @param sortedSet
     *            {@link SortedSet} to add user accounts to.
     * @param batchsize
     * @param offset
     * @return number of instances put on the list.
     * @throws PaperCutException
     */
    private int listUserAccounts(final SortedSet<CommonUser> sortedSet,
            final int batchsize, final int offset) throws PaperCutException {

        final Vector<Object> params = this.createParams();

        params.add(offset); // #1
        params.add(batchsize); // #2

        int count = 0;
        for (final Object obj : (Object[]) this
                .call(XMLMethod.LIST_USER_ACCOUNTS, params)) {

            final String userName = obj.toString();
            final CommonUser commonUser = this.getCommonUser(userName);

            if (commonUser == null) {
                throw new PaperCutException(
                        "Details of User [" + userName + "] not found.");
            }
            sortedSet.add(commonUser);

            count++;
        }
        return count;
    }

    /**
     * @return {@code true} if a {@link CircuitBreaker} is used to signal
     *         PaperCut connection status.
     */
    public boolean isUseCircuitBreaker() {
        return useCircuitBreaker;
    }

    /**
     * @param useBreaker
     *            {@code true} if a {@link CircuitBreaker} is used to signal
     *            PaperCut connection status.
     */
    public void setUseCircuitBreaker(final boolean useBreaker) {
        this.useCircuitBreaker = useBreaker;
    }

}
