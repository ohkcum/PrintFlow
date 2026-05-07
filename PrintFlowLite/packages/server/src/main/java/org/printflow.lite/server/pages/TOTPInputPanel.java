/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TOTPInputPanel extends Panel {

    /** */
    public enum ToolTip {
        /** */
        AUTH_APP("tooltip-totp"),
        /** */
        AUTH_APP_TELEGRAM("tooltip-totp-telegram");

        /** */
        private final String msgId;

        ToolTip(final String id) {
            this.msgId = id;
        }

        /**
         * @return message id
         */
        public String getMsgId() {
            return msgId;
        }
    }

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String ATTR_NAME_FOR_BITWARDEN_TOTP = "totp";

    /** */
    private static final String WID_TOTP_CODE_INPUT = "totp-code";

    /** */
    private static final String WID_TOOLTIP = "tooltip-totp";

    /**
     *
     * @param wid
     *            The non-null id of this component.
     * @param htmlInputId
     *            The id of the TOTP input field.
     * @param toolTip
     *            tooltip.
     */
    public TOTPInputPanel(final String wid, final String htmlInputId,
            final ToolTip toolTip) {

        super(wid);

        final MarkupHelper helper = new MarkupHelper(this);

        final Label totp = helper.addModifyLabelAttr(WID_TOTP_CODE_INPUT,
                MarkupHelper.ATTR_ID, htmlInputId);
        /*
         * Be compatible with Bitwarden ...
         */
        MarkupHelper.modifyLabelAttr(totp, MarkupHelper.ATTR_NAME,
                ATTR_NAME_FOR_BITWARDEN_TOTP);
        /*
         * ... and with other password managers that generate TOTP codes.
         */
        MarkupHelper.modifyLabelAttr(totp, MarkupHelper.ATTR_AUTOCOMPLETE,
                MarkupHelper.ATTR_AUTOCOMPLETE_ONE_TIME_CODE);

        //
        final TooltipPanel tooltip = new TooltipPanel(WID_TOOLTIP);
        tooltip.populate(helper.localized(toolTip.getMsgId()), true);
        this.add(tooltip);
    }

}
