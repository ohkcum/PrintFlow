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
package org.printflow.lite.server.ext;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.ext.ServerPlugin;
import org.printflow.lite.ext.payment.PaymentGatewayException;
import org.printflow.lite.ext.payment.PaymentGatewayListener;
import org.printflow.lite.ext.payment.PaymentGatewayTrx;
import org.printflow.lite.ext.payment.PaymentGatewayTrxEvent;
import org.printflow.lite.ext.payment.PaymentMethodEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dedicated CSV logger for {@link PaymentGatewayTrx} audit messages.
 *
 * @author Rijk Ravestein
 *
 */
public final class PaymentGatewayLogger implements PaymentGatewayListener {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaymentGatewayLogger.class);

    /** */
    private static final String DATEFORMAT_PATTERN = "yyyy-MM-dd'\t'HH:mm:ss.S";

    /**
     * Initial capacity of the {@link StringBuilder} building the audit record.
     */
    private static final int STRING_BUILDER_CAPACITY = 128;

    /** */
    private static final String MODE_LIVE = "live";

    /** */
    private static final String MODE_TEST = "test";

    /**
     * Prevent public instantiation.
     */
    private PaymentGatewayLogger() {

    }

    /**
     * .
     */
    private static class SingletonHolder {
        /** */
        public static final PaymentGatewayLogger INSTANCE =
                new PaymentGatewayLogger();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static PaymentGatewayLogger instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @return
     */
    public static boolean isEnabled() {
        return LOGGER.isInfoEnabled();
    }

    /**
     *
     * @return The formatted current datetime.
     */
    private static String formattedDateTime() {
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat(DATEFORMAT_PATTERN);
        return dateFormat.format(new Date());
    }

    /**
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     */
    private PaymentGatewayTrxEvent onPaymentTrx(final PaymentGatewayTrx trx) {

        if (!isEnabled()) {
            return null;
        }

        final String mode;

        if (trx.isLive()) {
            mode = MODE_LIVE;
        } else {
            mode = MODE_TEST;
        }

        final StringBuilder msg = new StringBuilder(STRING_BUILDER_CAPACITY);

        msg.append(formattedDateTime());
        msg.append('\t').append(trx.getGatewayId());
        msg.append('\t').append(trx.getTransactionId());
        msg.append('\t')
                .append(StringUtils.defaultString(trx.getTransactionAccount()));
        msg.append('\t').append(mode);

        if (trx.getPaymentMethod() != null) {

            msg.append('\t').append(trx.getPaymentMethod().toString());

            if (trx.getPaymentMethod() == PaymentMethodEnum.OTHER) {
                msg.append(" (")
                        .append(StringUtils
                                .defaultString(trx.getPaymentMethodOther()))
                        .append(')');
            }

        }

        msg.append('\t').append(trx.getStatus());
        msg.append('\t').append(trx.getUserId());
        msg.append('\t').append(trx.getAmount());
        msg.append('\t').append(StringUtils.defaultString(trx.getComment()));

        LOGGER.info(msg.toString());

        return null;
    }

    @Override
    public PaymentGatewayTrxEvent onPaymentFailed(final PaymentGatewayTrx trx) {
        return onPaymentTrx(trx);
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentCancelled(final PaymentGatewayTrx trx) {
        return onPaymentTrx(trx);
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentExpired(final PaymentGatewayTrx trx) {
        return onPaymentTrx(trx);
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentPending(final PaymentGatewayTrx trx) {
        return onPaymentTrx(trx);
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentRefunded(final PaymentGatewayTrx trx) {
        return onPaymentTrx(trx);
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentAcknowledged(final PaymentGatewayTrx trx) {
        return onPaymentTrx(trx);
    }

    @Override
    public void onPaymentCommitted(final PaymentGatewayTrxEvent event)
            throws PaymentGatewayException {
        // noop
    }

    @Override
    public void onPluginException(final ServerPlugin plugin,
            final IOException ex) {
        LOGGER.error(String.format("%s in plug-in [%s]: %s",
                ex.getClass().getSimpleName(), plugin.getId(),
                ex.getMessage()));
    }

}
