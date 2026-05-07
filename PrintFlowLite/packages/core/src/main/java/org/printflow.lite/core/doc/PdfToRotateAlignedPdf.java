/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.doc;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.printflow.lite.core.pdf.PDFBoxPdfCreator;
import org.printflow.lite.core.pdf.PdfPageRotateHelper;

/**
 * Converts a PDF file to a PDF with pages rotated to a requested orientation
 * using PDFBox.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToRotateAlignedPdf extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "rotated";

    /**
     * If {@code true}, pages are aligned to landscape orientation.
     */
    private final boolean alignLandscape;

    /**
     * A set with one-based PDF page numbers to be rotated to same orientation
     * as first page in a PDF document.
     */
    private final Set<Integer> pageNumbers;

    /**
     * @param alignToLandscape
     *            If {@code true}, pages are aligned to landscape orientation.
     * @param pages
     *            A set with one-based PDF page numbers to be rotated to same
     *            orientation as first page in a PDF document.
     */
    public PdfToRotateAlignedPdf(final boolean alignToLandscape,
            final Set<Integer> pages) {
        super();
        this.alignLandscape = alignToLandscape;
        this.pageNumbers = pages;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFBoxPdfCreator.setProducer(document.getDocumentInformation());

            for (final Integer entry : this.pageNumbers) {

                final PDPage page = document.getPage(entry.intValue() - 1);

                final int alignedRotation = PdfPageRotateHelper
                        .getAlignedRotation(page, this.alignLandscape);

                page.setRotation(alignedRotation);
            }
            document.save(pdfOut);
        }
        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
