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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.printflow.lite.core.json.PdfProperties;
import org.printflow.lite.core.pdf.PDFBoxPdfCreator;

/**
 * Converts a PDF file to an encrypted PDF using PDFBox.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToEncryptedPdf extends AbstractPdfConverter
        implements IPdfConverter {

    /** */
    public enum EncryptionKeyLength {
        /** */
        L40(40),
        /** */
        L128(128),
        /** */
        L256(256);

        /** */
        private final int length;

        EncryptionKeyLength(final int len) {
            this.length = len;
        }

        /**
         * @return Length of encryption key.
         */
        public int getLength() {
            return this.length;
        }
    }

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "encrypted";

    /** */
    private PdfProperties.PdfAllow pdfAllow;
    /** */
    private String pdfOwnerPass;
    /** */
    private String pdfUserPass;

    /**
     *
     * @param allow
     *            PDF permissions.
     * @param ownerPass
     *            PDF owner password.
     * @param userPass
     *            PDF user password.
     */
    public PdfToEncryptedPdf(final PdfProperties.PdfAllow allow,
            final String ownerPass, final String userPass) {
        super();
        this.pdfAllow = allow;
        this.pdfOwnerPass = ownerPass;
        this.pdfUserPass = userPass;
    }

    /**
     * Creates {@link StandardProtectionPolicy}.
     *
     * @param pdfAllow
     *            PDF permissions.
     * @param pdfOwnerPass
     *            PDF owner password.
     * @param pdfUserPass
     *            PDF user password.
     * @return the policy.
     */
    public static StandardProtectionPolicy createStandardProtectionPolicy(
            final PdfProperties.PdfAllow pdfAllow, final String pdfOwnerPass,
            final String pdfUserPass) {

        final AccessPermission accessPerm = new AccessPermission();

        accessPerm.setCanAssembleDocument(pdfAllow.getAssembly());
        accessPerm.setCanExtractContent(pdfAllow.getCopy());
        accessPerm.setCanExtractForAccessibility(pdfAllow.getCopyForAccess());
        accessPerm.setCanFillInForm(pdfAllow.getFillin());
        accessPerm.setCanModify(pdfAllow.getModifyContents());
        accessPerm.setCanModifyAnnotations(pdfAllow.getModifyAnnotations());
        accessPerm.setCanPrint(pdfAllow.getPrinting());
        accessPerm.setCanPrintFaithful(pdfAllow.getDegradedPrinting());

        final StandardProtectionPolicy spp = new StandardProtectionPolicy(
                pdfOwnerPass, pdfUserPass, accessPerm);

        spp.setEncryptionKeyLength(EncryptionKeyLength.L256.getLength());

        spp.setPermissions(accessPerm);

        return spp;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = this.getOutputFile(pdfFile);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFBoxPdfCreator.setProducer(document.getDocumentInformation());

            document.protect(createStandardProtectionPolicy(this.pdfAllow,
                    this.pdfOwnerPass, this.pdfUserPass));

            document.save(pdfOut);
        }
        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
