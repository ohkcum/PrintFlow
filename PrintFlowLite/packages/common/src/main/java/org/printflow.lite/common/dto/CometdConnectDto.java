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
public class CometdConnectDto {

    /**
     * Name of a CometD ServerMessage EXT_FIELD attribute.
     */
    public static final String SERVER_MSG_EXT_FIELD = "ext";

    /**
     * Name of a CometD ServerMessage EXT_FIELD attribute: value object
     * {@code Map<String, Object>}.
     * 
     * @see <a href="https://secure.datraverse.nl/issues/view.php?id=536">Mantis
     *      #536</a>
     * @since 0.9.8
     */
    public static final String SERVER_MSG_ATTR_AUTH = "org.printflow.lite.authn";

    /**
     * Name of a CometD ServerMessage EXT_FIELD attribute: value object
     * {@code String}.
     */
    public static final String SERVER_MSG_ATTR_SHARED_TOKEN = "token";

    /**
     * Name of a CometD ServerMessage EXT_FIELD attribute: value object
     * {@code String}.
     */
    public static final String SERVER_MSG_ATTR_USER_TOKEN = "userToken";

    private String authToken;
    private String urlPath;
    private String channelSubscribe;
    private String channelPublish;
    private Long maxNetworkDelay;

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public String getChannelSubscribe() {
        return channelSubscribe;
    }

    public void setChannelSubscribe(String channelSubscribe) {
        this.channelSubscribe = channelSubscribe;
    }

    public String getChannelPublish() {
        return channelPublish;
    }

    public void setChannelPublish(String channelPublish) {
        this.channelPublish = channelPublish;
    }

    public Long getMaxNetworkDelay() {
        return maxNetworkDelay;
    }

    public void setMaxNetworkDelay(Long maxNetworkDelay) {
        this.maxNetworkDelay = maxNetworkDelay;
    }

}
