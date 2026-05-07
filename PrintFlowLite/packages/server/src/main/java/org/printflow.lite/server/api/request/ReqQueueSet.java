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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.IppQueueDao;
import org.printflow.lite.core.dao.PrinterDao;
import org.printflow.lite.core.dao.enums.IppQueueAttrEnum;
import org.printflow.lite.core.dao.enums.IppRoutingEnum;
import org.printflow.lite.core.doc.store.DocStoreBranchEnum;
import org.printflow.lite.core.doc.store.DocStoreTypeEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.ipp.attribute.IppAuthMethodEnum;
import org.printflow.lite.core.jpa.IppQueue;
import org.printflow.lite.core.jpa.IppQueueAttr;
import org.printflow.lite.core.jpa.Printer;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.core.util.JsonHelper;

/**
 * Edits or creates a Queue.
 * <p>
 * Also, a logical delete can be applied or reversed.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqQueueSet extends ApiRequestMixin {

    /**
     * .
     */
    private static final PrinterDao PRINTER_DAO =
            ServiceContext.getDaoContext().getPrinterDao();

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final ReqQueueGet.DtoRsp dtoReq = ReqQueueGet.DtoRsp
                .create(ReqQueueGet.DtoRsp.class, this.getParmValueDto());

        final IppQueueDao ippQueueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final boolean isNew = dtoReq.getId() == null;
        final Date now = new Date();
        final String urlPath = dtoReq.getUrlpath().trim();

        // INVARIANT: Mantis #1105
        if (StringUtils.isBlank(urlPath)) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-queue-empty-path");
            return;
        }

        // INVARIANT
        if (StringUtils.isNotBlank(dtoReq.getIpallowed())
                && !InetUtils.isCidrSetValid(dtoReq.getIpallowed())) {
            setApiResult(ApiResultCodeEnum.WARN, "msg-value-invalid", "CIDR",
                    dtoReq.getIpallowed());
            return;
        }

        /*
         * Note: returns null when logically deleted!!
         */
        final IppQueue jpaQueueDuplicate = ippQueueDao.findByUrlPath(urlPath);

        IppQueue jpaQueue = null;

        boolean isDuplicate = true;

        if (isNew) {

            if (jpaQueueDuplicate == null) {

                jpaQueue = new IppQueue();
                jpaQueue.setCreatedBy(requestingUser);
                jpaQueue.setCreatedDate(now);

                isDuplicate = false;
            }

        } else {

            jpaQueue = ippQueueDao.findById(dtoReq.getId());

            if (jpaQueueDuplicate == null
                    || jpaQueueDuplicate.getId().equals(jpaQueue.getId())) {

                jpaQueue.setModifiedBy(requestingUser);
                jpaQueue.setModifiedDate(now);

                isDuplicate = false;
            }
        }

        // INVARIANT
        if (isDuplicate) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-queue-duplicate-path",
                    urlPath);
            return;
        }

        /*
         * (1) Queue
         */
        jpaQueue.setUrlPath(urlPath);
        jpaQueue.setIpAllowed(dtoReq.getIpallowed());
        jpaQueue.setTrusted(dtoReq.getTrusted());
        jpaQueue.setDisabled(dtoReq.getDisabled());

        final String keyOK;

        if (isNew) {

            // INVARIANT
            if (QUEUE_SERVICE.isReservedQueue(urlPath)) {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-queue-reserved-path",
                        urlPath);
                return;
            }
            // Create attribute list, so attributes can be set or deleted.
            jpaQueue.setAttributes(new ArrayList<IppQueueAttr>());

            ippQueueDao.create(jpaQueue);

            keyOK = "msg-queue-created-ok";

        } else {

            final boolean isDeleted = dtoReq.getDeleted();

            if (jpaQueue.getDeleted() != isDeleted) {

                if (isDeleted) {
                    QUEUE_SERVICE.setLogicalDeleted(jpaQueue, now,
                            requestingUser);
                } else {
                    QUEUE_SERVICE.undoLogicalDeleted(jpaQueue);
                }
            }

            ippQueueDao.update(jpaQueue);

            keyOK = "msg-queue-saved-ok";
        }

        /*
         * (2) Set/Delete Queue Attributes
         */
        if (DOCSTORE_SERVICE.isEnabled(DocStoreTypeEnum.JOURNAL,
                DocStoreBranchEnum.IN_PRINT)) {
            QUEUE_SERVICE.setQueueAttrValue(jpaQueue,
                    IppQueueAttrEnum.JOURNAL_DISABLE,
                    dtoReq.getJournalDisabled());
        }

        if (ConfigManager.isIppAuthOptionEnabled()
                && BooleanUtils.isTrue(dtoReq.getBasicAuth())) {
            QUEUE_SERVICE.setQueueAttrValue(jpaQueue,
                    IppQueueAttrEnum.IPP_AUTH_METHOD,
                    IppAuthMethodEnum.BASIC.toString());
        } else {
            if (!isNew) {
                QUEUE_SERVICE.deleteQueueAttrValue(jpaQueue,
                        IppQueueAttrEnum.IPP_AUTH_METHOD);
            }
        }

        if (ConfigManager.instance().isConfigValue(Key.IPP_ROUTING_ENABLE)) {

            if (dtoReq.getIppRouting() == null
                    || dtoReq.getIppRouting() == IppRoutingEnum.NONE) {

                if (!isNew) {
                    QUEUE_SERVICE.deleteQueueAttrValue(jpaQueue,
                            IppQueueAttrEnum.IPP_ROUTING);
                    QUEUE_SERVICE.deleteQueueAttrValue(jpaQueue,
                            IppQueueAttrEnum.IPP_ROUTING_OPTIONS);
                }

            } else {

                if (StringUtils.isNotBlank(dtoReq.getIppOptions())
                        && JsonHelper.createStringMapOrNull(
                                dtoReq.getIppOptions()) == null) {
                    setApiResultText(ApiResultCodeEnum.ERROR,
                            "Invalid option map (must be JSON format).");
                    return;
                }

                if (dtoReq.getIppRouting() == IppRoutingEnum.PRINTER) {

                    final String printerName = StringUtils
                            .defaultString(dtoReq.getIppRoutingPrinterName())
                            .toUpperCase();

                    final Printer printer = PRINTER_DAO.findByName(printerName);

                    if (printer == null) {
                        setApiResultText(ApiResultCodeEnum.ERROR, String.format(
                                "%s \"%s\" : %s",
                                NounEnum.PRINTER.uiText(getLocale()),
                                printerName,
                                PhraseEnum.NOT_FOUND.uiText(getLocale())));
                        return;
                    }
                    QUEUE_SERVICE.setQueueAttrValue(jpaQueue,
                            IppQueueAttrEnum.IPP_ROUTING_PRINTER_NAME,
                            printerName);
                } else {
                    QUEUE_SERVICE.deleteQueueAttrValue(jpaQueue,
                            IppQueueAttrEnum.IPP_ROUTING_PRINTER_NAME);
                }

                QUEUE_SERVICE.setQueueAttrValue(jpaQueue,
                        IppQueueAttrEnum.IPP_ROUTING,
                        dtoReq.getIppRouting().toString());

                QUEUE_SERVICE.setQueueAttrValue(jpaQueue,
                        IppQueueAttrEnum.IPP_ROUTING_OPTIONS,
                        StringUtils.defaultString(dtoReq.getIppOptions()));
            }
        }

        setApiResult(ApiResultCodeEnum.OK, keyOK);
    }

}
