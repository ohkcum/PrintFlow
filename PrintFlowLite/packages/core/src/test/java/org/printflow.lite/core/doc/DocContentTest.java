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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class DocContentTest {

    /**
    *
    */
    @Test
    public final void fileExtentionTest() {
        assertEquals(DocContent.FILENAME_EXT_PDF,
                DocContent.getFileExtension(DocContentTypeEnum.PDF),
                "PDF file extension");
        assertEquals(DocContent.FILENAME_EXT_PNG,
                DocContent.getFileExtension(DocContentTypeEnum.PNG),
                "PNG file extension");
        assertEquals(DocContent.FILENAME_EXT_PS,
                DocContent.getFileExtension(DocContentTypeEnum.PS),
                "PS file extension");
    }

    /**
    *
    */
    @Test
    public final void getContentTypeTest() {
        assertEquals(DocContentTypeEnum.DOC,
                DocContent.getContentTypeFromFile("file.doc"),
                "DOC content type from filename");
        assertEquals(DocContentTypeEnum.DOCX,
                DocContent.getContentTypeFromFile("file.docx"),
                "DOCX content type from filename");
        assertEquals(DocContentTypeEnum.ODT,
                DocContent.getContentTypeFromFile("file.odt"),
                "ODT content type from filename");
        assertEquals(DocContentTypeEnum.RTF,
                DocContent.getContentTypeFromFile("file.rtf"),
                "RTF content type from filename");
        assertEquals(DocContentTypeEnum.SVG,
                DocContent.getContentTypeFromFile("file.svg"),
                "SVG content type from filename");
        assertEquals(DocContentTypeEnum.SXW,
                DocContent.getContentTypeFromFile("file.sxw"),
                "SXW content type from filename");
        assertEquals(DocContentTypeEnum.XPS,
                DocContent.getContentTypeFromFile("PrintFlowLite-test.xps"),
                "XPS content type from filename");
    }

    /**
    *
    */
    @Test
    public final void getContentTypeByMimeTest() {
        assertEquals(DocContentTypeEnum.PDF,
                DocContent.getContentTypeFromMime(DocContent.MIMETYPE_PDF),
                "PDF content type from mime");
        assertNotSame(DocContentTypeEnum.DOC,
                DocContent.getContentTypeFromMime(DocContent.MIMETYPE_PDF),
                "PDF content type from mime");
    }

    /**
     *
     */
    @Test
    public final void fileSiblingTest() {

        final String dir = "/some/random/path/temp";
        final String pathIn = dir + "/x.doc";
        final String pathOut = dir + "/x." + DocContent.FILENAME_EXT_PDF;

        final File fileIn = new File(pathIn);

        final File fileOut = AbstractDocFileConverter.getFileSibling(fileIn,
                DocContentTypeEnum.PDF);

        assertEquals(fileOut.getAbsolutePath(), pathOut, "x.doc to x.pdf");
    }

}
