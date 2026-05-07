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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.ext.papercut.services.impl;

import java.math.BigDecimal;
import java.util.Locale;

import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.AccountingService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.account.UserAccountContext;
import org.printflow.lite.core.services.helpers.account.UserAccountContextEnum;
import org.printflow.lite.ext.papercut.PaperCutException;
import org.printflow.lite.ext.papercut.PaperCutServerProxy;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAccountContextPaperCut implements UserAccountContext {

    /** */
    private final PaperCutServerProxy server;

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     *
     * @param serverProxy
     *            The {@link PaperCutServerProxy}.
     */
    public UserAccountContextPaperCut(final PaperCutServerProxy serverProxy) {
        this.server = serverProxy;
    }

    @Override
    public UserAccountContextEnum asEnum() {
        return UserAccountContextEnum.PAPERCUT;
    }

    @Override
    public String getFormattedUserBalance(final User user, final Locale locale,
            final String currencySymbol) {

        final BigDecimal balance = this.getUserBalance(user);
        return ACCOUNTING_SERVICE.formatUserBalance(balance, locale,
                currencySymbol);
    }

    @Override
    public BigDecimal getUserBalance(final User user) {

        try {
            return server.getUserAccountBalance(user.getUserId(),
                    ConfigManager.getUserBalanceDecimals());
        } catch (PaperCutException e) {
            // no code intended
        }
        return BigDecimal.ZERO;
    }
}
