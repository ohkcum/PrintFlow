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
package org.printflow.lite.ext.papercut.job;

import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.enums.ExternalSupplierEnum;
import org.printflow.lite.core.jpa.Account.AccountTypeEnum;
import org.printflow.lite.core.jpa.DocLog;
import org.printflow.lite.ext.papercut.PaperCutAccountResolver;
import org.printflow.lite.ext.papercut.PaperCutDbProxy;
import org.printflow.lite.ext.papercut.PaperCutHelper;
import org.printflow.lite.ext.papercut.PaperCutPrintJobListener;
import org.printflow.lite.ext.papercut.PaperCutPrintMonitorPattern;
import org.printflow.lite.ext.papercut.PaperCutServerProxy;
import org.slf4j.Logger;

/**
 * Monitoring PaperCut print status of jobs issued from
 * {@link ExternalSupplierEnum#PrintFlowLite}.
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutPrintMonitor extends PaperCutPrintMonitorPattern {

    /**
     * .
     */
    private final Logger logger;

    /**
     *
     * @param papercutServerProxy
     *            The {@link PaperCutServerProxy}.
     * @param papercutDbProxy
     *            The {@link PaperCutDbProxy}.
     * @param statusListener
     *            The {@link PaperCutPrintJobListener}.
     * @param loggerListener
     *            The logger listening to log events.
     */
    protected PaperCutPrintMonitor(
            final PaperCutServerProxy papercutServerProxy,
            final PaperCutDbProxy papercutDbProxy,
            final PaperCutPrintJobListener statusListener,
            final Logger loggerListener) {

        super(ExternalSupplierEnum.PrintFlowLite, papercutServerProxy,
                papercutDbProxy, statusListener);
        this.logger = loggerListener;
    }

    /**
     * @return As in {@link PaperCutAccountResolver#getUserAccountName()}.
     */
    public static String getAccountNameUser() {
        return ConfigManager.instance().getConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL);
    }

    @Override
    public String getUserAccountName() {
        return getAccountNameUser();
    }

    /**
     * @return As in
     *         {@link PaperCutAccountResolver#getSharedParentAccountName()}.
     */
    public static String getSharedAccountNameParent() {
        return ConfigManager.instance().getConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_PARENT);
    }

    @Override
    public String getSharedParentAccountName() {
        return getSharedAccountNameParent();
    }

    /**
     * @return As in {@link PaperCutAccountResolver#getSharedJobsAccountName()}.
     */
    public static String getSharedAccountNameJobs() {
        return ConfigManager.instance().getConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_CHILD_JOBS);
    }

    @Override
    public String getSharedJobsAccountName() {
        return getSharedAccountNameJobs();
    }

    @Override
    protected int getAccountTrxWeightTotal(final DocLog docLogOut,
            final DocLog docLogIn) {
        return docLogOut.getDocOut().getPrintOut().getNumberOfCopies();
    }

    /**
     * As {@link PaperCutAccountResolver#getKlasFromAccountName(String)}.
     *
     * @param accountName
     *            The composed account name.
     * @return The extracted klas.
     */
    public static String extractKlasFromAccountName(final String accountName) {
        return PaperCutHelper.decomposeSharedAccountName(accountName);
    }

    @Override
    public String getKlasFromAccountName(final String accountName) {
        return extractKlasFromAccountName(accountName);
    }

    @Override
    protected Logger getLogger() {
        return this.logger;
    }

    @Override
    protected boolean isDocInAccountTrx() {
        return false;
    }

    /**
     * Static version of
     * {@link PaperCutAccountResolver#composeSharedSubAccountName(AccountTypeEnum, String, String)}
     * .
     *
     * @param accountType
     *            The PrintFlowLite account type.
     * @param accountName
     *            The PrintFlowLite account name.
     * @param accountNameParent
     *            The name of the PrintFlowLite parent account. Is {@code null} when
     *            account is not a child account, but a parent account itself.
     * @return The composed PaperCut name.
     */
    public static String createSharedSubAccountName(
            final AccountTypeEnum accountType, final String accountName,
            final String accountNameParent) {
        return PaperCutHelper.composeSharedAccountName(accountType, accountName,
                accountNameParent);
    }

    @Override
    public String composeSharedSubAccountName(final AccountTypeEnum accountType,
            final String accountName, final String accountNameParent) {
        return createSharedSubAccountName(accountType, accountName,
                accountNameParent);
    }

}
