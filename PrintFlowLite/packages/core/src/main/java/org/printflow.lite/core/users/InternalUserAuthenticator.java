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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.core.users;

import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.dao.enums.UserAttrEnum;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.UserService;

/**
 * Authenticator for internal users only.
 *
 * @author Rijk Ravestein
 *
 */
public final class InternalUserAuthenticator implements IUtility {

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * Utility class.
     */
    private InternalUserAuthenticator() {
    }

    /**
     * @param user
     * @param password
     * @return {@code true} if password is valid.
     */
    public static boolean authenticate(final User user, final String password) {

        final String checkSum = USER_SERVICE.findUserAttrValue(user.getId(),
                UserAttrEnum.INTERNAL_PASSWORD);

        return ConfigManager.instance().isUserPasswordValid(checkSum,
                user.getUserId(), password);
    }

}
