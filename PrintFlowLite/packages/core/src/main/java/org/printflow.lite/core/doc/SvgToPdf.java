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
package org.printflow.lite.core.doc;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.pdf.PDFBoxPdfCreator;
import org.printflow.lite.core.system.SystemInfo;

/**
 * SVG file convert to PDF.
 *
 * @author Rijk Ravestein
 *
 */
public final class SvgToPdf extends AbstractDocFileConverter {

    /**
     *
     */
    public SvgToPdf() {
        super(ExecMode.MULTI_THREADED);
    }

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    @Override
    protected File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {
        return SystemInfo.Command.RSVG_CONVERT.cmdLineExt("-f pdf", "-o",
                fileOut.getAbsolutePath(), fileIn.getAbsolutePath());
    }

    @Override
    public boolean notifyStdOutMsg() {
        return this.hasStdout();
    }

    /**
     * Converts SVG to PDF and scales the PDF to mediabox dimensions.
     *
     * @param fileIn
     *            Input file.
     * @param mediaboxTarget
     *            Mediabox to scale to.
     * @return result Converted (scaled) file.
     * @throws DocContentToPdfException
     * @throws UnavailableException
     * @throws IOException
     */
    public File convert(final File fileIn, final PDRectangle mediaboxTarget)
            throws DocContentToPdfException, UnavailableException, IOException {

        final File convertedFile = this.convert(DocContentTypeEnum.SVG, fileIn);

        try (PDDocument convertedDoc = Loader
                .loadPDF(new RandomAccessReadBufferedFile(convertedFile))) {

            final PDDocument scaledDoc = PDFBoxPdfCreator
                    .scaleToMediabox(convertedDoc.getPage(0), mediaboxTarget);

            if (scaledDoc != null) {
                scaledDoc.save(convertedFile);
            }
        }
        return convertedFile;
    }

}
