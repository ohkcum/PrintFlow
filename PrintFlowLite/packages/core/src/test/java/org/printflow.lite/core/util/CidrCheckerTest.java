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
package org.printflow.lite.core.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CidrCheckerTest {

    @Test
    public void test1() {

        final String cidr064 = "2001:db8:85a3::8a2e:370:7334/64";
        final String cidr128 = "2001:db8:85a3::8a2e:370:7334/128";
        final String cidrAll = cidr064 + "," + cidr128;

        final Object[][] tests = new Object[][] { //
                { cidr064, "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
                        Boolean.TRUE }, //
                { cidr064, "2001:0db8:85a3:0000:0001:8a2e:0370:7334",
                        Boolean.TRUE }, //
                { cidr064, "2001:0db8:85a3:0001:0000:8a2e:0370:7334",
                        Boolean.FALSE }, //
                //
                { cidr128, "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
                        Boolean.TRUE }, //
                { cidr128, "2001:0db8:85a3:0000:0000:8a2e:0370:7333",
                        Boolean.FALSE }, //
                { cidrAll, "2001:0db8:85a3:0000:0000:8a2e:0370:7333",
                        Boolean.TRUE }, //
                { "::01/128", "0:0:0:0:0:0:0:1", Boolean.TRUE }, //
                { "::01/128", "0:0:0:0:0:0:0:2", Boolean.FALSE}, //
        };

        for (final Object[] test : tests) {
            assertTrue(InetUtils.isIpAddrInCidrRanges(test[0].toString(),
                    test[1].toString()) == (Boolean) test[2]);
        }
    }
}
