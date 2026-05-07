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

import java.math.BigDecimal;
import java.net.URL;

import org.printflow.lite.ext.ServerPlugin;

/**
 * Payment Gateway Transaction. All amounts are in the transactions's own
 * currency code.
 *
 * @author Rijk Ravestein
 *
 */
public final class PaymentGatewayTrx {

    private String gatewayId;
    private String transactionId;
    private String transactionAccount;
    private String status;
    private String userId;
    private String currencyCode;
    private BigDecimal amount;
    private BigDecimal fee;
    private String exchangeCurrencyCode;
    private BigDecimal exchangeRate;
    private PaymentMethodEnum paymentMethod;
    private String paymentMethodOther;
    private String comment;
    private boolean live;
    private int confirmations;
    private String details;

    /**
     * The {@link URL}, provided by the Payment Gateway, where the payment is
     * executed. After a payment request the User WebApp should redirect the
     * user to this URL.
     */
    private URL paymentUrl;

    /**
     *
     * @return The unique ID of the Payment Gateway. See
     *         {@link ServerPlugin#getId()}.
     */
    public String getGatewayId() {
        return gatewayId;
    }

    /**
     * Sets the unique ID of the Payment Gateway. See
     * {@link ServerPlugin#getId()}.
     *
     * @param gatewayId
     *            The ID.
     */
    public void setGatewayId(final String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionAccount() {
        return transactionAccount;
    }

    public void setTransactionAccount(String transactionAccount) {
        this.transactionAccount = transactionAccount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     *
     * @return The ISO currency code of the transaction.
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     *
     * @param currencyCode
     *            The ISO currency code of the transaction.
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getExchangeCurrencyCode() {
        return exchangeCurrencyCode;
    }

    public void setExchangeCurrencyCode(String exchangeCurrencyCode) {
        this.exchangeCurrencyCode = exchangeCurrencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public PaymentMethodEnum getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethodEnum paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodOther() {
        return paymentMethodOther;
    }

    public void setPaymentMethodOther(String paymentTypeOther) {
        this.paymentMethodOther = paymentTypeOther;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     *
     * @return The {@link URL} where the payment is executed.
     */
    public URL getPaymentUrl() {
        return paymentUrl;
    }

    /**
     *
     * @param paymentUrl
     *            The {@link URL} where the payment is executed.
     */
    public void setPaymentUrl(URL paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     *
     * @return The exchange rate to the exchange currency.
     */
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

}
