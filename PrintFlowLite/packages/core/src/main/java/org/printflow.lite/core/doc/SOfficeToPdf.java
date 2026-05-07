/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.doc;

import java.io.File;

import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.doc.soffice.SOfficeBusyException;
import org.printflow.lite.core.doc.soffice.SOfficeCommonConvertTask;
import org.printflow.lite.core.doc.soffice.SOfficeDocFormat;
import org.printflow.lite.core.doc.soffice.SOfficeDocFormatRegistryDefault;
import org.printflow.lite.core.doc.soffice.SOfficeTaskTimeoutException;
import org.printflow.lite.core.services.SOfficeService;
import org.printflow.lite.core.services.ServiceContext;

/**
 * A {@link ExecMode#MULTI_THREADED} implementation of Libre Office PDF
 * conversion.
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeToPdf extends AbstractDocFileConverter {

    /**
     * Constructor.
     */
    private static final SOfficeService SOFFICE_SERVICE =
            ServiceContext.getServiceFactory().getSOfficeService();

    /**
     *
     */
    private static final SOfficeDocFormat PDF_OFFICE_FORMAT =
            new SOfficeDocFormatRegistryDefault()
                    .getFormatByExtension(DocContent.FILENAME_EXT_PDF);

    @Override
    protected ExecType getExecType() {
        return ExecType.ADVANCED;
    }

    /**
     * Constructor.
     */
    public SOfficeToPdf() {
        super(ExecMode.MULTI_THREADED);
    }

    @Override
    protected File getOutputFile(final File fileIn) {
        return getFileSibling(fileIn, DocContentTypeEnum.PDF);
    }

    @Override
    protected String getOsCommand(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut) {
        return null;
    }

    @Override
    protected void convertCustom(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut)
            throws DocContentToPdfException, UnavailableException {

        final SOfficeCommonConvertTask task = new SOfficeCommonConvertTask(
                fileIn, fileOut, PDF_OFFICE_FORMAT);

        try {
            SOFFICE_SERVICE.execute(task);
        } catch (SOfficeBusyException e) {
            throw new UnavailableException(UnavailableException.State.TEMPORARY,
                    e);
        } catch (SOfficeTaskTimeoutException e) {
            throw new DocContentToPdfException(
                    "PDF conversion did not complete within time.", e);

        }
    }

    /**
     *
     * @return {@code true} if this converter is available.
     */
    public static boolean isAvailable() {
        return SOFFICE_SERVICE.isRunning();
    }

    @Override
    public boolean notifyStdOutMsg() {
        return false;
    }
}
