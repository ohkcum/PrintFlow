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
package org.printflow.lite.client.cometd;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserEventMsgListener
        implements ClientSessionChannel.MessageListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserEventMsgListener.class);

    /**
     * User Events.
     *
     * <p>
     * <b>WARNING</b>: <i>This enum is a duplicate of
     * org.printflow.lite.server.cometd.UserEventEnum</i>
     * </p>
     * <p>
     * <b>TODO</b>: Create/use enum of org.printflow.lite.common.
     * </p>
     */
    private enum UserEventEnum {
        /** */
        ACCOUNT,
        /** */
        JOBTICKET,
        /** */
        ERROR,
        /** */
        NULL,
        /** */
        PRINT_IN,
        /** */
        PRINT_IN_EXPIRED,
        /** */
        PRINT_MSG,
        /** */
        SERVER_SHUTDOWN,
        /** */
        SYS_MAINTENANCE
    }

    /**
     * TODO: use statics from printflowlite-common project.
     */
    private static final String KEY_EVENT = "event";
    private static final String KEY_DATA = "data";
    private static final String KEY_ERROR = "error";
    @SuppressWarnings("unused")
    private static final String KEY_JOBS = "jobs";
    @SuppressWarnings("unused")
    private static final String KEY_PAGES = "pages";
    private static final String KEY_MSG_TIME = "msgTime";

    /**
     *
     */
    private final CommonMsgListener commonMsgListener;

    /**
     * Get from server.
     */
    private Long prevMsgTime;

    /**
     *
     * @param msg
     *            The JSON object.
     */
    private void setPrevMsgTime(final JsonNode msg) {
        final JsonNode msgTime = msg.get(KEY_MSG_TIME);
        this.prevMsgTime = Long.valueOf(msgTime.asLong());
    }

    /**
     *
     * @param listener
     *            The {@link CommonMsgListener}.
     */
    public UserEventMsgListener(final CommonMsgListener listener) {
        super();
        this.commonMsgListener = listener;
    }

    /**
     * Composes the UI message.
     *
     * @param msg
     *            The {@link JsonNode}.
     * @return The UI message.
     */
    private static String composeUiMessage(final JsonNode msg) {

        final Iterator<JsonNode> iterMessages =
                msg.get(KEY_DATA).get("messages").elements();

        StringBuilder builder = new StringBuilder();

        while (iterMessages.hasNext()) {

            final JsonNode jsonNode = iterMessages.next();
            // final boolean isError = jsonNode.get("level").asInt() >
            // 0;
            final String msgText = jsonNode.get("text").asText();

            builder.append(msgText).append(" ");
        }
        return builder.toString();
    }

    @Override
    public void onMessage(final ClientSessionChannel channel,
            final Message message) {

        final String json = message.getData().toString();

        LOGGER.trace(json);

        final JsonNode msg;

        boolean isFatalError = false;
        String uiMsg;

        try {

            /*
             * IMPORTANT: do NOT use json.getBytes(), since this will give UTF-8
             * errors.
             */
            msg = new ObjectMapper().readTree(json);

            final UserEventEnum event =
                    UserEventEnum.valueOf(msg.get(KEY_EVENT).asText());

            final String logMsg;

            switch (event) {

            case ACCOUNT:
                // no break intended
            case JOBTICKET:
            case PRINT_IN_EXPIRED:

                setPrevMsgTime(msg);

                uiMsg = composeUiMessage(msg);
                logMsg = String.format("%s %s", event.toString(), uiMsg);

                if (event == UserEventEnum.ACCOUNT) {
                    this.commonMsgListener.onAccountMessage(uiMsg);
                } else if (event == UserEventEnum.PRINT_IN_EXPIRED) {
                    this.commonMsgListener.onPrintInExpired(uiMsg);
                } else {
                    this.commonMsgListener.onJobTicketMessage(uiMsg);
                }

                break;

            case ERROR:

                isFatalError = true;

                final String errorText = msg.get(KEY_ERROR).asText();
                logMsg = String.format("%s : %s", event.toString(), errorText);
                this.commonMsgListener.onError(errorText);

                break;

            case NULL:
                setPrevMsgTime(msg);
                logMsg = String.format("%s [%s]", event.toString(),
                        new Date(this.prevMsgTime.longValue()).toString());
                break;

            case PRINT_IN:

                setPrevMsgTime(msg);

                logMsg = event.toString();
                this.commonMsgListener.onPrintIn();
                break;

            case PRINT_MSG:

                setPrevMsgTime(msg);

                uiMsg = composeUiMessage(msg);
                logMsg = String.format("%s %s", event.toString(), uiMsg);

                this.commonMsgListener.onPrintOutMessage(uiMsg);

                break;

            case SERVER_SHUTDOWN:

                logMsg = String.format("%s %s", event.toString(),
                        "server is shutting down...");
                break;

            default:
                /*
                 * Set previous time to prevent endless notification.
                 */
                this.prevMsgTime = Long.valueOf(System.currentTimeMillis());
                logMsg = String.format("%s ", event.toString());
                break;
            }

            LOGGER.info(logMsg);

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        if (!isFatalError) {
            this.commonMsgListener.onPollInvitation(this.prevMsgTime);
        }
    }

}
