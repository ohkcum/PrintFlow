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

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PaymentGatewayListener extends PaymentGatewayListenerBase {

    /**
     * Notifies a failed {@link PaymentGatewayTrx}.
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     * @return The {@link PaymentGatewayTrxEvent}.
     * @throws PaymentGatewayException
     *             When an error occurred.
     */
    PaymentGatewayTrxEvent onPaymentFailed(PaymentGatewayTrx trx)
            throws PaymentGatewayException;

    /**
     * Notifies a cancelled {@link PaymentGatewayTrx}.
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     * @return The {@link PaymentGatewayTrxEvent}.
     * @throws PaymentGatewayException
     *             When an error occurred.
     */
    PaymentGatewayTrxEvent onPaymentCancelled(PaymentGatewayTrx trx)
            throws PaymentGatewayException;

    /**
     * Notifies an expired {@link PaymentGatewayTrx}.
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     * @return The {@link PaymentGatewayTrxEvent}.
     * @throws PaymentGatewayException
     *             When an error occurred.
     */
    PaymentGatewayTrxEvent onPaymentExpired(PaymentGatewayTrx trx)
            throws PaymentGatewayException;

    /**
     * Notifies an acknowledged {@link PaymentGatewayTrx}.
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     * @return The {@link PaymentGatewayTrxEvent}.
     * @throws PaymentGatewayException
     *             When an error occurred or when user is not found.
     */
    PaymentGatewayTrxEvent onPaymentAcknowledged(PaymentGatewayTrx trx)
            throws PaymentGatewayException;

    /**
     * Notifies a pending {@link PaymentGatewayTrx}.
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     * @return The {@link PaymentGatewayTrxEvent}.
     * @throws PaymentGatewayException
     *             When an error occurred.
     */
    PaymentGatewayTrxEvent onPaymentPending(PaymentGatewayTrx trx)
            throws PaymentGatewayException;

    /**
     * Notifies a refunded {@link PaymentGatewayTrx}.
     *
     * @param trx
     *            The {@link PaymentGatewayTrx}.
     * @return The {@link PaymentGatewayTrxEvent}.
     * @throws PaymentGatewayException
     *             When an error occurred.
     */
    PaymentGatewayTrxEvent onPaymentRefunded(PaymentGatewayTrx trx)
            throws PaymentGatewayException;

}
