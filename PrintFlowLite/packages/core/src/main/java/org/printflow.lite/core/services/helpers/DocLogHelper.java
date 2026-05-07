/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.core.services.helpers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.dao.enums.DaoEnumHelper;
import org.printflow.lite.core.dao.enums.ExternalSupplierEnum;
import org.printflow.lite.core.i18n.PrintOutNounEnum;
import org.printflow.lite.core.ipp.attribute.IppDictJobTemplateAttr;
import org.printflow.lite.core.ipp.helpers.IppOptionMap;
import org.printflow.lite.core.ipp.helpers.IppPrintInData;
import org.printflow.lite.core.jpa.DocLog;
import org.printflow.lite.core.print.server.PostScriptFilter;
import org.printflow.lite.core.services.ProxyPrintService;
import org.printflow.lite.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogHelper implements IUtility {

    /**
     * Utility class.
     */
    private DocLogHelper() {
    }

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * Updates {@link ExternalSupplierInfo} from
     * {@link PostScriptFilter.Result}.
     *
     * @param supplierInfo
     * @param result
     */
    public static void updateExternalSupplierInfo(
            final ExternalSupplierInfo supplierInfo,
            final PostScriptFilter.Result result) {

        if (supplierInfo == null || supplierInfo.getData() == null) {
            return;
        }

        switch (supplierInfo.getSupplier()) {

        case IPP_CLIENT:

            if (supplierInfo.getData() instanceof IppPrintInData) {

                final IppPrintInData data =
                        (IppPrintInData) supplierInfo.getData();

                if (data.getAttrCreateJob() == null) {
                    final Map<String, String> attr = new HashMap<>();
                    data.setAttrCreateJob(attr);
                }
                data.getAttrCreateJob()
                        .putAll(result.getIppOptionMap().getOptionValues());
            }
            break;

        case RAW_IP_PRINT:
        case WEB_SERVICE:
            if (supplierInfo.getData() instanceof RawPrintInData) {
                final RawPrintInData data =
                        (RawPrintInData) supplierInfo.getData();
                data.getIppAttr()
                        .putAll(result.getIppOptionMap().getOptionValues());
            }
            break;

        default:
            break;
        }
    }

    /**
     * Gets the IPP option/value map (if present) supplied by
     * {@link ExternalSupplierEnum} for a set of PrintIn IPP options.
     *
     * @param docLog
     *            Document log.
     * @param options
     *            IPP options to select.
     * @return {@code null} if not present.
     */
    public static Map<String, String> getExtSuppliedIppOptionMap(
            final DocLog docLog, final Set<String> options) {

        final IppOptionMap ippMap = getExtSuppliedIppOptionMap(docLog);

        if (ippMap != null) {

            final Map<String, String> map = new HashMap<>();

            for (final String key : options) {
                final String value = ippMap.getOptionValue(key);
                if (value != null) {
                    map.put(key, value);
                }
            }
            return map;
        }
        return null;
    }

    /**
     * Gets the IPP option map (if present) supplied by
     * {@link ExternalSupplierEnum}.
     *
     * @param docLog
     *            Document log.
     * @return {@code null} if not present.
     */
    private static IppOptionMap
            getExtSuppliedIppOptionMap(final DocLog docLog) {

        if (docLog.getExternalData() != null) {

            final ExternalSupplierEnum supl =
                    DaoEnumHelper.getExtSupplier(docLog);

            if (supl != null) {
                final Map<String, String> options;
                switch (supl) {
                case IPP_CLIENT:
                    options = IppPrintInData
                            .createFromData(docLog.getExternalData())
                            .getAttrCreateJob();
                    break;
                case RAW_IP_PRINT:
                case WEB_SERVICE:
                    options = RawPrintInData
                            .createFromData(docLog.getExternalData())
                            .getIppAttr();
                    break;
                default:
                    options = null;
                    break;
                }
                if (options != null) {
                    return new IppOptionMap(options);
                }
            }
        }
        return null;
    }

    /**
     * Creates a localized UI string of IPP values for a subset of IPP options.
     *
     * @param ippOptionSubset
     *            IPP options for UI.
     * @param ippOptionMap
     *            IPP Option/values.
     * @param locale
     *            The {@link Locale}.
     * @return {@code null} if no options found.
     */
    public static String createIppOptionsUi(final Set<String> ippOptionSubset,
            final Map<String, String> ippOptionMap, final Locale locale) {

        final StringBuilder txt = new StringBuilder();

        for (final String option : ippOptionSubset) {

            final String value = ippOptionMap.get(option);

            if (value != null) {

                String part = null;

                if (option.equals(IppDictJobTemplateAttr.ATTR_COPIES)) {
                    if (!value.equals("1")) {
                        part = String.format("%s %s", value,
                                PrintOutNounEnum.COPY.uiText(locale, true));
                    }
                } else {
                    part = PROXYPRINT_SERVICE.localizePrinterOptValue(locale,
                            option, value);
                }

                if (part != null) {
                    if (txt.length() > 0) {
                        txt.append(" • ");
                    }
                    txt.append(part);
                }
            }
        }
        if (txt.length() > 0) {
            return txt.toString();
        }
        return null;
    }

}
