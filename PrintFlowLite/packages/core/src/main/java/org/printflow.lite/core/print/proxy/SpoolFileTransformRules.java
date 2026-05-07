/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.print.proxy;

import java.util.List;

import org.printflow.lite.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class SpoolFileTransformRules extends JsonAbstractBase {

    /** */
    public static class Replace {

        private String search;
        private String replacement;

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replace) {
            this.replacement = replace;
        }

        public String getSearch() {
            return search;
        }

        public void setSearch(String search) {
            this.search = search;
        }
    }

    /** */
    public static class PJL {
        @JsonProperty("header")
        private PJLHeader pjlHeader;

        public PJLHeader getPjlHeader() {
            return pjlHeader;
        }

        public void setPjlHeader(PJLHeader pjlHeader) {
            this.pjlHeader = pjlHeader;
        }
    }

    /** */
    public static class PJLHeader {

        private List<Replace> replace;

        public List<Replace> getReplace() {
            return replace;
        }

        public void setReplace(List<Replace> replace) {
            this.replace = replace;
        }
    }

    @JsonProperty("@PJL")
    private PJL pjl;

    public PJL getPjl() {
        return pjl;
    }

    public void setPjl(PJL pjl) {
        this.pjl = pjl;
    }

}
