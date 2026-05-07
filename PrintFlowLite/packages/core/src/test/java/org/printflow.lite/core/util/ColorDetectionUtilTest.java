/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ColorDetectionUtilTest {

    @Test
    public void testColor() {
        final String[] arraySVG = new String[] { //
                " RGB(0,0,1)", //
                " RgB(255,128,255)", //
                "xxx RgB(0,0,1) xx xx RgB(0,0,0)", //
                "xRGB ( 2,2,2  ) RGB ( 1,2,1  ) ", //
                " RGB(22,222,1)", //
                " RGB ( 0,0,1)", //
                " RGB ( 0,0,1  )", //
                " RGB ( 0, 0,  1)" //
        };
        for (String temp : arraySVG) {
            final boolean isColor = ColorDetectionUtil.isColorRGBinSVG(temp);
            if (!isColor) {
                System.err.printf("testColor [%s] : detected as monochrome\n",
                        temp);
            }
            assertTrue(isColor);
        }
    }

    @Test
    public void testMonochrome() {
        final String[] arraySVG = new String[] { //
                " RGB(0,0,0)", //
                " RgB(255,255,255)", //
                " RgB(1,1,1)", //
                " xRGB ( 1,1,2  )", //
                "xxx RgB(48,48,48) xx xx RgB(0,0,0)", //
                " RGB ( 0,0,0)", //
                " xRGB ( 1,1,1  )", //
                " RGB ( 22, 22,  22)" //
        };
        for (String temp : arraySVG) {
            final boolean isColor = ColorDetectionUtil.isColorRGBinSVG(temp);
            if (isColor) {
                System.err.printf("testMonochrome [%s] : detected as color\n",
                        temp);
            }
            assertTrue(!ColorDetectionUtil.isColorRGBinSVG(temp));
        }
    }
}
