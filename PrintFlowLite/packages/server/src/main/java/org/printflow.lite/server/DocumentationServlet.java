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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.ServerFilePathEnum;
import org.printflow.lite.core.config.ServerUrlPath;

/**
 * Delivers static documentation, like the User Manual (DocBook) and the Third
 * Party License Information.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "DocumentationServlet",
        urlPatterns = { DocumentationServlet.SERVLET_URL_PATTERN })
public final class DocumentationServlet extends HttpServlet {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final int BUFFER_SIZE = 1014;

    /**
     * Base path of the documentation web files (without leading or trailing
     * '/').
     */
    public static final String URL_PATH_BASE = ServerUrlPath.DOCS;

    /** */
    public static final String SERVLET_URL_PATTERN = "/" + URL_PATH_BASE + "/*";

    /**
     * @param reqURI
     * @param resp
     * @throws IOException
     */
    private void handleNotFound(final String reqURI,
            final HttpServletResponse resp) throws IOException {

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType(WebApplication.get().getMimeType("x.txt"));

        final byte[] msg = String.format("%s: not found", reqURI).getBytes();

        resp.setContentLength(msg.length);
        resp.getOutputStream().write(msg);
    }

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        final String reqURI = req.getRequestURI();

        final String reqURIWrk = StringUtils.removeEnd(reqURI, "/");
        if (reqURIWrk.endsWith(ServerUrlPath.DOCS_MANUAL)
                || reqURIWrk.endsWith(ServerUrlPath.DOCS_LICENSES)) {
            resp.sendRedirect(
                    reqURIWrk + "/" + ServerUrlPath.LANDING_PAGE_HTML_FILE);
            return;
        }

        // URL must point to a specific document.
        if (reqURI.endsWith("/")) {
            this.handleNotFound(reqURI, resp);
            return;
        }

        // Map URI to relative server file path.
        String relativeFilePath = reqURI.replace(ServerUrlPath.DOCS_MANUAL,
                ServerFilePathEnum.DOCS_MANUAL.getPath());

        if (relativeFilePath.equals(reqURI)) {
            relativeFilePath = reqURI.replace(ServerUrlPath.DOCS_LICENSES,
                    ServerFilePathEnum.DOCS_LICENSES.getPath());
        }

        // Create File on full file path.
        final File file = new File(
                ConfigManager.getServerHome().concat(relativeFilePath));

        if (!file.exists()) {
            this.handleNotFound(reqURI, resp);
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);

        resp.setContentType(WebApplication.get().getMimeType(reqURI));
        resp.setContentLength((int) file.length());

        final OutputStream ostr = resp.getOutputStream();

        try (InputStream istr = new FileInputStream(file);) {

            final byte[] aByte = new byte[BUFFER_SIZE];

            int nBytes = istr.read(aByte);

            while (-1 < nBytes) {
                ostr.write(aByte, 0, nBytes);
                nBytes = istr.read(aByte);
            }
        }
    }
}
