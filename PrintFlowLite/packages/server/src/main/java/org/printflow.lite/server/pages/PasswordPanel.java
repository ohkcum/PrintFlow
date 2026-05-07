/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server.pages;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.server.helpers.CssClassEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PasswordPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public PasswordPanel(final String id) {
        super(id);
    }

    /**
     * Creates and populates a panel instance.
     *
     * @param wid
     *            The non-null id of this component.
     * @param inputId
     *            {@link MarkupHelper#ATTR_ID}.
     * @param maxLength
     *            If {@code null} {@link MarkupHelper#ATTR_MAXLENGTH} is NOT
     *            set.
     * @return new instance.
     */
    public static PasswordPanel createPopulate(final String wid,
            final String inputId, final Integer maxLength) {

        final PasswordPanel passwordPanel = new PasswordPanel(wid);
        passwordPanel.populate(inputId, maxLength,
                new HashedMap<String, String>());

        return passwordPanel;
    }

    /**
     * Creates and populates a panel instance.
     *
     * @param wid
     *            The non-null id of this component.
     * @param inputId
     *            {@link MarkupHelper#ATTR_ID}.
     * @param maxLength
     *            If {@code null} {@link MarkupHelper#ATTR_MAXLENGTH} is NOT
     *            set.
     * @param htmlInputAttr
     *            {@code <input>} attribute key/values.
     * @return new instance.
     */
    public static PasswordPanel createPopulate(final String wid,
            final String inputId, final Integer maxLength,
            final Map<String, String> htmlInputAttr) {

        final PasswordPanel passwordPanel = new PasswordPanel(wid);
        passwordPanel.populate(inputId, maxLength, htmlInputAttr);

        return passwordPanel;
    }

    /**
     * @param inputId
     *            {@link MarkupHelper#ATTR_ID}.
     * @param maxLength
     *            If {@code null} {@link MarkupHelper#ATTR_MAXLENGTH} is NOT
     *            set.
     * @param htmlInputAttr
     *            {@code <input>} attribute key/values.
     */
    public void populate(final String inputId, final Integer maxLength,
            final Map<String, String> htmlInputAttr) {

        Label labelWrk;

        labelWrk = new Label("input-id");

        MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_ID, inputId);
        if (maxLength != null) {
            MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_MAXLENGTH,
                    maxLength.toString());
        }
        for (final Entry<String, String> entry : htmlInputAttr.entrySet()) {
            MarkupHelper.modifyLabelAttr(labelWrk, entry.getKey(),
                    entry.getValue());
        }

        this.add(labelWrk);

        //
        labelWrk = new Label("input-icon");

        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DATA_PrintFlowLite_KEY,
                inputId));

        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_TITLE,
                NounEnum.VISIBILITY.uiText(getLocale())));

        MarkupHelper.appendLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                CssClassEnum.PFL_BTN_SECRET_VISIBILITY.clazz());
        MarkupHelper.appendLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                CssClassEnum.JQM_UI_ICON_PFL_PASSWORD_EYE_OPEN.clazz());

        this.add(labelWrk);
    }

}
