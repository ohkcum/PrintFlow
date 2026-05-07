/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
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
package org.printflow.lite.ext.rest;

import java.util.Date;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface RestClient {

    /**
     * POST a request.
     *
     * @param <T>
     *            Response type.
     * @param entityReq
     *            Request.
     * @param mediaTypeReq
     *            Media type of request.
     * @param mediaTypeRsp
     *            Media type of response. For example "application/json".
     * @param entityRspType
     *            Response type class.
     * @return Response type instance.
     */
    <T> T post(String entityReq, String mediaTypeReq, String mediaTypeRsp,
            Class<T> entityRspType);

    /**
     * Formats a {@link Date} to a ISO 8601 formatted date-time string
     * <i>with</i> time-zone.
     * <p>
     * <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>
     * </p>
     *
     * @param date
     *            The date to convert.
     * @return The formatted date string.
     */
    String toISODateTimeZ(Date date);
}
