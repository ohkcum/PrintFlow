/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2024 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.services.helpers;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.json.JsonAbstractBase;
import org.printflow.lite.core.pdf.PdfInfoDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ PdfPrintInData.JSON_PDFINFO })
public final class PdfPrintInData extends JsonAbstractBase
        implements ExternalSupplierData {

    /** */
    public static final String JSON_PDFINFO = "pdf";

    @JsonProperty(JSON_PDFINFO)
    private PdfInfoDto pdfInfo;

    public PdfInfoDto getPdfInfo() {
        return pdfInfo;
    }

    public void setPdfInfo(PdfInfoDto pdfInfo) {
        this.pdfInfo = pdfInfo;
    }

    @Override
    public String dataAsString() {
        try {
            return this.stringify();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    /**
     * @param dto
     * @return {@link PdfPrintInData}
     */
    public static PdfPrintInData create(final PdfInfoDto dto) {
        final PdfPrintInData obj = new PdfPrintInData();
        obj.setPdfInfo(dto);
        return obj;
    }

    /**
     * Creates an object from data string.
     *
     * @param json
     *            The serialized data.
     * @return The {@link PdfPrintInData} object or {@code null} if JSON is
     *         blank or de-serialization failed.
     */
    public static PdfPrintInData createFromData(final String json) {
        if (!StringUtils.isBlank(json)) {
            try {
                return PdfPrintInData.create(PdfPrintInData.class, json);
            } catch (Exception e) {
                // noop
            }
        }
        return null;
    }

}
