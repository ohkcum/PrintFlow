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
package org.printflow.lite.server.api.request.export;

import java.io.File;

import org.printflow.lite.core.dao.enums.PrinterAttrEnum;
import org.printflow.lite.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqExportPrinterRawPPD extends AbstractReqExportPrinterFile {

    /**
     * @param id
     *            The Database key of printer.
     */
    public ReqExportPrinterRawPPD(final Long id) {
        super(id);
    }

    @Override
    protected PrinterAttrEnum getPrinterAttrEnum() {
        return PrinterAttrEnum.RAW_PRINT_PPD_FILE;
    }

    @Override
    protected File getExportFile(final String fileBaseName) {
        return ServiceContext.getServiceFactory().getProxyPrintService()
                .getRawPrintPPDFile(fileBaseName);
    }
}
