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
package org.printflow.lite.server.helpers;

import java.util.Locale;

import org.printflow.lite.core.jpa.Printer;
import org.printflow.lite.core.util.LocaleHelper;
import org.printflow.lite.server.WebApp;

/**
 * Images for {@link Printer}.
 *
 * @author Rijk Ravestein
 *
 */
public enum HtmlPrinterImgEnum {

    /**
     * A Job Ticket Printer.
     */
    JOBTICKET("printer-jobticket-32x32.png"),

    /**
     * An unsecured Proxy Printer that no one is allowed to print to.
     */
    NON_SECURE("printer-terminal-none-16x16.png"),

    /**
     * An unsecured Proxy Printer that <b>anyone</b> can print to.
     */
    NON_SECURE_ALLOWED("printer-terminal-any-16x16.png"),

    /**
     * A secured Proxy Printer whose jobs needs to be authorized with a NFC Card
     * swipe on a Network Card Reader.
     */
    READER("printer-terminal-auth-16x16.png"),

    /**
     * A Proxy Printer that can only be used from certain Terminals.
     */
    TERMINAL("printer-terminal-custom-16x16.png"),

    /**
     * A Proxy Printer that can only be used from certain Terminals and whose
     * jobs needs to be authorized with a NFC Card swipe on a Network Card
     * Reader on other Terminals.
     */
    TERMINAL_AND_READER("printer-terminal-custom-or-auth-16x16.png");

    /**
     * .
     */
    private final String img;

    /**
     * Constructor.
     *
     * @param value
     *            The CSS class.
     */
    HtmlPrinterImgEnum(final String value) {
        this.img = value;
    }

    /**
     *
     * @return URL path of printer image.
     */
    public String urlPath() {
        return String.format("%s/%s", WebApp.PATH_IMAGES, this.img);
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized tooltip text.
     */
    public String uiToolTip(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

}
