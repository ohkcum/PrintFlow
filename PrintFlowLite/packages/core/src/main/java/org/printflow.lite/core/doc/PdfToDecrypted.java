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
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.system.SystemInfo;

/**
 * Decrypts a PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToDecrypted extends AbstractFileConverter
        implements IPdfConverter {

    /**
     * The directory location of the created file (can be {@code null}).
     */
    private final File createHome;

    /**
     * @return {@code true} if this convertor is available.
     */
    public static boolean isAvailable() {
        return SystemInfo.isQPdfInstalled();
    }

    /**
     *
     */
    public PdfToDecrypted() {
        super(ExecMode.MULTI_THREADED);
        this.createHome = null;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToDecrypted(final File createDir) {
        super(ExecMode.MULTI_THREADED);
        this.createHome = createDir;
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
                .append("-decrypted.")
                .append(DocContent.getFileExtension(DocContentTypeEnum.PDF));

        return new File(builder.toString());
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder cmd = new StringBuilder(128);

        try {
            cmd.append(SystemInfo.Command.QPDF.cmd());
            cmd.append(" --decrypt");

            // See Mantis #1333
            final boolean ignoreWarnings = true; // Fixed value for now.

            if (ignoreWarnings) {
                /*
                 * Warnings are echoed on stderr and therefore logged in
                 * server.log by PrintFlowLite. We do NOT use the "--no-warn" option
                 * that suppresses warning messages.
                 */
                // cmd.append(" --no-warn");

                /*
                 * In case of warnings use exit code zero (0), this makes
                 * PrintFlowLite accept the decrypted result.
                 */
                cmd.append(" --warning-exit-0");
            }
            cmd.append(" \"").append(fileIn.getCanonicalPath()).append("\" \"")
                    .append(fileOut.getCanonicalPath()).append("\"");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        return cmd.toString();
    }

    @Override
    public File convert(final File fileIn) throws IOException {
        final File filePdf = getOutputFile(fileIn);
        try {
            return convertWithOsCommand(DocContentTypeEnum.PDF, fileIn, filePdf,
                    getOsCommand(DocContentTypeEnum.PDF, fileIn, filePdf));
        } catch (DocContentToPdfException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    protected void onStdout(final String stdout) {
        // no code intended.
    }

}
