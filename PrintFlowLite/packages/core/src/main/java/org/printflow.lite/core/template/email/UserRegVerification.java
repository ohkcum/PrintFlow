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
package org.printflow.lite.core.template.email;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.printflow.lite.core.template.TemplateAttrEnum;
import org.printflow.lite.core.template.dto.TemplateDto;
import org.printflow.lite.core.template.dto.TemplateUserRegistrationDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserRegVerification extends EmailTemplateMixin {

    /** */
    private final TemplateUserRegistrationDto registration;

    /**
     *
     * @param customHome
     *            The directory with the custom Email template files.
     * @param userReg
     *            The ticket.
     */
    public UserRegVerification(final File customHome,
            final TemplateUserRegistrationDto userReg) {
        super(customHome);
        this.registration = userReg;
    }

    @Override
    protected Map<String, TemplateDto> onRender(final Locale locale) {
        final Map<String, TemplateDto> map = new HashMap<>();
        map.put(TemplateAttrEnum.USER_REGISTRATION.asAttr(), this.registration);
        return map;
    }

}
