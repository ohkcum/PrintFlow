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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.system.CommandExecutor;
import org.printflow.lite.core.system.ICommandExecutor;
import org.printflow.lite.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractFileConverter {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractFileConverter.class);

    /** */
    private static final int MAX_ERROR_LEN = 512;

    /**
     * .
     */
    protected enum ExecMode {
        /**
         * ONE call to {@link ICommandExecutor#executeCommand()} can be
         * performed at a point in time.
         */
        SINGLE_THREADED,

        /**
         * MULTIPLE calls to {@link ICommandExecutor#executeCommand()} can be
         * performed concurrently.
         */
        MULTI_THREADED
    }

    /**
     * .
     */
    protected enum ExecType {
        /**
         * Uses separate threads for capturing stdout and stdin. This prevents
         * deadlock when stdin and stdout fills up input stream buffers to the
         * max.
         */
        ADVANCED,

        /**
         * Single thread capturing of stdout and stdin after OS process has
         * finished.
         * <p>
         * <b>CAUTION</b>: this might lead to deadlock. <i>Use if you're
         * absolutely sure stdin and stdout output is small.</i>
         * </p>
         */
        SIMPLE
    }

    /** */
    private final ExecMode execMode;

    /** */
    private boolean hasStdout;

    /** */
    private boolean hasStderr;

    /**
     *
     * @param emode
     *            The {@link ExecMode}.
     */
    protected AbstractFileConverter(final ExecMode emode) {
        this.execMode = emode;
    }

    /**
     * @return {@link ExecMode}.
     */
    protected final ExecMode getExecMode() {
        return this.execMode;
    }

    /**
     * @return {@link ExecType}.
     */
    protected abstract ExecType getExecType();

    /**
     * @return {@link true} to log performance.
     */
    protected boolean logPerformance() {
        return false;
    }

    /**
     * Returns the OS shell command to perform the conversion.
     *
     * @param contentType
     *            The {@link DocContentTypeEnum} of the input file.
     * @param fileIn
     *            The input {@link File}.
     * @param filePdf
     *            The output PDF {@link File}.
     * @return The shell command, or {@code null} when no OS command is
     *         applicable and
     *         {@link #convertCustom(DocContentTypeEnum, File, File)} is used.
     */
    protected abstract String getOsCommand(DocContentTypeEnum contentType,
            File fileIn, File filePdf);

    /**
     * Creates a sibling of a File in the same directory with the file extension
     * of the offered {@link DocContentTypeEnum}.
     *
     * @param file
     *            The input file to create the sibling from.
     * @param contentType
     *            The content type if the sibling.
     * @return The sibling File object.
     */
    public static File getFileSibling(final File file,
            final DocContentTypeEnum contentType) {
        return getFileSibling(file, contentType, "");
    }

    /**
     * Creates a sibling of a File in the same directory with the file extension
     * of the offered {@link DocContentTypeEnum}.
     *
     * @param file
     *            The input file to create the sibling from.
     * @param contentType
     *            The content type if the sibling.
     * @param baseNameSuffix
     *            Suffix to append to the file base name.
     * @return The sibling File object.
     */
    public static File getFileSibling(final File file,
            final DocContentTypeEnum contentType, final String baseNameSuffix) {

        final StringBuilder builder = new StringBuilder(128);

        builder.append(file.getParent()).append(File.separator)
                .append(FilenameUtils.getBaseName(file.getAbsolutePath()))
                .append(baseNameSuffix).append(".")
                .append(DocContent.getFileExtension(contentType));

        return new File(builder.toString());
    }

    /**
     * Gets the output file.
     *
     * @param fileIn
     *            The input {@link File}.
     * @return The output {@link File}.
     */
    protected abstract File getOutputFile(File fileIn);

    /**
     * Notifies output on stdout.
     *
     * @param stdout
     *            Output on stdout.
     */
    protected abstract void onStdout(String stdout);

    /**
     * @return {@code true} if this converter gave stdout messages.
     */
    public boolean hasStdout() {
        return this.hasStdout;
    }

    /**
     * @return {@code true} if this converter gave stderr messages.
     */
    public boolean hasStderr() {
        return this.hasStderr;
    }

    /**
     * @return {@code true} if stdout messages must be reported.
     */
    protected boolean reportStdout() {
        return true;
    }

    /**
     * @return {@code true} if stderr messages must be reported.
     */
    protected boolean reportStderr() {
        return true;
    }

    /**
     * Performs a conversion using an OS Command.
     *
     * @param contentType
     *            The type of input file.
     * @param fileIn
     *            The file to convert.
     * @param filePdf
     *            The output file.
     * @param command
     *            The OS Command
     * @return The output file.
     * @throws DocContentToPdfException
     *             if error.
     */
    protected final File convertWithOsCommand(
            final DocContentTypeEnum contentType, final File fileIn,
            final File filePdf, final String command)
            throws DocContentToPdfException {

        final String pdfName = filePdf.getAbsolutePath();

        final Date startDate = new Date();

        LOGGER.debug(command);

        final ICommandExecutor exec;

        if (this.getExecType() == ExecType.SIMPLE) {
            exec = CommandExecutor.createSimple(command);
        } else {
            exec = CommandExecutor.create(command);
        }

        boolean pdfCreated = false;

        try {
            int rc = 1;

            if (this.execMode == ExecMode.SINGLE_THREADED) {
                synchronized (this) {
                    rc = exec.executeCommand();
                }
            } else {
                rc = exec.executeCommand();
            }

            final String stdout = exec.getStandardOutput();
            final String stderr = exec.getStandardError();

            this.hasStdout = StringUtils.isNotBlank(stdout);
            this.hasStderr = StringUtils.isNotBlank(stderr);

            if (this.hasStdout && this.reportStdout()) {
                LOGGER.debug("[{}] {}", command, stdout);
            }

            final String stderrMsg;

            if (this.hasStderr && this.reportStderr()) {
                final StringBuilder msg = new StringBuilder();
                msg.append(" ")
                        .append(StringUtils.abbreviate(stderr, MAX_ERROR_LEN));
                if (stderr.length() > MAX_ERROR_LEN) {
                    msg.append(" (and more)");
                }
                stderrMsg = msg.toString();
                LOGGER.warn("[{}]{}", command, stderrMsg);
            } else {
                stderrMsg = "";
            }

            if (rc != 0) {
                LOGGER.error("PDF could not be created. Exit status [{}] : {}",
                        rc, command);
                throw new DocContentToPdfException(
                        "PDF could not be created" + stderrMsg);
            }

            pdfCreated = true;

            if (filePdf.exists()) {

                if (this.logPerformance()) {
                    this.logPerformance(startDate, fileIn.getName());
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[" + pdfName + "] created.");
                }
            } else {
                LOGGER.error("[" + pdfName + "] NOT created.");
                throw new DocContentToPdfException("PDF is not created");
            }

            this.onStdout(stdout);

        } catch (IOException | InterruptedException e) {
            throw new SpException(e);
        } finally {
            if (!pdfCreated) {
                File file2Delete = new File(pdfName);
                if (file2Delete.exists()) {
                    file2Delete.delete();
                }
            }
        }
        return filePdf;
    }

    /**
     *
     * @param startDate
     * @param fileName
     */
    private void logPerformance(final Date startDate, final String fileName) {

        final Date now = new Date();
        final long time = now.getTime() - startDate.getTime();

        final long minutes =
                TimeUnit.MILLISECONDS.toHours(time) * DateUtil.MINUTES_IN_HOUR
                        + TimeUnit.MILLISECONDS.toMinutes(time)
                                % DateUtil.MINUTES_IN_HOUR;
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(time)
                % DateUtil.SECONDS_IN_MINUTE;

        final String msg = String.format("%s [%s] : %s",
                this.getClass().getSimpleName(), fileName,
                String.format("%02d:%02d (mm:ss)", minutes, seconds));

        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                msg);
    }
}
