/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.common.dto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ClientAppConnectDto {

    public static enum Status {

        /**
         * Authenticated without errors.
         */
        OK,

        /**
         * An authentication error.
         */
        ERROR_AUTH,

        /**
         * A fatal error.
         */
        ERROR_FATAL
    }

    private Long serverTime;
    private String userAuthToken;
    private String webAppPath;
    private String webAppQuery;
    private String webAppQueryPrintIn;
    private String printInActionButton;
    private CometdConnectDto cometd;
    private Status status;
    private String statusMessage;

    public Long getServerTime() {
        return serverTime;
    }

    public void setServerTime(Long serverTime) {
        this.serverTime = serverTime;
    }

    public String getWebAppPath() {
        return webAppPath;
    }

    public void setWebAppPath(String webAppPath) {
        this.webAppPath = webAppPath;
    }

    public String getWebAppQuery() {
        return webAppQuery;
    }

    public void setWebAppQuery(String webAppQuery) {
        this.webAppQuery = webAppQuery;
    }

    public String getWebAppQueryPrintIn() {
        return webAppQueryPrintIn;
    }

    public void setWebAppQueryPrintIn(String webAppQueryPrintIn) {
        this.webAppQueryPrintIn = webAppQueryPrintIn;
    }

    public String getPrintInActionButton() {
        return printInActionButton;
    }

    public void setPrintInActionButton(String printInActionButton) {
        this.printInActionButton = printInActionButton;
    }

    public CometdConnectDto getCometd() {
        return cometd;
    }

    public void setCometd(CometdConnectDto cometd) {
        this.cometd = cometd;
    }

    public String getUserAuthToken() {
        return userAuthToken;
    }

    public void setUserAuthToken(String userAuthToken) {
        this.userAuthToken = userAuthToken;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
