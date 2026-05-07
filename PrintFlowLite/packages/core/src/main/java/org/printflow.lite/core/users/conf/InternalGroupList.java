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
package org.printflow.lite.core.users.conf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IServerDataFile;
import org.printflow.lite.core.config.ServerDataFileNameEnum;
import org.printflow.lite.core.users.CommonUser;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class InternalGroupList implements IServerDataFile {

    /** */
    private static final class GroupReader extends ConfFileReader {

        private Set<String> groups;
        private Map<String, Boolean> groupsMember;
        private String user;

        @Override
        protected void onItem(final String group, final String userId) {

            if (this.user == null) {
                this.groups.add(group);
            } else {
                this.groupsMember.put(group,
                        Boolean.valueOf(userId.equals(this.user)));
            }
        }

        public Set<String> getGroups(final File file) throws IOException {
            this.groups = new HashSet<>();
            this.user = null;
            this.read(file);
            return groups;
        }

        public Map<String, Boolean> getGroups(final File file,
                final String userId) throws IOException {
            this.groupsMember = new HashMap<>();
            this.user = userId;
            this.read(file);
            return groupsMember;
        }
    }

    /**
     * .
     */
    private static final class GroupMemberReader extends ConfFileReader {

        /** */
        private final String userGroup;

        /** */
        private final SortedSet<CommonUser> users =
                CommonUser.createSortedSet();

        GroupMemberReader(final String groupName) {
            this.userGroup = groupName;
        }

        @Override
        protected void onItem(final String group, final String userId) {
            if (group.equals(this.userGroup)) {
                final CommonUser commonUser = new CommonUser();
                commonUser.setUserName(userId);
                this.users.add(commonUser);
            }
        }

        public SortedSet<CommonUser> getMembers(final File file)
                throws IOException {
            this.read(file);
            return this.users;
        }
    }

    /**
     *
     */
    public InternalGroupList() {
    }

    private static File getFile() {
        return ServerDataFileNameEnum.INTERNAL_GROUPS_TXT
                .getPathAbsolute(ConfigManager.getServerHomePath()).toFile();
    }

    /**
     * Gets all the groups.
     *
     * @return The groups.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static Set<String> getGroups() throws IOException {
        return new GroupReader().getGroups(getFile());
    }

    /**
     * Gets the users in a group.
     *
     * @param groupName
     *            The group name.
     * @return The groups.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static SortedSet<CommonUser> getUsersInGroup(final String groupName)
            throws IOException {
        return new GroupMemberReader(groupName).getMembers(getFile());
    }

    /**
     * Gets all the groups with an indication if userId is found as a member.
     *
     * @param userId
     *            The user id.
     * @return The groups/membership map.
     * @throws IOException
     *             When IO errors reading the groups file.
     */
    public static Map<String, Boolean> getGroupsOfUser(final String userId)
            throws IOException {
        return new GroupReader().getGroups(getFile(), userId);
    }

}
