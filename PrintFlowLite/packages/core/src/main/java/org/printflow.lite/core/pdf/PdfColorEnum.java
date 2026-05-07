/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.pdf;

import java.util.Locale;

import org.printflow.lite.core.util.LocaleHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum PdfColorEnum {
    /** */
    WHITE(255, 255, 255),
    /** */
    LIGHT_GRAY(192, 192, 192),
    /** */
    GRAY(128, 128, 128),
    /** */
    DARK_GRAY(64, 64, 64),
    /** */
    BLACK(0, 0, 0),
    /** */
    RED(255, 0, 0),
    /** */
    PINK(255, 175, 175),
    /** */
    ORANGE(255, 200, 0),
    /** */
    YELLOW(255, 255, 0),
    /** */
    GREEN(0, 255, 0),
    /** */
    MAGENTA(255, 0, 255),
    /** */
    CYAN(0, 255, 255),
    /** */
    BLUE(0, 0, 255);

    /** */
    private final int red;
    /** */
    private final int green;
    /** */
    private final int blue;

    /** */
    private static final int COMPONENT_MAX = 255;

    /**
     * @param r
     *            red.
     * @param g
     *            green.
     * @param b
     *            blue.
     */
    PdfColorEnum(final int r, final int g, final int b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    /**
     * @return float[3] array with RGB colors as fraction of max 1.0f.
     */
    public float[] asFloatArray() {
        return new float[] { //
                (float) this.red / COMPONENT_MAX,
                (float) this.green / COMPONENT_MAX,
                (float) this.blue / COMPONENT_MAX };
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

}
