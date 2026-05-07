/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.dao;

import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.dao.enums.DeviceAttrEnum;
import org.printflow.lite.core.jpa.Device;
import org.printflow.lite.core.jpa.DeviceAttr;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface DeviceAttrDao extends GenericDao<DeviceAttr> {

    /**
     *
     */
    String WEBAPP_USER_PREFIX = "webapp.user";

    /**
     *
     */
    String AUTH_MODE_PREFIX = "auth-mode";

    /**
     * Key Prefix for Card Number.
     */
    String CARD_NUMBER_PREFIX = "card.number";

    /**
    *
    */
    String VALUE_CARD_NUMBER_HEX = IConfigProp.CARD_NUMBER_FORMAT_V_HEX;

    /**
    *
    */
    String VALUE_CARD_NUMBER_DEC = IConfigProp.CARD_NUMBER_FORMAT_V_DEC;

    /**
    *
    */
    String VALUE_CARD_NUMBER_LSB = IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB;

    /**
    *
    */
    String VALUE_CARD_NUMBER_MSB = IConfigProp.CARD_NUMBER_FIRSTBYTE_V_MSB;

    /**
     * Finds a {@link DeviceAttr} for a {@link Device}.
     *
     * @param deviceId
     *            The primary key of the {@link Device}.
     * @param name
     *            The {@link DeviceAttrEnum}.
     * @return The {@link DeviceAttr} or {@code null} when not found.
     */
    DeviceAttr findByName(Long deviceId, DeviceAttrEnum name);

}
