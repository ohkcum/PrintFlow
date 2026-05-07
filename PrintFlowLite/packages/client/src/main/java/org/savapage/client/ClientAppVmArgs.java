/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
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
package org.printflow.lite.client;

import org.printflow.lite.common.SystemPropertyEnum;

/**
 * The VM arguments of the {@link ClientApp}.
 * <p>
 * These are set in the Linux/Mac/Cygwin and Windows start scripts as generated
 * by the Maven {@code appassembler-maven-plugin}.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ClientAppVmArgs {

    /**
     * {@code -Dapp.name=name}.
     */
    private final String appName;

    /**
     * {@code -Dapp.pid=pid}.
     */
    private final String appPid;

    /**
     * {@code -Dapp.repo=repo}.
     */
    private final String appRepo;

    /**
     * {@code -Dapp.home=home}.
     */
    private final String appHome;

    public ClientAppVmArgs() {
        this.appName = SystemPropertyEnum.MAVEN_APPASSEMBLER_NAME.getValue();
        this.appPid = SystemPropertyEnum.MAVEN_APPASSEMBLER_PID.getValue();
        this.appRepo = SystemPropertyEnum.MAVEN_APPASSEMBLER_REPO.getValue();
        this.appHome = SystemPropertyEnum.MAVEN_APPASSEMBLER_HOME.getValue();
    }

    public String getAppName() {
        return appName;
    }

    public String getAppPid() {
        return appPid;
    }

    public String getAppRepo() {
        return appRepo;
    }

    public String getAppHome() {
        return appHome;
    }

}
