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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.printflow.lite.core.system.SystemInfo;

/**
 * Use {@link SystemInfo.Command#CUPSFILTER} to convert PDF to spool file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToSpoolFile extends AbstractDocFileConverter {

    /** */
    public static final class FilterParms {

        private File ppd;
        private String destinationMimetype;

        private String userName;
        private int copies;
        private String title;
        private Map<String, String> options;

        public FilterParms() {
            this.options = new HashMap<>();
        }

        public File getPPD() {
            return ppd;
        }

        public void setPPD(final File ppdFile) {
            this.ppd = ppdFile;
        }

        public String getDestinationMimetype() {
            return destinationMimetype;
        }

        public void setDestinationMimetype(String destinationMimetype) {
            this.destinationMimetype = destinationMimetype;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public int getCopies() {
            return copies;
        }

        public void setCopies(final int copies) {
            this.copies = copies;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void addOption(final String key, final String value) {
            this.options.put(key, value);
        }

    }

    /** */
    private final FilterParms filterParms;

    /**
     * @param parms
     *            filter parameters.
     */
    public PdfToSpoolFile(final FilterParms parms) {
        super(ExecMode.MULTI_THREADED);
        this.filterParms = parms;
    }

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    @Override
    protected File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.TXT, "-spoolfile");
    }

    /**
     * TEST.
     *
     * @param fileIn
     * @param fileOut
     * @return CLI command
     */
    public String getOsCommand(final File fileIn, final File fileOut) {
        return this.getOsCommand(DocContentTypeEnum.PDF, fileIn, fileOut);
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {

        final StringBuilder options = new StringBuilder();

        for (final Entry<String, String> entry : this.filterParms.getOptions()
                .entrySet()) {
            options.append("-o ").append(entry.getKey()).append("=")
                    .append(entry.getValue());
            options.append(" ");
        }

        final String destinationMimeType;
        if (this.filterParms.getDestinationMimetype() == null) {
            destinationMimeType = MimeTypeEnum.APPLICATION_PDF.getWord();
        } else {
            destinationMimeType = this.filterParms.getDestinationMimetype();
        }

        final String cmd = SystemInfo.Command.CUPSFILTER.cmdLineExt(//
                "-e", // Use every filter from the PPD file.
                "-U", this.filterParms.getUserName(), //
                "-n", Integer.toString(this.filterParms.getCopies()), //
                "-t",
                String.format("\"%s\"",
                        this.filterParms.getTitle().replace("\"", "\\\"")), //
                "-p", this.filterParms.getPPD().getAbsolutePath(), //
                options.toString().strip(), //
                "-i", MimeTypeEnum.APPLICATION_PDF.getWord(), //
                "-m", destinationMimeType, //
                fileIn.getAbsolutePath(), ">", fileOut.getAbsolutePath());
        return cmd;
    }

    @Override
    protected boolean reportStderr() {
        /*
         * `cupsfilter` writes DEBUG messages to stderr (!) How to suppress
         * them? For now, ignore them.
         */
        return false;
    }

    @Override
    public boolean notifyStdOutMsg() {
        return false;
    }

}
