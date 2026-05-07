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
package org.printflow.lite.core.ipp.client;

import java.io.InputStream;
import java.net.URI;

import org.printflow.lite.core.ipp.attribute.IppDictPrinterDescAttr;

/**
 * Attributes of a CUPS printer.
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsAttrPrinter {

    /**
     * See {@link IppDictPrinterDescAttr#ATTR_PRINTER_STATE} and
     * <a href="https://www.rfc-editor.org/rfc/rfc8011">RFC8011</a>.
     * <p>
     * In this context only the 'idle(3)' and 'stopped(5)' enumerations are
     * recognized, 'processing(4)' is NOT applicable.
     * <p>
     */
    public enum State {

        /**
         * Indicates that new Jobs can start processing without waiting.
         */
        IDLE("3"),
        /**
         * Indicates that no Jobs can be processed and intervention is required
         * (aka 'pauzed').
         */
        STOPPED("5");

        /** */
        private final String value;

        State(final String v) {
            this.value = v;
        }

        /**
         * @return IPP attribute value.
         */
        public String ippValue() {
            return this.value;
        }
    };

    /** */
    private URI deviceUri;

    /** */
    private String ppdName;

    /** */
    private Boolean isAcceptingJobs;

    /** */
    private String info;

    /** */
    private String location;

    /** */
    private URI moreInfo;

    /** */
    private State state;

    /** */
    private InputStream ppdFile;

    public URI getDeviceUri() {
        return deviceUri;
    }

    public void setDeviceUri(URI deviceUri) {
        this.deviceUri = deviceUri;
    }

    public String getPpdName() {
        return ppdName;
    }

    public void setPpdName(String ppdName) {
        this.ppdName = ppdName;
    }

    public Boolean getIsAcceptingJobs() {
        return isAcceptingJobs;
    }

    public void setIsAcceptingJobs(Boolean isAcceptingJobs) {
        this.isAcceptingJobs = isAcceptingJobs;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public URI getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(URI moreInfo) {
        this.moreInfo = moreInfo;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public InputStream getPpdFile() {
        return ppdFile;
    }

    public void setPpdFile(InputStream ppdFile) {
        this.ppdFile = ppdFile;
    }

}
