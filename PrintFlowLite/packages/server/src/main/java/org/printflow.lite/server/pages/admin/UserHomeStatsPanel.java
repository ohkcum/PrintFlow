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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.server.pages.admin;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.dao.UserDao;
import org.printflow.lite.core.dto.UserHomeStatsDto;
import org.printflow.lite.core.i18n.AdjectiveEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.UserService;
import org.printflow.lite.core.util.LocaleHelper;
import org.printflow.lite.server.WebApp;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserHomeStatsPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();
    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /** */
    private static final String WID_BTN_USERHOME_REFRESH =
            "btn-userhome-refresh";

    /** */
    private static final String WID_USER_COUNT = "user-count";

    /** */
    private static final String WID_USER_HOME_COUNT = "user-home-count";

    /** */
    private static final String WID_USER_HOME_COUNT_SPAN =
            "user-home-count-span";

    /** */
    private static final String WID_STATS_DATE = "stats-date";

    /** */
    private static final String WID_IMG_USER = "img-user";

    /** */
    private static final String WID_IMG_STATS_UNKNOWN = "img-stats-unknown";

    /** */
    private static final String WID_IMG_STATS_CLEANUP = "img-stats-cleanup";

    /** */
    private static final String WID_IMG_STATS_INBOX = "img-stats-inbox";

    /** */
    private static final String WID_IMG_STATS_OUTBOX = "img-stats-outbox";

    /** */
    private static final String WID_IMG_USER_REGISTRATION_COUNT_PENDING =
            "img-user-registration-count-pending";

    /** */
    private static final String WID_IMG_USER_REGISTRATION_COUNT_EXPIRED =
            "img-user-registration-count-expired";

    /** */
    private static final String WID_STATS_UNKNOWN_COUNT = "stats-unknown-count";

    /** */
    private static final String WID_STATS_CLEANUP_COUNT = "stats-cleanup-count";

    /** */
    private static final String WID_USER_REGISTRATION_COUNT_PENDING =
            "user-registration-count-pending";

    /** */
    private static final String WID_USER_REGISTRATION_COUNT_EXPIRED =
            "user-registration-count-expired";

    /** */
    private static final String WID_STATS_INBOX_COUNT = "stats-inbox-count";

    /** */
    private static final String WID_STATS_UNKNOWN_COUNT_SPAN =
            WID_STATS_UNKNOWN_COUNT + "-span";

    /** */
    private static final String WID_STATS_CLEANUP_COUNT_SPAN =
            WID_STATS_CLEANUP_COUNT + "-span";

    /** */
    private static final String WID_STATS_INBOX_COUNT_SPAN =
            WID_STATS_INBOX_COUNT + "-span";

    /** */
    private static final String WID_USER_REGISTRATION_COUNT_PENDING_SPAN =
            WID_USER_REGISTRATION_COUNT_PENDING + "-span";
    /** */
    private static final String WID_USER_REGISTRATION_COUNT_EXPIRED_SPAN =
            WID_USER_REGISTRATION_COUNT_EXPIRED + "-span";

    /** */
    private static final String WID_STATS_INBOX_SIZE = "stats-inbox-size";

    /** */
    private static final String WID_STATS_OUTBOX_COUNT = "stats-outbox-count";

    /** */
    private static final String WID_STATS_OUTBOX_COUNT_SPAN =
            WID_STATS_OUTBOX_COUNT + "-span";

    /** */
    private static final String WID_STATS_OUTBOX_SIZE = "stats-outbox-size";

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public UserHomeStatsPanel(final String id) {
        super(id);
    }

    /**
     * @param helper
     */
    private void populateRegistration(final MarkupHelper helper) {

        final long nPending;
        final long nExpired;

        if (ConfigManager.isUserRegistrationEnabled()) {

            final Date expiryDate =
                    USER_SERVICE.getUserRegistrationExpiry(new Date());

            nPending = USER_DAO.countRegistrationsPending(expiryDate);
            nExpired = USER_DAO.countRegistrationsExpired(expiryDate);

        } else {
            nPending = 0;
            nExpired = 0;
        }

        if (nPending > 0) {
            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(
                            WID_USER_REGISTRATION_COUNT_PENDING_SPAN),
                    MarkupHelper.ATTR_TITLE,
                    NounEnum.REGISTRATION.uiText(getLocale(), true) + " "
                            + AdjectiveEnum.PENDING.uiText(getLocale())
                                    .toLowerCase());

            helper.addModifyLabelAttr(WID_IMG_USER_REGISTRATION_COUNT_PENDING,
                    MarkupHelper.ATTR_SRC,
                    MarkupHelper.IMG_PATH_USER_REGISTRATION_PENDING);

            helper.addLabel(WID_USER_REGISTRATION_COUNT_PENDING,
                    String.format("%d", nPending));

        } else {
            helper.discloseLabel(WID_USER_REGISTRATION_COUNT_PENDING);
        }

        if (nExpired > 0) {
            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(
                            WID_USER_REGISTRATION_COUNT_EXPIRED_SPAN),
                    MarkupHelper.ATTR_TITLE,
                    NounEnum.REGISTRATION.uiText(getLocale(), true) + " : "
                            + AdjectiveEnum.EXPIRED.uiText(getLocale())
                                    .toLowerCase());

            helper.addModifyLabelAttr(WID_IMG_USER_REGISTRATION_COUNT_EXPIRED,
                    MarkupHelper.ATTR_SRC,
                    MarkupHelper.IMG_PATH_USER_REGISTRATION_EXPIRED);

            helper.addLabel(WID_USER_REGISTRATION_COUNT_EXPIRED,
                    String.format("%d", nExpired));
        } else {
            helper.discloseLabel(WID_USER_REGISTRATION_COUNT_EXPIRED);
        }
    }

    /**
     * @param dto
     *            User Home statistics. If {@code null} no statistics are
     *            available.
     * @param hasEditorAccess
     *            {@code true} If editor access.
     */
    public void populate(final UserHomeStatsDto dto,
            final boolean hasEditorAccess) {

        final MarkupHelper helper = new MarkupHelper(this);
        final LocaleHelper localeHelper = new LocaleHelper(getLocale());
        final UserDao userDAO = ServiceContext.getDaoContext().getUserDao();

        helper.addLabel(WID_USER_COUNT,
                helper.localizedNumber(userDAO.countActiveUsers()));

        this.populateRegistration(helper);

        final long userHomeCount;
        if (dto == null) {
            userHomeCount = 0;
        } else {
            userHomeCount = dto.getCurrent().getUsers().getCount();
        }

        if (dto == null) {
            helper.discloseLabel(WID_USER_HOME_COUNT);
            helper.discloseLabel(WID_STATS_DATE);
        } else {
            helper.addLabel(WID_USER_HOME_COUNT,
                    helper.localizedNumber(userHomeCount));
            helper.addModifyLabelAttr(WID_IMG_USER, MarkupHelper.ATTR_SRC,
                    MarkupHelper.IMG_PATH_GENERIC_HDD);

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_USER_HOME_COUNT_SPAN),
                    MarkupHelper.ATTR_TITLE,
                    FileUtils.byteCountToDisplaySize(dto.calcScannedBytes()));

            helper.addLabel(WID_STATS_DATE,
                    localeHelper.getLongMediumDateTime(dto.getDate())
                            .replace(" ", "&nbsp;"))
                    .setEscapeModelStrings(false);
        }

        final long cleanupCount;
        if (dto == null || dto.getCleanup() == null) {
            cleanupCount = 0;
        } else {
            cleanupCount = dto.calcCleanupFiles();
        }
        if (cleanupCount > 0) {

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_STATS_CLEANUP_COUNT_SPAN),
                    MarkupHelper.ATTR_TITLE,
                    FileUtils.byteCountToDisplaySize(dto.calcCleanupBytes()));

            final String imgPath;

            if (dto.isCleaned()) {
                imgPath = MarkupHelper.IMG_PATH_GENERIC_TIME;
            } else {
                imgPath = MarkupHelper.IMG_PATH_GENERIC_TIME_DELETE;
            }
            helper.addModifyLabelAttr(WID_IMG_STATS_CLEANUP,
                    MarkupHelper.ATTR_SRC, imgPath);

            helper.addLabel(WID_STATS_CLEANUP_COUNT,
                    String.format("%d", cleanupCount));

        } else {
            helper.discloseLabel(WID_STATS_CLEANUP_COUNT);
        }

        final long unknownCount;
        if (dto == null || dto.getCurrent().getUnkown() == null) {
            unknownCount = 0;
        } else {
            unknownCount = dto.getCurrent().getUnkown().getCount();
        }
        if (unknownCount > 0) {

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_STATS_UNKNOWN_COUNT_SPAN),
                    MarkupHelper.ATTR_TITLE, FileUtils.byteCountToDisplaySize(
                            dto.getCurrent().getUnkown().getSize()));

            helper.addModifyLabelAttr(WID_IMG_STATS_UNKNOWN,
                    MarkupHelper.ATTR_SRC,
                    MarkupHelper.IMG_PATH_GENERIC_CROSS_RED);

            helper.addLabel(WID_STATS_UNKNOWN_COUNT,
                    String.format("%d", unknownCount));

        } else {
            helper.discloseLabel(WID_STATS_UNKNOWN_COUNT);
        }

        final long inboxCount;
        if (dto == null) {
            inboxCount = 0;
        } else {
            inboxCount = dto.getCurrent().getInbox().getCount();
        }

        boolean encloseSize = false;

        if (inboxCount > 0) {

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_STATS_INBOX_COUNT_SPAN),
                    MarkupHelper.ATTR_TITLE, FileUtils.byteCountToDisplaySize(
                            dto.getCurrent().getInbox().getSize()));

            helper.addModifyLabelAttr(WID_IMG_STATS_INBOX,
                    MarkupHelper.ATTR_SRC,
                    String.format("%s%c%s", WebApp.PATH_IMAGES_FILETYPE,
                            File.separatorChar, "pdf-32.png"));

            helper.addLabel(WID_STATS_INBOX_COUNT,
                    String.format("%d", inboxCount));

            if (encloseSize) {
                helper.addLabel(WID_STATS_INBOX_SIZE,
                        FileUtils.byteCountToDisplaySize(
                                dto.getCurrent().getInbox().getSize()));
            } else {
                helper.discloseLabel(WID_STATS_INBOX_SIZE);
            }

        } else {
            helper.discloseLabel(WID_STATS_INBOX_COUNT);
        }

        final long outboxCount;
        if (dto == null) {
            outboxCount = 0;
        } else {
            outboxCount = dto.getCurrent().getOutbox().getCount();
        }
        if (outboxCount > 0) {

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_STATS_OUTBOX_COUNT_SPAN),
                    MarkupHelper.ATTR_TITLE, FileUtils.byteCountToDisplaySize(
                            dto.getCurrent().getOutbox().getSize()));

            helper.addModifyLabelAttr(WID_IMG_STATS_OUTBOX,
                    MarkupHelper.ATTR_SRC,
                    String.format("%s%c%s", WebApp.PATH_IMAGES,
                            File.separatorChar,
                            "printer-terminal-auth-16x16.png"));
            helper.addLabel(WID_STATS_OUTBOX_COUNT,
                    String.format("%d", outboxCount));

            if (encloseSize) {
                helper.addLabel(WID_STATS_OUTBOX_SIZE,
                        FileUtils.byteCountToDisplaySize(
                                dto.getCurrent().getOutbox().getSize()));
            } else {
                helper.discloseLabel(WID_STATS_OUTBOX_SIZE);
            }

        } else {
            helper.discloseLabel(WID_STATS_OUTBOX_COUNT);
        }

        if (hasEditorAccess) {
            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(WID_BTN_USERHOME_REFRESH),
                    MarkupHelper.ATTR_TITLE,
                    HtmlButtonEnum.REFRESH.uiText(getLocale(), true));
        } else {
            helper.discloseLabel(WID_BTN_USERHOME_REFRESH);
        }

    }

}
