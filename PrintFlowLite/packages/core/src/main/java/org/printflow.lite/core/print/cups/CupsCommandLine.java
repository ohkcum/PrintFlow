/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.print.cups;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.system.CommandExecutor;
import org.printflow.lite.core.system.ICommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CUPS <a href="https://www.cups.org/doc/admin.html">Command-Line Printer
 * Administration</a> wrapper.
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsCommandLine implements IUtility {

    /** Utility class. */
    private CupsCommandLine() {
    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CupsCommandLine.class);

    /**
     * {@code -h} option value.
     */
    private static final String CUPS_HOST_PORT =
            ConfigManager.getDefaultCupsHost().concat(":")
                    .concat(ConfigManager.getCupsPort());

    /** */
    private static final int MAX_ERROR_LEN = 512;

    /**
     * Configure cups printers and classes.
     */
    private static final String CMD_LPADMIN = "lpadmin";

    /**
     * Print cups status information.
     */
    private static final String CMD_LPSTAT = "lpstat";

    /**
     * Show available devices or drivers (deprecated).
     */
    private static final String CMD_LPINFO = "lpinfo";

    /** */
    private static class Result {

        final int rc;
        final String stdout;

        Result(final int r, final String s) {
            this.rc = r;
            this.stdout = s;
        }

        boolean isSuccess() {
            return this.rc == 0;
        }
    }

    /**
     *
     * @param cmd
     *            CUPS command
     * @param options
     *            CLI options
     * @return stdout
     * @throws InterruptedException
     * @throws IOException
     */
    private static synchronized Result execOsCommand(final String cmd,
            final String options) throws IOException, InterruptedException {

        final String command =
                String.format("%s -h %s %s", cmd, CUPS_HOST_PORT, options);

        LOGGER.debug(command);

        final ICommandExecutor exec = CommandExecutor.create(command);

        final int rc = exec.executeCommand();

        final String stdout = exec.getStandardOutput();
        final String stderr = exec.getStandardError();

        final boolean hasStdout = StringUtils.isNotBlank(stdout);
        final boolean hasStderr = StringUtils.isNotBlank(stderr);

        if (hasStdout) {
            LOGGER.debug("[{}] {}", command, stdout);
        }

        final String stderrMsg;

        if (hasStderr) {
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
            throw new SpException(stderrMsg);
        }

        return new Result(rc, stdout);
    }

    /**
     * Creates or modifies a printer.
     *
     * @param name
     * @param description
     * @param location
     * @param model
     * @param deviceUri
     * @return {@code true} if successful.
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean addModifyPrinter(final String name,
            final String description, final String location, final String model,
            final String deviceUri) throws IOException, InterruptedException {

        final StringBuilder cmd = new StringBuilder();

        cmd.append(" -p ").append(name);
        cmd.append(" -D \"").append(description).append('\"');
        cmd.append(" -m ").append(model);
        cmd.append(" -L \"").append(location).append('\"');
        cmd.append(" -v ").append(deviceUri);

        // Enables the printer and accepts new print jobs.
        cmd.append(" -E ");

        return execOsCommand(CMD_LPADMIN, cmd.toString()).isSuccess();
    }

    /**
     * Deletes a printer.
     *
     * @param printerName
     * @return {@code true} if successful.
     * @throws InterruptedException
     * @throws IOException
     */
    public static boolean deletePrinter(final String printerName)
            throws IOException, InterruptedException {

        final StringBuilder cmd = new StringBuilder();

        cmd.append("-x ").append(printerName);

        return execOsCommand(CMD_LPADMIN, cmd.toString()).isSuccess();
    }

    /**
     * Shows all available destinations on the local network.
     *
     * @return stdout
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getPrinters()
            throws IOException, InterruptedException {
        return execOsCommand(CMD_LPSTAT, "-e").stdout;
    }

    /**
     * List all of the available drivers ("models") on CUPS system.
     *
     * @return stdout
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getAvailableDrivers()
            throws IOException, InterruptedException {
        return execOsCommand(CMD_LPINFO, "-m").stdout;
    }

    /**
     * @param printerName
     *            CUPS printer name.
     * @param options
     *            IPP (PPD) keyword/value pairs.
     * @return {@code true} if successful.
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean setPrinterOptions(final String printerName,
            final Map<String, String> options)
            throws IOException, InterruptedException {

        final StringBuilder cmd = new StringBuilder();

        cmd.append("-p ").append(printerName);

        for (Entry<String, String> opt : options.entrySet()) {
            cmd.append(" -o ").append(opt.getKey()).append("=")
                    .append(opt.getValue());
        }

        return execOsCommand(CMD_LPADMIN, cmd.toString()).isSuccess();
    }
}
