/*
 * This file is part of the PrintFlowLite project <http://PrintFlowLite.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.core.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.printflow.lite.core.config.ConfigManager;

/**
 * Create a PDF file from {@link DocContentTypeEnum#EML}.
 *
 * @author Rijk Ravestein
 *
 */
public final class HEIFToPdf implements IStreamConverter {

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrPdf)
            throws Exception {

        final File fileHEIF =
                ConfigManager.createAppTmpFile(this.getClass().getSimpleName());

        try {
            /*
             * Step 1 : HEIF to file.
             */
            FileUtils.copyInputStreamToFile(istrDoc, fileHEIF);

            final long lengthHEIF = fileHEIF.length();

            /*
             * Step 2 : HEIF file to JPEG file.
             */
            final HEIFToJPEG toJPEG = new HEIFToJPEG();
            final File fileJPEG = toJPEG.convert(contentType, fileHEIF);

            /*
             * Step 3 : JPEG input stream to PDF outputstream.
             */
            try (InputStream istrJPEG = new FileInputStream(fileJPEG);
                    DocInputStream docistr = new DocInputStream(istrJPEG)) {

                final ImageToPdf toPDF = new ImageToPdf();
                toPDF.convert(DocContentTypeEnum.JPEG, docistr, ostrPdf);

            } finally {
                fileJPEG.delete();
            }

            return lengthHEIF;

        } finally {
            fileHEIF.delete();
        }

    }

}
