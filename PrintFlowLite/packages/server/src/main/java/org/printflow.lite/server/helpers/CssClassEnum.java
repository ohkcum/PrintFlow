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

/**
 * CSS class names.
 *
 * @author Rijk Ravestein
 *
 */
public enum CssClassEnum {

    /** JQuery Mobile icon. */
    JQM_UI_ICON_PFL_PASSWORD_EYE_OPEN("ui-icon-sp-password-eye-open"),
    /**  */
    PFL_BTN_ABOUT("sp-btn-about"),
    /** */
    PFL_BTN_ABOUT_ORG("sp-btn-about-org"),
    /** */
    PFL_BTN_ABOUT_USER_ID("sp-btn-about-userid"),
    /** */
    PFL_BTN_SECRET_VISIBILITY("sp-btn-secret-visibility");

    /**
     * CSS class name.
     */
    private final String clazz;

    /**
     * Constructor.
     *
     * @param value
     *            The CSS class.
     */
    CssClassEnum(final String value) {
        this.clazz = value;
    }

    /**
     * @return the CSS class name.
     */
    public String clazz() {
        return this.clazz;
    }

}
