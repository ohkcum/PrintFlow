/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.printflow.lite.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CopyToClipBoardPanel extends Panel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * JavaScript function in. See {@code jquery.PrintFlowLite.js}.
     */
    private static final String JS_FUNCTION = "_copyHtmlToClipboard";

    /** */
    private static final String ONCLICK_FORMAT = JS_FUNCTION + "('#%s')";

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public CopyToClipBoardPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param idToCopy
     */
    public void populate(final String idToCopy) {

        final String title = HtmlButtonEnum.COPY.uiText(getLocale());

        final MarkupHelper helper = new MarkupHelper(this);

        final Label label = helper.addModifyLabelAttr("copy-anchor", "onclick",
                String.format(ONCLICK_FORMAT, idToCopy));

        MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_TITLE, title);
    }

    /**
     * Adds label to container with clipboard button.
     *
     * @param container
     * @param wid
     *            Wicket ID of text to copy.
     * @param panelId
     *            Wicket ID of panel with copy button.
     * @param text
     *            Label text
     */
    public static void addLabelAndClipBoardCopy(final MarkupContainer container,
            final String wid, final String panelId, final String text) {

        final Label labelToCopy = new Label(wid, text);
        container.add(labelToCopy);

        final String uuid = UUID.randomUUID().toString();
        MarkupHelper.modifyLabelAttr(labelToCopy, MarkupHelper.ATTR_ID, uuid);

        final CopyToClipBoardPanel panel = new CopyToClipBoardPanel(panelId);
        panel.populate(uuid);
        container.add(panel);
    }

}
