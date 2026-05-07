/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server.pages;

import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserPinReset extends AbstractAuthPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final int MAX_PIN_LENGTH = 6;

    /**
     * A unique CSS class to trap keystrokes on PIN {@code <input>}. See
     * {@code <script>} in {@code html} file belonging to this class.
     */
    private static final String CSS_CLASS_NUMBERS_ONLY_UUID =
            "b8c6bb54-6163-11f0-b372-f37b360d4548";

    /**
     * @return {@code null} if no limit.
     */
    public static Integer getMaxPinLength() {
        final Integer maxLength = ConfigManager.instance()
                .getConfigInt(Key.USER_PIN_LENGTH_MAX, MAX_PIN_LENGTH);

        if (maxLength.intValue() == 0) {
            return null;
        }
        return maxLength;
    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public UserPinReset(final PageParameters parameters) {

        super(parameters);

        final Map<String, String> attrMap = new HashedMap<>();
        attrMap.put(MarkupHelper.ATTR_CLASS, CSS_CLASS_NUMBERS_ONLY_UUID);

        final Integer maxLength = getMaxPinLength();

        this.add(PasswordPanel.createPopulate("user-pin-reset-panel",
                "user-pin-reset", maxLength, attrMap));
        this.add(PasswordPanel.createPopulate("user-pin-reset-confirm-panel",
                "user-pin-reset-confirm", maxLength, attrMap));

        final MarkupHelper helper = new MarkupHelper(this);
        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-cancel", HtmlButtonEnum.CANCEL);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
