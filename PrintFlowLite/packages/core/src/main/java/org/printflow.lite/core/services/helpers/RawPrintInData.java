/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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
import java.util.Map;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * PrintIn data mapped to IPP attributes.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ RawPrintInData.JSON_IPP_MAPPING })
public final class RawPrintInData extends JsonAbstractBase
        implements ExternalSupplierData {

    /** */
    public static final String JSON_IPP_MAPPING = "ipp-mapping";

    /** */
    @JsonProperty(RawPrintInData.JSON_IPP_MAPPING)
    private Map<String, String> ippAttr;

    /**
     * @return IPP attributes.
     */
    public Map<String, String> getIppAttr() {
        return ippAttr;
    }

    /**
     * @param ippAttr
     *            IPP attributes.
     */
    public void setIppAttr(Map<String, String> ippAttr) {
        this.ippAttr = ippAttr;
    }

    /**
     * @param map
     *            Key/value map
     * @param key
     *            Key
     * @return Value, or {@code null} if not found.
     */
    @JsonIgnore
    private static String findAttrValue(final Map<String, String> map,
            final String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    /**
     * @param ippKeyword
     *            IPP keyword.
     * @return {@code null} if not found.
     */
    @JsonIgnore
    private String getAttrValue(final String ippKeyword) {
        return findAttrValue(this.ippAttr, ippKeyword);
    }

    /**
     * Creates an object from data string.
     *
     * @param data
     *            The serialized data.
     * @return The {@link RawPrintInData} object.
     */
    public static RawPrintInData createFromData(final String data) {
        return RawPrintInData.create(RawPrintInData.class, data);
    }

    @Override
    public String dataAsString() {
        try {
            return this.stringify();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }
}
