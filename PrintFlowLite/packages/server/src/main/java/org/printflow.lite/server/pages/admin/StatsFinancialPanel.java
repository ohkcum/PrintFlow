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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.dto.FinancialDisplayInfoDto;
import org.printflow.lite.core.services.AccountingService;
import org.printflow.lite.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class StatsFinancialPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The panel id.
     */
    public StatsFinancialPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    public void populate() {

        final AccountingService svc =
                ServiceContext.getServiceFactory().getAccountingService();

        final FinancialDisplayInfoDto dto =
                svc.getFinancialDisplayInfo(getLocale(), null);

        final String appCurrencyCode = ConfigManager.getAppCurrencyCode();

        add(new Label("currency-code", appCurrencyCode));

        add(new Label("accounts-deb-cnt", dto.getUserDebit().getCount()));
        add(new Label("accounts-deb-min", dto.getUserDebit().getMin()));
        add(new Label("accounts-deb-max", dto.getUserDebit().getMax()));
        add(new Label("accounts-deb-sum", dto.getUserDebit().getSum()));
        add(new Label("accounts-deb-avg", dto.getUserDebit().getAvg()));

        add(new Label("accounts-crd-cnt", dto.getUserCredit().getCount()));
        add(new Label("accounts-crd-min", dto.getUserCredit().getMin()));
        add(new Label("accounts-crd-max", dto.getUserCredit().getMax()));
        add(new Label("accounts-crd-sum", dto.getUserCredit().getSum()));
        add(new Label("accounts-crd-avg", dto.getUserCredit().getAvg()));
    }
}
