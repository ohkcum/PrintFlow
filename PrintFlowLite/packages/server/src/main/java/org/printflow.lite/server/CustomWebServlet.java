/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.protocol.http.WebApplication;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.ServerFilePathEnum;
import org.printflow.lite.core.config.ServerUrlPath;
import org.printflow.lite.core.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers files for web customization.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "CustomWebServlet",
        urlPatterns = { CustomWebServlet.SERVLET_URL_PATTERN })
public final class CustomWebServlet extends HttpServlet {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CustomWebServlet.class);

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final int BUFFER_SIZE = 1014;

    /**
     * Base path of custom web files (without leading or trailing '/').
     */
    public static final String URL_PATH_BASE = ServerUrlPath.CUSTOM_WEB;

    /**
     * Base path of custom web themes (without leading or trailing '/').
     */
    public static final String PATH_BASE_THEMES =
            ServerUrlPath.CUSTOM_WEB_THEMES;

    /** */
    public static final String SERVLET_URL_PATTERN =
            "/" + CustomWebServlet.URL_PATH_BASE + "/*";

    /**
     * Writes error.
     *
     * @param resp
     *            response.
     * @param ostr
     *            output.
     * @param file
     *            error subject
     * @param error
     *            error message.
     * @throws IOException
     *             if IO error.
     */
    private void writeErrorMsg(final HttpServletResponse resp,
            final OutputStream ostr, final File file, final String error)
            throws IOException {

        LOGGER.debug(String.format("%s [%s]", error, file.getAbsolutePath()));

        final byte[] msg = error.getBytes();

        resp.setContentType(WebApplication.get().getMimeType("x.txt"));
        resp.setContentLength(msg.length);

        ostr.write(msg);
    }

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        final String reqURI = req.getRequestURI();

        // Map URI to relative server file path.
        final String relativeFilePath = reqURI.replace(ServerUrlPath.CUSTOM_WEB,
                ServerFilePathEnum.CUSTOM_WEB.getPath());

        InputStream istr = null;

        try {

            final OutputStream ostr = resp.getOutputStream();

            // Create File on full file path.
            final File file = new File(
                    ConfigManager.getServerHome().concat(relativeFilePath));

            if (file.exists()) {

                if (file.isDirectory()) {
                    writeErrorMsg(resp, ostr, file,
                            String.format("%s is not a file.", reqURI));

                } else {

                    resp.setContentType(
                            WebApplication.get().getMimeType(reqURI));
                    resp.setContentLength((int) file.length());

                    istr = new FileInputStream(file);
                    final byte[] aByte = new byte[BUFFER_SIZE];

                    int nBytes = istr.read(aByte);
                    while (-1 < nBytes) {
                        ostr.write(aByte, 0, nBytes);
                        nBytes = istr.read(aByte);
                    }
                }
            } else {
                writeErrorMsg(resp, ostr, file,
                        String.format("%s does not exist.", reqURI));

            }

            resp.setStatus(HttpServletResponse.SC_OK);

        } finally {
            IOHelper.closeQuietly(istr);
        }
    }
}
