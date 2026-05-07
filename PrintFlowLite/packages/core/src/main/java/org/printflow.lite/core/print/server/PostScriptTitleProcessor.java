/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.print.server;

import java.util.Arrays;

import org.printflow.lite.common.IUtility;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PostScriptTitleProcessor implements IUtility {

    /**
     * Utility class.
     */
    private PostScriptTitleProcessor() {
    }

    /**
     * Processes a {@link PostScriptFilter#PFX_TITLE} line, taking octal escaped
     * (unicode) characters in account. Examples:
     * <p>
     * Escaped Cyrillic "Ӱ ӱ Ӳ ӳ Ӵ ӵ Ӷ ӷ Ӹ ӹ Ӻ ӻ Ӽ ӽ Ӿ ӿ" = "\323\260 \323\261
     * \323\262 \323\263 \323\264 \323\265 \323\266 \323\267 \323\270 \323\271
     * \323\272 \323\273 \323\274 \323\275 \323\276 \323\277"/p>
     * <p>
     * Escaped CJK "丐 丑 丒 专 且 丕 世 丗 丘 丙 业 丛 东 丝 丞 丟" = "\344\270\220
     * \344\270\221 \344\270\222 \344\270\223 \344\270\224 \344\270\225
     * \344\270\226 \344\270\227 \344\270\230 \344\270\231 \344\270\232
     * \344\270\233 \344\270\234 \344\270\235 \344\270\236 \344\270\237
     * .txt)\344\270\232 \344\270\233 \344\270\234 \344\270\235 \344\270\236
     * \344\270\237"/p>
     *
     * @param title
     *            Title to process.
     * @return Processed title.
     */
    public static String process(final String title) {
        return processGeneric(title);
    }

    /**
     * Processes octal escaped characters.
     *
     * @param title
     *            Title to process.
     * @return Processed title.
     */
    private static String processGeneric(final String title) {

        final char[] cArr = title.toCharArray();
        final byte[] bArr = new byte[cArr.length];

        final char cSlash = '\\';

        final int octalRadix = 8;
        final int nMaxOctalDigits = 3;
        final int[] digitsWlk = new int[nMaxOctalDigits];

        final int nOctalDigitsStart = -1;
        int nDigitsCollected = nOctalDigitsStart;

        int j = 0;

        for (int i = 0; i < cArr.length; i++) {

            final char c = cArr[i];

            if (nDigitsCollected > nOctalDigitsStart) {
                if (Character.isDigit(c)) {
                    digitsWlk[nDigitsCollected] =
                            Integer.parseInt(Character.toString(c));
                    nDigitsCollected++;
                    if (nDigitsCollected == nMaxOctalDigits) {
                        bArr[j++] =
                                (byte) (digitsWlk[0] * octalRadix * octalRadix
                                        + digitsWlk[1] * octalRadix
                                        + digitsWlk[2]);
                        nDigitsCollected = nOctalDigitsStart;
                    }
                    continue;
                } else {
                    bArr[j++] = (byte) cSlash;
                    bArr[j++] = (byte) c;
                    nDigitsCollected = nOctalDigitsStart;
                    continue;
                }
            }

            if (c == cSlash) {
                nDigitsCollected = 0;
                continue;
            }
            bArr[j++] = (byte) c;
        }

        // Flush collected.
        if (nDigitsCollected > nOctalDigitsStart) {
            bArr[j++] = (byte) cSlash;
            for (int i = 0; i < nDigitsCollected; i++) {
                bArr[j++] = (byte) digitsWlk[i];
            }
        }
        // Return collected characters, and remove double escaped slashes.
        return new String(Arrays.copyOfRange(bArr, 0, j)).replace("\\\\", "\\");
    }

}
