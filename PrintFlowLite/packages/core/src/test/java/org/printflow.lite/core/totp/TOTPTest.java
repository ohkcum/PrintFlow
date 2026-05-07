/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.totp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printflow.lite.lib.totp.TOTPAuthenticator;
import org.printflow.lite.lib.totp.TOTPAuthenticator.BaseNCodecEnum;
import org.printflow.lite.lib.totp.TOTPAuthenticator.HmacAlgorithmEnum;
import org.printflow.lite.lib.totp.TOTPAuthenticator.KeySizeEnum;
import org.printflow.lite.lib.totp.TOTPException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TOTPTest {

    /**
     *
     * @param auth
     *            TOTPAuthenticator.
     */
    private void validateCode(final TOTPAuthenticator auth) {
        final long code = auth.generateTOTPAsLong();
        try {
            assertTrue(auth.verifyTOTP(code));
            assertFalse(auth.verifyTOTP(code + 1));
        } catch (TOTPException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Defaults.
     */
    @Test
    public void test01() {
        final TOTPAuthenticator auth = TOTPAuthenticator.buildKey().build();
        this.validateCode(auth);
    }

    /**
     * Specifics.
     */
    @Test
    public void test02() {
        final TOTPAuthenticator auth = TOTPAuthenticator
                .buildKey(BaseNCodecEnum.BASE64, KeySizeEnum.SIZE_16).build();
        this.validateCode(auth);
    }

    /**
     * Specifics.
     */
    @Test
    public void test03() {
        final TOTPAuthenticator auth = TOTPAuthenticator
                .buildKey(BaseNCodecEnum.BASE32, KeySizeEnum.SIZE_16)
                .setHmacAlgorithm(HmacAlgorithmEnum.SHA512).build();
        this.validateCode(auth);
    }

}
