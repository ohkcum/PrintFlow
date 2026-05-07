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
package org.printflow.lite.core.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Abstract class implementing
 * <a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-11">JSON
 * Hypertext Application Language</a> (HAL). Also see <a href=
 * "https://en.wikipedia.org/wiki/Hypertext_Application_Language">this</a>
 * wikipedia entry.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class JsonHypertextAppLang extends JsonAbstractBase {

    /** */
    public static final String MIME_TYPE = "application/hal+json";

    /**
     * Embedded resources.
     */
    public static final String PROP_EMBEDDED = "_embedded";

    /**
     * Links to other resources.
     */
    public static final String PROP_LINKS = "_links";

    /**
     * Links to self.
     */
    public static final String PROP_LINK_SELF = "self";

    /**
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link extends JsonAbstractBase {

        /** Required. */
        private String href;

        /** Optional. */
        private String type;

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

    };

    /**
     */
    public static class Links extends JsonAbstractBase {

        @JsonProperty(JsonHypertextAppLang.PROP_LINK_SELF)
        private Link self;

        public Link getSelf() {
            return self;
        }

        public void setSelf(Link self) {
            this.self = self;
        }

    }

}
