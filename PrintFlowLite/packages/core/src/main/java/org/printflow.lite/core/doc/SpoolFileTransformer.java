/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.print.proxy.SpoolFileTransformRules;
import org.printflow.lite.core.print.proxy.SpoolFileTransformRules.PJLHeader;
import org.printflow.lite.core.print.proxy.SpoolFileTransformRules.Replace;
import org.printflow.lite.core.util.FileSystemHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class SpoolFileTransformer implements IDocConverter {

    /** */
    private static final String PFX_PJL = "@PJL";

    /** */
    private String[][] pjlSearchReplace;

    /** */
    public SpoolFileTransformer() {
    }

    /**
     * @param line
     * @return string with replacements
     */
    private String searchReplace(final String line) {
        return StringUtils.replaceEach(line, this.pjlSearchReplace[0],
                this.pjlSearchReplace[1]);
    }

    /**
     * Creates a 2-dimensional array with arrays for
     * {@link StringUtils#replaceEach(String, String[], String[])}.
     *
     * @param rules
     * @return array
     */
    private static String[][]
            getPJLHeaderSearchReplace(final SpoolFileTransformRules rules) {

        String[][] searchReplaceArray = new String[2][0];

        if (rules.getPjl() != null) {
            final SpoolFileTransformRules.PJL pjl = rules.getPjl();

            if (pjl.getPjlHeader() != null) {

                final PJLHeader header = pjl.getPjlHeader();

                if (header.getReplace() != null) {

                    final List<Replace> replaceList = header.getReplace();

                    searchReplaceArray = new String[2][replaceList.size()];

                    int i = 0;
                    for (final Replace replace : replaceList) {
                        searchReplaceArray[0][i] = replace.getSearch();
                        searchReplaceArray[1][i] = replace.getReplacement();
                        i++;
                    }
                }
            }
        }
        return searchReplaceArray;
    }

    /**
     * @param istr
     * @param ostr
     * @throws IOException
     */
    public void transform(final InputStream istr, final OutputStream ostr)
            throws IOException {

        ByteArrayOutputStream bostrLineWlk = null;
        int nLineCounter = 0;
        boolean processHeader = true;
        int chWlk;

        while ((chWlk = istr.read()) != -1) {

            if (processHeader) {

                if (bostrLineWlk == null) {
                    bostrLineWlk = new ByteArrayOutputStream();
                }

                bostrLineWlk.write(chWlk);

                if (chWlk == '\n') {

                    bostrLineWlk.flush();

                    String headerLine = bostrLineWlk.toString();
                    final boolean isPJL =
                            headerLine.strip().startsWith(PFX_PJL);

                    if (isPJL) {
                        headerLine = this.searchReplace(headerLine);
                    }
                    ostr.write(headerLine.getBytes());

                    processHeader = nLineCounter == 0 || isPJL;
                    nLineCounter++;
                    bostrLineWlk = null;
                }
            } else {
                ostr.write(chWlk);
            }
        }
    }

    /**
     * @param spoolfile
     * @param rules
     * @throws IOException
     */
    public void transform(final File spoolfile,
            final SpoolFileTransformRules rules) throws IOException {

        this.pjlSearchReplace = getPJLHeaderSearchReplace(rules);

        final File transformedFile = AbstractFileConverter.getFileSibling(
                spoolfile, DocContentTypeEnum.TXT, "-transformed");

        try {
            try (InputStream istr = new FileInputStream(spoolfile);
                    OutputStream ostr = new FileOutputStream(transformedFile)) {

                this.transform(istr, ostr);
            }
            FileSystemHelper.replaceWithNewVersion(spoolfile, transformedFile);

        } finally {
            if (transformedFile.exists()) {
                transformedFile.delete();
            }
        }

    }

}
