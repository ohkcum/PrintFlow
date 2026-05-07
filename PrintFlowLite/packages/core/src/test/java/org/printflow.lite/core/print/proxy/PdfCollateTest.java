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
package org.printflow.lite.core.print.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printflow.lite.core.pdf.PdfPrintCollector;

/**
 * Test cases for
 * {@link PdfPrintCollector#calcBlankAppendPagesOfCopy(ProxyPrintSheetsCalcParms)}.
 * .
 *
 * @author Datraverse B.V.
 *
 */
public class PdfCollateTest {

    /**
     * Calculates the extra blank pages to append to a PDF copy in a collated
     * sequence.
     *
     * @param calcParms
     *            The {@link ProxyPrintSheetsCalcParms}.
     * @return The number of extra blank pages to append to each copy.
     */
    public static int calcBlankCollatePagesToAppend(
            final ProxyPrintSheetsCalcParms calcParms) {
        return PdfPrintCollector.calcBlankAppendPagesOfCopy(calcParms);
    }

    private static final boolean DUPLEX = true;

    /**
     *
     * @param pages
     * @param nup
     * @param duplex
     * @return
     */
    private static BasePrintSheetCalcParms createParms(final int copies,
            final int pages, final int nup, final boolean duplex) {
        final BasePrintSheetCalcParms parms = new BasePrintSheetCalcParms();

        parms.setNumberOfCopies(copies);
        parms.setNumberOfPages(pages);
        parms.setNup(nup);
        parms.setDuplex(duplex);

        return parms;
    }

    @Test
    public void test4Up() {

        final int nUp = 4;

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, !DUPLEX)),
                "1 copy, 4 pages, 4-up, singlex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(2, 3, nUp, !DUPLEX)),
                "2 copy, 3 pages, 4-up, singlex");

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, !DUPLEX)),
                "10 copies, 4 pages, 4-up, singlex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)),
                "1 copy, 3 pages, 4-up, singlex");

        assertEquals(3,
                calcBlankCollatePagesToAppend(createParms(1, 5, nUp, !DUPLEX)),
                "5 copies, 5 pages, 4-up, singlex");

        //
        assertEquals(4,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, DUPLEX)),
                "1 copy, 4 pages, 4-up, duplex");

        assertEquals(5,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)),
                "1 copy, 3 pages, 4-up, duplex");

        assertEquals(3,
                calcBlankCollatePagesToAppend(createParms(1, 5, nUp, DUPLEX)),
                "3 copies, 5 pages, 4-up, duplex");
    }

    @Test
    public void test2Up() {

        final int nUp = 2;

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, !DUPLEX)),
                "1 copy, 4 pages, 2-up, singlex");

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, !DUPLEX)),
                "10 copies, 4 pages, 2-up, singlex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)),
                "1 copy, 3 pages, 2-up, singlex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)),
                "5 copy, 5 pages, 2-up, singlex");

        //
        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, DUPLEX)),
                "1 copy, 4 pages, 2-up, duplex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)),
                "1 copy, 3 pages, 2-up, duplex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)),
                "3 copies, 5 pages, 2-up, duplex");
    }

    @Test
    public void test1Up() {

        final int nUp = 1;

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, !DUPLEX)),
                "1 copy, 4 pages, 1-up, singlex");

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, !DUPLEX)),
                "10 copies, 4 pages, 1-up, singlex");

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, !DUPLEX)),
                "1 copy, 3 pages, 1-up, singlex");

        //
        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(1, 4, nUp, DUPLEX)),
                "1 copy, 4 pages, 1-up, duplex");

        assertEquals(0,
                calcBlankCollatePagesToAppend(createParms(10, 4, nUp, DUPLEX)),
                "10 copies, 4 pages, 1-up, duplex");

        assertEquals(1,
                calcBlankCollatePagesToAppend(createParms(1, 3, nUp, DUPLEX)),
                "1 copy, 3 pages, 1-up, duplex");

    }

}
