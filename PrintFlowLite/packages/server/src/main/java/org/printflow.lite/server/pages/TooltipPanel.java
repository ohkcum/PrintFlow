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

import java.util.UUID;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TooltipPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public TooltipPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param htmlContent
     *            The HTMl tooltip content.
     * @param escapeMarkup
     *            {@code true} if model strings should be escaped.
     */
    public void populate(final String htmlContent, final boolean escapeMarkup) {
        this.populate(htmlContent, escapeMarkup, null);
    }

    /**
     *
     * @param htmlContent
     *            The HTMl tooltip content.
     * @param escapeMarkup
     *            {@code true} if model strings should be escaped.
     * @param prompt
     *            Text prompt or {@code null} for default button.
     */
    public void populate(final String htmlContent, final boolean escapeMarkup,
            final String prompt) {

        final String uuid = UUID.randomUUID().toString();
        final String title = this.getString("title");

        Label labelWrk;

        //
        if (prompt == null) {
            labelWrk = new Label("tooltip-anchor", title);
        } else {
            labelWrk = new Label("tooltip-anchor", prompt);
        }
        labelWrk.setEscapeModelStrings(escapeMarkup);

        if (prompt != null) {
            MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_CLASS,
                    "sp-tooltip-panel-txt");
        }

        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_HREF,
                String.format("#%s", uuid)));
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_TITLE, title));
        add(labelWrk);

        //
        labelWrk = new Label("tooltip-content", htmlContent);
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_ID, uuid));
        labelWrk.setEscapeModelStrings(escapeMarkup);

        add(labelWrk);
    }

}
