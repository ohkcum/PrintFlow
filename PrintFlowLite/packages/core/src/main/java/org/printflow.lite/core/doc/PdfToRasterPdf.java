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

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.pdf.PDFBoxPdfCreator;
import org.printflow.lite.core.pdf.PdfResolutionEnum;
import org.printflow.lite.core.system.SystemInfo;
import org.printflow.lite.core.system.SystemInfo.ArgumentGS;

/**
 * Converts a PDF file to rasterized PDF using Ghostscript.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToRasterPdf extends AbstractFileConverter
        implements IPdfConverter {

    public enum Raster {
        /** */
        GRAYSCALE("pdfimage8"),
        /** */
        RGB("pdfimage24"),
        /** */
        CMYK("pdfimage32");

        /** */
        private final String device;

        Raster(final String d) {
            this.device = d;
        }

        private String getDevice() {
            return this.device;
        }
    }

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /** */
    private final Raster raster;

    /** */
    private final PdfResolutionEnum resolution;

    /**
     * @param dpi
     *            Resolution.
     * @return CLI parms.
     */
    private static String getCliParms(final PdfResolutionEnum dpi) {
        final String factor;
        switch (dpi) {
        case DPI_100:
            factor = "5";
            break;
        case DPI_150:
            factor = "4";
            break;
        case DPI_200:
            factor = "3";
            break;
        case DPI_300:
            factor = "2";
            break;
        case DPI_600:
            factor = "1";
            break;
        default:
            throw new SpException(
                    "Unhandled resolution ".concat(dpi.toString()));
        }
        return "-r600 -dDownScaleFactor=" + factor;
    }

    /**
     * @param rst
     *            Raster.
     * @param res
     *            Resolution.
     */
    public PdfToRasterPdf(final Raster rst, final PdfResolutionEnum res) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = null;
        this.raster = rst;
        this.resolution = res;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     * @param img
     *            Image.
     * @param res
     *            Image resolution.
     */
    public PdfToRasterPdf(final File createDir, final Raster img,
            final PdfResolutionEnum res) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = createDir;
        this.raster = img;
        this.resolution = res;
    }

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    @Override
    protected File getOutputFile(final File fileIn) {

        final StringBuilder builder = new StringBuilder(128);

        if (this.createHome == null) {
            builder.append(fileIn.getParent());
        } else {
            builder.append(this.createHome.getAbsolutePath());
        }

        builder.append(File.separator)
                .append(FilenameUtils.getBaseName(fileIn.getAbsolutePath()))
                .append("-imaged.")
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return new File(builder.toString());
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            cmd.append(SystemInfo.Command.GS.cmd()).append(" -sOutputFile=\"")
                    .append(fileOut.getCanonicalPath()).append("\" -sDEVICE=")
                    .append(this.raster.getDevice()).append(" ")
                    .append(getCliParms(this.resolution))
                    .append(" -dNOPAUSE -dBATCH ")
                    .append(ArgumentGS.STDOUT_TO_DEV_NULL.getArg())
                    .append(" \"").append(fileIn.getCanonicalPath())
                    .append("\"");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }

    @Override
    public File convert(final File fileIn) throws IOException {
        final File filePdf = getOutputFile(fileIn);
        try {
            final File intermediate = this.convertWithOsCommand(
                    DocContentTypeEnum.PDF, fileIn, filePdf,
                    this.getOsCommand(DocContentTypeEnum.PDF, fileIn, filePdf));
            /*
             * Convert command wipes PDF info and encryption, so copy from the
             * orginal.
             */
            return this.copyInfoAndEncryption(fileIn, intermediate);
        } catch (DocContentToPdfException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Saves target file to a new file with info and encryption copied from
     * source file. Deletes target file after new file is created.
     *
     * @param src
     *            source.
     * @param trg
     *            target.
     * @return new file.
     * @throws IOException
     */
    private File copyInfoAndEncryption(final File src, final File trg)
            throws IOException {
        final String trgPath = trg.getAbsolutePath();
        final File newFile = new File(FilenameUtils.getFullPath(trgPath)
                .concat("copy-").concat(FilenameUtils.getName(trgPath)));

        try (PDDocument docSrc = Loader.loadPDF(
                new RandomAccessReadBufferedFile(src.getAbsolutePath()));
                PDDocument docTrg =
                        Loader.loadPDF(new RandomAccessReadBufferedFile(
                                trg.getAbsolutePath()))) {

            PDFBoxPdfCreator.setInfoAndEncryption(docSrc, docTrg);

            docTrg.save(newFile);
            trg.delete();
        }
        return newFile;
    }

    @Override
    protected void onStdout(final String stdout) {
        // no code intended.
    }

    @Override
    protected boolean logPerformance() {
        return true;
    }
}
