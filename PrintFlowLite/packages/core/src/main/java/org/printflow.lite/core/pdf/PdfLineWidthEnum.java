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
 * PDF line width as shown in Adobe Acrobat.
 *
 * @author Rijk Ravestein
 *
 */
public enum PdfLineWidthEnum {

    /** */
    INVISIBLE(0),
    /** */
    THIN(1.0f),
    /** */
    MEDIUM(2.0f),
    /** */
    THICK(3.0f);

    /** */
    private final float width;

    /**
     * @param w
     *            width.
     */
    PdfLineWidthEnum(final float w) {
        this.width = w;
    }

    /**
     * @return line width.
     */
    public float getWidth() {
        return this.width;
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
