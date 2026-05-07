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
package org.printflow.lite.core.pdf;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.printflow.lite.core.i18n.PdfPropertiesEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.json.JsonAbstractBase;

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
@JsonPropertyOrder({ PdfInfoDto.JSON_PRODUCER, PdfInfoDto.JSON_CREATOR,
        PdfInfoDto.JSON_AUTHOR, PdfInfoDto.JSON_TITLE, PdfInfoDto.JSON_FORMAT,
        PdfInfoDto.JSON_FONTS })
public final class PdfInfoDto extends JsonAbstractBase {

    /** */
    public static final String JSON_PRODUCER = "producer";
    /** */
    public static final String JSON_CREATOR = "creator";
    /** */
    public static final String JSON_AUTHOR = "author";
    /** */
    public static final String JSON_TITLE = "title";
    /** */
    public static final String JSON_FORMAT = "format";
    /** */
    public static final String JSON_FONTS = "fonts";

    /**
     *
     * @author Rijk Ravestein
     *
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Fonts extends JsonAbstractBase {
        /** */
        public static final String JSON_COUNT = "count";
        /** */
        public static final String JSON_STDEMBED = "stdembed";

        @JsonProperty(JSON_COUNT)
        private int count;

        @JsonProperty(JSON_STDEMBED)
        private Boolean standardEmbed;

        public int getCount() {
            return count;
        }

        public void setCount(int total) {
            this.count = total;
        }

        public Boolean getStandardEmbed() {
            return standardEmbed;
        }

        public void setStandardEmbed(Boolean standardEmbed) {
            this.standardEmbed = standardEmbed;
        }

    }

    @JsonProperty(JSON_PRODUCER)
    private String producer;

    @JsonProperty(JSON_CREATOR)
    private String creator;

    @JsonProperty(JSON_AUTHOR)
    private String author;

    @JsonProperty(JSON_TITLE)
    private String title;

    @JsonProperty(JSON_FORMAT)
    private String format;

    @JsonProperty(JSON_FONTS)
    private Fonts fonts;

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Fonts getFonts() {
        return fonts;
    }

    public void setFonts(Fonts fonts) {
        this.fonts = fonts;
    }

    /**
     * Creates a map with PDF property (key) and value.
     *
     * @param locale
     *            {@link Locale}.
     * @return the map.
     */
    public Map<String, String> createPropertyMap(final Locale locale) {
        final Map<String, String> mapProps = new LinkedHashMap<>();

        appendProp(mapProps, PdfPropertiesEnum.PRODUCER, this.getProducer(),
                locale);
        appendProp(mapProps, PdfPropertiesEnum.CREATOR, this.getCreator(),
                locale);
        appendProp(mapProps, PdfPropertiesEnum.AUTHOR, this.getAuthor(),
                locale);
        appendProp(mapProps, PdfPropertiesEnum.TITLE, this.getTitle(), locale);
        appendProp(mapProps, PdfPropertiesEnum.FORMAT, this.getFormat(),
                locale);

        final PdfInfoDto.Fonts fontsDto = this.getFonts();

        if (fontsDto != null) {
            final String value;
            if (fontsDto.getCount() == 0) {
                value = "";
            } else if (fontsDto.getStandardEmbed() != null) {
                if (BooleanUtils.isTrue(fontsDto.getStandardEmbed())) {
                    value = PhraseEnum.PDF_FONTS_STANDARD_OR_EMBEDDED_SHORT
                            .uiText(locale);
                } else {
                    value = PhraseEnum.PDF_FONTS_SOME_NON_STANDARD_OR_EMBEDDED_SHORT
                            .uiText(locale);
                }
            } else {
                value = null;
            }
            if (value != null) {
                appendProp(mapProps, PdfPropertiesEnum.FONTS, value, locale);
            }
        }

        return mapProps;
    }

    /**
     * Appends a PDF property.
     *
     * @param mapProps
     *            Map to append to.
     * @param prop
     *            property
     * @param locale
     * @param value
     */
    private static void appendProp(final Map<String, String> mapProps,
            final PdfPropertiesEnum prop, final String value,
            final Locale locale) {
        mapProps.put(prop.uiText(locale), Objects.toString(value, ""));
    }

}
