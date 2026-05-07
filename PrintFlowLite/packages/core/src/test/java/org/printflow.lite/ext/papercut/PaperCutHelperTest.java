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
package org.printflow.lite.ext.papercut;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.printflow.lite.core.jpa.Account.AccountTypeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutHelperTest {

    private static final String TEST_ACCOUNT = "account";
    private static final String TEST_PARENT = "parent";

    @Test
    public void testAccountCompose() {

        String composed;

        //
        composed = PaperCutHelper.composeSharedAccountName(
                AccountTypeEnum.GROUP, TEST_ACCOUNT, null);

        assertEquals(PaperCutHelper.decomposeSharedAccountName(composed),
                TEST_ACCOUNT);
        //
        composed = PaperCutHelper.composeSharedAccountName(
                AccountTypeEnum.SHARED, TEST_ACCOUNT, TEST_PARENT);

        assertEquals(PaperCutHelper.decomposeSharedAccountName(composed),
                PaperCutHelper.composeSharedAccountNameSuffix(TEST_ACCOUNT,
                        TEST_PARENT));

    }

    @Test
    public void testException() {

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> PaperCutHelper.decomposeSharedAccountName(
                        PaperCutHelper.composeSharedAccountNameSuffix(
                                TEST_ACCOUNT, TEST_PARENT)));

        Assertions.assertThrows(NullPointerException.class,
                () -> PaperCutHelper.decomposeSharedAccountName(null));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> PaperCutHelper.decomposeSharedAccountName(TEST_ACCOUNT));
    }
}
