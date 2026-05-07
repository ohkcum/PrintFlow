/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2020 Datraverse B.V. <info@datraverse.com>
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
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.util.FileNameExtEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PPDFileOptionsAddin extends AbstractAdminPage
        implements IFileOptionsAddin {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public PPDFileOptionsAddin(final PageParameters parameters) {

        super(parameters);

        final FileOptionsPanel panel = new FileOptionsPanel("panel",
                ConfigManager.getServerCustomRawPrintPPDHome(),
                FileNameExtEnum.PPD);
        add(panel);
        panel.populate();
    }

}
