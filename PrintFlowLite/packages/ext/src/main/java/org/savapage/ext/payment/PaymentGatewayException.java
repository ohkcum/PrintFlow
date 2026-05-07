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

import org.printflow.lite.ext.ServerPluginException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PaymentGatewayException extends ServerPluginException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link PaymentGatewayException} without a message.
     *
     */
    private PaymentGatewayException() {
        super("");
    }

    /**
     * Constructs a new {@link PaymentGatewayException}.
     *
     * @param message
     *            The detail message.
     */
    public PaymentGatewayException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@link PaymentGatewayException}.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public PaymentGatewayException(final String message,
            final Throwable cause) {
        super(message, cause);
    }
}
