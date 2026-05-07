/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.dto.AbstractDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class PdfOutDto extends AbstractDto {

    private Boolean removeGraphics;
    private Boolean ecoprint;
    private Boolean grayscale;
    private Boolean rasterize;
    private Boolean links;

    public Boolean getRemoveGraphics() {
        return removeGraphics;
    }

    public void setRemoveGraphics(Boolean removeGraphics) {
        this.removeGraphics = removeGraphics;
    }

    public Boolean getEcoprint() {
        return ecoprint;
    }

    public void setEcoprint(Boolean ecoprint) {
        this.ecoprint = ecoprint;
    }

    public Boolean getGrayscale() {
        return grayscale;
    }

    public void setGrayscale(Boolean grayscale) {
        this.grayscale = grayscale;
    }

    public Boolean getRasterize() {
        return rasterize;
    }

    public void setRasterize(Boolean rasterize) {
        this.rasterize = rasterize;
    }

    public Boolean getLinks() {
        return links;
    }

    public void setLinks(Boolean links) {
        this.links = links;
    }

    /**
     * @param json
     *            JSON string.
     * @return {@code null} if JSON is blank or invalid.
     */
    public static PdfOutDto create(final String json) {
        if (!StringUtils.isBlank(json)) {
            try {
                return create(PdfOutDto.class, json);
            } catch (Exception e) {
                // noop
            }
        }
        return null;
    }
}
