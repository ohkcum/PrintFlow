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
package org.printflow.lite.core.ipp.client;

import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.ipp.IppPrinterEventEnum;
import org.printflow.lite.core.ipp.IppPrinterStateEnum;
import org.printflow.lite.core.ipp.IppServerEventEnum;
import org.printflow.lite.core.ipp.IppSyntaxException;
import org.printflow.lite.core.services.ProxyPrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppNotificationRecipient {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppNotificationRecipient.class);

    /** */
    private static final PubTopicEnum ADMIN_PUB_TOPIC = PubTopicEnum.CUPS;

    /**
     * .
     */
    @SuppressWarnings("unused")
    private final ProxyPrintService proxyPrintService;

    /**
     * @param svc
     *            {@link ProxyPrintService}.
     */
    public IppNotificationRecipient(final ProxyPrintService svc) {
        this.proxyPrintService = svc;
    }

    /**
     *
     * @param printerName
     * @param printerEvent
     * @param printerState
     */
    public void onPrinterEvent(final String printerName,
            final IppPrinterEventEnum printerEvent,
            final IppPrinterStateEnum printerState) {

        final String msg = String.format("%s | %s | %s", printerName,
                printerEvent.asEvent(), printerState.asLogText());

        final PubLevelEnum level;
        switch (printerState) {
        case IDLE:
            level = PubLevelEnum.CLEAR;
            break;
        case PROCESSING:
            level = PubLevelEnum.INFO;
            break;
        case STOPPED:
            level = PubLevelEnum.WARN;
            break;
        case UNKNOWN:
        default:
            level = PubLevelEnum.ERROR;
            break;
        }

        AdminPublisher.instance().publish(ADMIN_PUB_TOPIC, level, msg);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg);
        }
    }

    /**
     * @param serverEvent
     */
    public void onServerEvent(final IppServerEventEnum serverEvent) {

        final PubLevelEnum level;

        final String msg = String.format("%s | %s", ADMIN_PUB_TOPIC.name(),
                serverEvent.asEvent());

        switch (serverEvent) {
        case AUDIT:
            level = PubLevelEnum.INFO;
            break;
        case RESTARTED:
        case STARTED:
            level = PubLevelEnum.CLEAR;
            break;
        case STOPPED:
            level = PubLevelEnum.WARN;
            break;
        default:
            level = PubLevelEnum.ERROR;
            break;
        }

        final AdminPublisher admPub = AdminPublisher.instance();

        admPub.publish(ADMIN_PUB_TOPIC, level, msg);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg);
        }

        try {
            this.proxyPrintService.initPrinterCache();
            admPub.publishPopup(ADMIN_PUB_TOPIC, level, msg);

        } catch (IppConnectException | IppSyntaxException e) {

            final String warn = e.getMessage();

            admPub.publish(ADMIN_PUB_TOPIC, PubLevelEnum.WARN, warn);
            admPub.publishPopup(ADMIN_PUB_TOPIC, PubLevelEnum.WARN, warn);
            LOGGER.warn(warn);
        }

    }

}
