/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.ext.payment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Currency;
import java.util.Map;

import org.printflow.lite.ext.ServerPlugin;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PaymentGatewayPlugin extends ServerPlugin {

    /**
     * .
     */
    final class PaymentRequest {

        /**
         * The unique id of the requesting user.
         */
        private String userId;

        /**
         *
         */
        private Currency currency;

        /**
         *
         */
        private PaymentMethodEnum method;

        /**
         * The payment amount.
         */
        private double amount;

        /**
         * The payment description.
         */
        private String description;

        /**
         * The callback {@link URL} for the Payment Gateway.
         */
        private URL callbackUrl;

        /**
         * The {@link URL} the user is redirected to after the payment dialog.
         */
        private URL redirectUrl;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public PaymentMethodEnum getMethod() {
            return method;
        }

        public void setMethod(PaymentMethodEnum method) {
            this.method = method;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public URL getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(URL callbackUrl) {
            this.callbackUrl = callbackUrl;
        }

        public URL getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(URL redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

    }

    /**
     * .
     */
    final class CallbackResponse {

        /**
         * HTTP status to be returned to the caller.
         */
        private final int httpStatus;

        /**
         * HTTP ContentType to be returned to the caller.
         */
        private final String contentType;

        /**
         * The {@link Object} offered to the plug-in after a successful commit.
         */
        private Object pluginObject;

        /**
         *
         * @param httpStatus
         *            HTTP status to be returned to the caller.
         */
        public CallbackResponse(final int httpStatus) {
            this.httpStatus = httpStatus;
            this.contentType = null;
        }

        /**
         *
         * @param httpStatus
         *            HTTP status to be returned to the caller.
         * @param contentType
         *            HTTP ContentType to be returned to the caller.
         */
        public CallbackResponse(final int httpStatus,
                final String contentType) {
            this.httpStatus = httpStatus;
            this.contentType = contentType;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public String getContentType() {
            return contentType;
        }

        public Object getPluginObject() {
            return pluginObject;
        }

        public void setPluginObject(Object pluginObject) {
            this.pluginObject = pluginObject;
        }

    }

    /**
     *
     * @return {@code true} when plug-in is live, {@code false} when in test
     *         mode.
     */
    boolean isLive();

    /**
     *
     * @return {@code true} when plug-in is online, {@code false} when offline.
     */
    boolean isOnline();

    /**
     *
     * @param online
     *            {@code true} when plug-in is online, {@code false} when
     *            offline.
     */
    void setOnline(boolean online);

    /**
     * @param currencyCode
     *            The ISO currency code. E.g. "USD, "EUR"
     * @return {@code true} when currency code is supported.
     */
    boolean isCurrencySupported(String currencyCode);

    /**
     * Notifies a {@link PaymentRequest}.
     *
     * @param request
     *            The {@link PaymentRequest}.
     * @return The {@link PaymentGatewayTrx} with information about the pending
     *         payment transaction.
     * @throws IOException
     *             When a communication error occurs.
     * @throws PaymentGatewayException
     *             When a logical error occurs.
     */
    PaymentGatewayTrx onPaymentRequest(PaymentRequest request)
            throws IOException, PaymentGatewayException;

    /**
     * Notifies the Web API callback.
     *
     * @param parameterMap
     *            The callback parameters and their values as in
     *            {@link HttpServletRequest#getParameterMap()}.
     * @param live
     *            If {@code true} this is a live (real) payment, if
     *            {@code false} this is a test.
     * @param currency
     *            The {@link Currency}.
     * @param request
     *            The {@link BufferedReader} for reading the body of the
     *            request.
     * @param response
     *            The {@link PrintWriter} for writing the response.
     *
     * @return The {@link CallbackResponse}.
     * @throws IOException
     *             When communication errors.
     * @throws PaymentGatewayException
     *             When logical errors occur;
     */
    CallbackResponse onCallBack(Map<String, String[]> parameterMap,
            boolean live, Currency currency, BufferedReader request,
            PrintWriter response) throws IOException, PaymentGatewayException;

    /**
     * Notifies that
     * {@link #onCallBack(Map, boolean, Currency, BufferedReader, PrintWriter)}
     * was committed.
     *
     * @param pluginObject
     *            The object as in {@link CallbackResponse#getPluginObject()}.
     * @throws PaymentGatewayException
     *             When logical errors occur;
     */
    void onCallBackCommitted(Object pluginObject)
            throws PaymentGatewayException;

}
