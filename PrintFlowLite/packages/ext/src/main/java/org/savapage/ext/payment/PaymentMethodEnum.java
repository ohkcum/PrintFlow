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
 * Payments Methods. (Default) Image file names are taken from
 * <a href="https://www.mollie.com/resources">Mollie Resources</a> and
 *
 * @author Rijk Ravestein
 *
 */
public enum PaymentMethodEnum {

    /** */
    INGHOMEPAY("ING Home'Pay"),

    /** */
    BITCOIN("Bitcoin"),

    /** */
    GIROPAY("Giropay"),

    /** Dutch iDEAL | Wero. */
    IDEAL("iDEAL | Wero", "iDEAL-Wero-Extra-large-Squircle.png",
            "iDEAL-Wero-Extra-large-Squircle@2x.png"),

    /** */
    CREDITCARD("Credit Card"),

    /** */
    DIRECTDEBIT("Direct Debit"),

    /** */
    KBC,

    /** */
    MISTERCASH("MisterCash"),

    /** */
    PAYPAL("PayPal"),

    /** */
    PAYSAFECARD("paysafecard"),

    /** */
    SOFORT("Sofort"),

    /** */
    BANKTRANSFER("Bank Transfer"),

    /** */
    BELFIUS("Belfius"),

    /** Any other payment type. */
    OTHER("Other Payment");

    /**
     * UI text.
     */
    private final String uiText;

    /**
     * Image file name.
     */
    private final String imgFileName;

    /**
     * Image file name (twice the size).
     */
    private final String imgFileName2x;

    PaymentMethodEnum() {
        this.uiText = this.name();
        this.imgFileName = null;
        this.imgFileName2x = null;
    }

    /**
     * @param text
     *            UI text.
     */
    PaymentMethodEnum(final String text) {
        this.uiText = text;
        this.imgFileName = null;
        this.imgFileName2x = null;
    }

    /**
     * @param text
     *            UI text.
     * @param img
     *            Image file name.
     * @param img2x
     *            Image file name (twice the size).
     */
    PaymentMethodEnum(final String text, final String img, final String img2x) {
        this.uiText = text;
        this.imgFileName = img;
        this.imgFileName2x = img2x;
    }

    /**
     * @return The localized text.
     */
    public String uiText() {
        return this.uiText;
    }

    /**
     * The default name of the PNG file is EQ to the lower case enum value of
     * the payment method.
     *
     * @return Image file name.
     */
    private String imageFileName() {
        if (this.imgFileName == null) {
            return this.toString().toLowerCase().concat(".png");
        }
        return this.imgFileName;
    }

    /**
     * The default name of the PNG file is EQ to the lower case enum value of
     * the payment method. For a bigger image "@2x" is appended.
     *
     * @return Image file name (twice the size).
     */
    private String imageFileName2x() {
        if (this.imgFileName2x == null) {
            return this.toString().toLowerCase().concat("@2x.png");
        }
        return this.imgFileName2x;
    }

    /**
     * Gets the PNG image URL of the payment method.
     *
     * @param urlPathImage
     *            URL path of the image.
     * @param bigger
     *            {@code true} for bigger image.
     * @return The relative URL as string.
     */
    public String getPaymentMethodImgUrl(final String urlPathImage,
            final boolean bigger) {

        final StringBuilder url =
                new StringBuilder().append(urlPathImage).append("/");

        if (bigger) {
            url.append(this.imageFileName2x());
        } else {
            url.append(this.imageFileName());
        }

        return url.toString();
    }

}
