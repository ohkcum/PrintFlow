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
import org.printflow.lite.core.json.PdfProperties;
import org.printflow.lite.core.pdf.PDFBoxPdfCreator;
import org.printflow.lite.core.pdf.PDFBoxUrlAnnotator;

/**
 * Converts a PDF file to a PDF with annotated URL and optional PDF encryption
 * using PDFBox.
 *
 * @author Rijk Ravestein
 *
 */
public final class PDFBoxPdfToAnnotatedURL extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "annotated-url";

    /** */
    private final boolean pdfEncryption;

    /** */
    private PdfProperties.PdfAllow pdfAllow;
    /** */
    private PdfProperties.PdfLinks pdfLinks;

    /** */
    private String pdfOwnerPass;
    /** */
    private String pdfUserPass;

    /**
     * @param links
     *            PDF link specification.
     */
    public PDFBoxPdfToAnnotatedURL(final PdfProperties.PdfLinks links) {
        super();
        this.pdfLinks = links;
        this.pdfEncryption = false;
    }

    /**
     *
     * @param allow
     *            PDF permissions.
     * @param links
     *            PDF link specification.
     * @param ownerPass
     *            PDF owner password.
     * @param userPass
     *            PDF user password.
     */
    public PDFBoxPdfToAnnotatedURL(final PdfProperties.PdfAllow allow,
            final PdfProperties.PdfLinks links, final String ownerPass,
            final String userPass) {
        super();
        this.pdfEncryption = true;
        this.pdfAllow = allow;
        this.pdfLinks = links;
        this.pdfOwnerPass = ownerPass;
        this.pdfUserPass = userPass;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PDFBoxPdfToAnnotatedURL(final File createDir) {
        super(createDir);
        this.pdfEncryption = false;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFBoxPdfCreator.setProducer(document.getDocumentInformation());

            if (this.pdfEncryption) {
                document.protect(PdfToEncryptedPdf
                        .createStandardProtectionPolicy(this.pdfAllow,
                                this.pdfOwnerPass, this.pdfUserPass));
            }

            if (this.pdfLinks != null) {
                PDFBoxUrlAnnotator.annotate(document, this.pdfLinks);
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
