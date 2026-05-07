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

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ConnectionMsgListener
        implements ClientSessionChannel.MessageListener {

    private boolean wasConnected;
    private boolean connected;

    private final BayeuxClient bayeuxClient;
    private final CommonMsgListener genericListener;

    /**
     *
     * @param client
     *            The {@link BayeuxClient}.
     * @param genericListener
     *            The {@link CommonMsgListener}.
     */
    public ConnectionMsgListener(final BayeuxClient client,
            final CommonMsgListener genericListener) {

        super();
        this.bayeuxClient = client;
        this.genericListener = genericListener;
    }

    @Override
    public void onMessage(final ClientSessionChannel channel,
            final Message message) {

        if (bayeuxClient.isDisconnected()) {
            connected = false;
            genericListener.onConnectionClosed();
            return;
        }

        wasConnected = connected;

        connected = message.isSuccessful();

        if (!wasConnected && connected) {
            genericListener.onConnectionEstablished();
        } else if (wasConnected && !connected) {
            genericListener.onConnectionBroken();
        }

    }

}
