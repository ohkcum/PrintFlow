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
package org.printflow.lite.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.dao.enums.ACLOidEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.ipp.IppSyntaxException;
import org.printflow.lite.core.ipp.client.IppConnectException;
import org.printflow.lite.core.services.ProxyPrintService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class Dashboard extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * @param parameters
     *            The page parameters.
     */
    public Dashboard(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final boolean hasEditorAccess =
                this.probePermissionToEdit(ACLOidEnum.A_DASHBOARD);

        final SystemStatusPanel systemStatusPanel =
                new SystemStatusPanel("system-status-panel");
        add(systemStatusPanel);

        systemStatusPanel.populate(hasEditorAccess);

        helper.addButton("button-clear", HtmlButtonEnum.CLEAR);

        helper.addLabel("realtime-activity",
                PhraseEnum.REALTIME_ACTIVITY.uiText(getLocale()));
    }

    @Override
    protected void onAfterRenderCompleted() {
        // Initialize the printer cache to get CUPS events in Dashboard.
        try {
            PROXYPRINT_SERVICE.lazyInitPrinterCache();
        } catch (IppConnectException | IppSyntaxException e) {
            AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                    PubLevelEnum.ERROR,
                    String.format("CUPS: %s", e.getMessage()));
        }

    }

}
