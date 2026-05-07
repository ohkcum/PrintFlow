/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server.dropzone;

/**
 * The resource that handles DropZone file uploads. It reads the file items from
 * the request parameters and validates the PDF file(s).
 * <p>
 * Additionally it writes the response's content type and body.
 * </p>
 * <p>
 * Checks for max upload size and supported file type are also done at the
 * client (JavaScript) side, before sending the file(s).
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfValidateDropZoneFileResource
        extends AbstractDropZoneFileResource {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    public PdfValidateDropZoneFileResource() {
    }

    @Override
    protected ResourceResponse
            newResourceResponse(final Attributes attributes) {

        return this.newResourceResponse(PdfValidateUploadHelper.instance(),
                attributes);
    }

}
