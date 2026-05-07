/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.ext.papercut;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.rfid.RfidNumberFormat;
import org.printflow.lite.core.users.AbstractUserSource;
import org.printflow.lite.core.users.CommonUser;
import org.printflow.lite.core.users.CommonUserGroup;
import org.printflow.lite.core.users.IUserSource;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutUserSource extends AbstractUserSource
        implements IUserSource {

    /**
     * Creates a {@link PaperCutServerProxy} instance with the actual
     * {@link ConfigManager} settings.
     *
     * @return server instance
     */
    private static PaperCutServerProxy createPaperCutServerProxy() {
        return PaperCutServerProxy.create(ConfigManager.instance(), false);
    }

    @Override
    public SortedSet<CommonUserGroup> getGroups() {
        try {
            return createPaperCutServerProxy().listUserGroups();
        } catch (PaperCutException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    @Override
    public CommonUserGroup getGroup(final String groupName) {
        return createPaperCutServerProxy().getUserGroup(groupName);
    }

    @Override
    public List<String> getGroupHierarchy(final String parentGroup,
            final boolean formatted) {
        return new ArrayList<>();
    }

    @Override
    public boolean isGroupPresent(final String groupName) {
        return createPaperCutServerProxy().isGroupExists(groupName);
    }

    @Override
    public boolean isUserInGroup(final String uid, final String groupName) {
        return createPaperCutServerProxy().isGroupMemberExists(uid, groupName);
    }

    @Override
    public SortedSet<CommonUser> getUsers() {
        try {
            return createPaperCutServerProxy().listUserAccounts();
        } catch (PaperCutException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    @Override
    public CommonUser getUser(final String uid) {
        return createPaperCutServerProxy().getCommonUser(uid);
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName) {
        try {
            return createPaperCutServerProxy().getGroupMembers(groupName);
        } catch (PaperCutException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName,
            final boolean nested) {
        // Nesting is not supported.
        return getUsersInGroup(groupName);
    }

    @Override
    public RfidNumberFormat createRfidNumberFormat() {

        if (!this.isCardNumberProvided()) {
            return null;
        }

        RfidNumberFormat.FirstByte firstByte;
        RfidNumberFormat.Format format;

        ConfigManager cm = ConfigManager.instance();

        if (cm.getConfigValue(
                Key.CUSTOM_USER_SYNC_PAPERCUT_USER_CARD_NUMBER_FIRST_BYTE)
                .equals(IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB)) {
            firstByte = RfidNumberFormat.FirstByte.LSB;
        } else {
            firstByte = RfidNumberFormat.FirstByte.MSB;
        }

        if (cm.getConfigValue(
                Key.CUSTOM_USER_SYNC_PAPERCUT_USER_CARD_NUMBER_FORMAT)
                .equals(IConfigProp.CARD_NUMBER_FORMAT_V_HEX)) {
            format = RfidNumberFormat.Format.HEX;
        } else {
            format = RfidNumberFormat.Format.DEC;
        }
        return new RfidNumberFormat(format, firstByte);
    }

    @Override
    public boolean isIdNumberProvided() {
        return false;
    }

    @Override
    public boolean isCardNumberProvided() {
        return true;
    }

    @Override
    public boolean isEmailProvided() {
        return true;
    }

}
