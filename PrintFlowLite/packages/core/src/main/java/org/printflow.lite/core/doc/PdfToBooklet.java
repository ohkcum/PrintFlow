/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2024 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.doc;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.printflow.lite.core.pdf.PDFBoxPdfCreator;

/**
 * Converts a PDF file to 2-up duplex booklet page ordering using PDFBox.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToBooklet extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "booklet";

    /**
     * Number of virtual pages on 2-up duplex sheet.
     */
    private static final int PAGES_ON_SHEET = 4;

    /**
     *
     */
    public PdfToBooklet() {
        super();
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToBooklet(final File createDir) {
        super(createDir);
    }

    /**
     * Gets booklet page order.
     *
     * @param nSheets
     *            Number of booklet sheets.
     * @return Array with 1-based page ordinals.
     */
    private static int[] getPageOrder(final int nSheets) {

        final int[] pageOrder = new int[nSheets * PAGES_ON_SHEET];

        int nWlkEnd = nSheets * PAGES_ON_SHEET;
        int nWlkStart = 1;
        int j = 0;

        for (int i = 0; i < nSheets; i++) {
            pageOrder[j++] = nWlkEnd--;
            pageOrder[j++] = nWlkStart++;
            pageOrder[j++] = nWlkStart++;
            pageOrder[j++] = nWlkEnd--;
        }
        return pageOrder;
    }

    /**
     * Calculates the number of blank pages to be appended.
     *
     * @param nPages
     *            Number of pages in input PDF document.
     * @return Number of blank pages.
     */
    private static int calcBlankPages(final int nPages) {
        int nPagesBlank = 0;

        while (true) {
            if ((nPages + nPagesBlank) % PAGES_ON_SHEET == 0) {
                break;
            }
            nPagesBlank++;
        }
        return nPagesBlank;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = getOutputFile(pdfFile);

        try (PDDocument pdDocIn = Loader.loadPDF(pdfFile);
                PDDocument pdDocOut = new PDDocument()) {

            PDFBoxPdfCreator
                    .setProducerAndCreator(pdDocOut.getDocumentInformation());

            final int nPagesMax = pdDocIn.getNumberOfPages();
            final int nPagesBlank = calcBlankPages(nPagesMax);

            final int[] pageOrder =
                    getPageOrder((nPagesMax + nPagesBlank) / PAGES_ON_SHEET);

            // Use page size of 1st page for any blank page.
            final PDRectangle mediaBox = pdDocIn.getPage(0).getMediaBox();

            for (int i = 0; i < pageOrder.length; i++) {

                final int nPage = pageOrder[i];

                if (nPage > nPagesMax) {
                    pdDocOut.addPage(new PDPage(mediaBox));
                } else {
                    pdDocOut.addPage(pdDocIn.getPage(nPage - 1));
                }
            }

            pdDocOut.save(pdfOut);
        }

        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
