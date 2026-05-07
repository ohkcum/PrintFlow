/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.server.api.request;

/**
 * Keys of entries in i18n message.xml file that belongs to this package.
 *
 * @author Rijk Ravestein
 *
 */
public enum ReqMessageEnum {

    /** */
    USER_REGISTRATION_LOCATION_NOT_PERMITTED(
            "msg-user-reg-location-not-permitted"),

    /** */
    USER_REGISTRATION_USERNAME_MISSING("msg-user-reg-username-missing"),
    /** */
    USER_REGISTRATION_USERNAME_TAKEN("msg-user-reg-username-taken"),
    /** */
    USER_REGISTRATION_FULLNAME_MISSING("msg-user-reg-fullname-missing"),
    /** */
    USER_REGISTRATION_EMAIL_INVALID("msg-user-reg-email-invalid"),
    /** */
    USER_REGISTRATION_EMAIL_NOT_PERMITTED("msg-user-reg-email-not-permitted"),
    /** */
    USER_REGISTRATION_EMAIL_TAKEN("msg-user-reg-email-taken"),

    /** */
    VOUCHER_BATCH_CREATED_OK("msg-voucher-batch-created-ok"),

    /** */
    VOUCHER_BATCH_DELETED_ZERO("msg-voucher-batch-deleted-zero", true),
    /** */
    VOUCHER_BATCH_DELETED_ONE("msg-voucher-batch-deleted-one", true),
    /** */
    VOUCHER_BATCH_DELETED_MANY("msg-voucher-batch-deleted-many", true),

    /** */
    VOUCHER_BATCH_EXPIRED_ZERO("msg-voucher-batch-expired-zero", true),
    /** */
    VOUCHER_BATCH_EXPIRED_ONE("msg-voucher-batch-expired-one", true),
    /** */
    VOUCHER_BATCH_EXPIRED_MANY("msg-voucher-batch-expired-many", true),

    /** */
    VOUCHER_DELETED_EXPIRED_ZERO("msg-voucher-deleted-expired-zero"),
    /** */
    VOUCHER_DELETED_EXPIRED_ONE("msg-voucher-deleted-expired-one"),
    /** */
    VOUCHER_DELETED_EXPIRED_MANY("msg-voucher-deleted-expired-many", true),

    /** */
    VOUCHER_REDEEM_OK("msg-voucher-redeem-ok"),
    /** */
    VOUCHER_REDEEM_INVALID("msg-voucher-redeem-invalid", true),
    /** */
    VOUCHER_REDEEM_USER_UNKNOWN("msg-voucher-redeem-user-unknown", true),

    /** */
    VOUCHER_USER_REDEEM_OK("msg-user-voucher-redeem-ok"),
    /** */
    VOUCHER_USER_REDEEM_VOID("msg-user-voucher-redeem-void"),
    /** */
    VOUCHER_USER_REDEEM_NUMBER_INVALID(
            "msg-user-voucher-redeem-number-invalid"),
    /** */
    VOUCHER_USER_REDEEM_USER_UNKNOWN("msg-user-voucher-redeem-user-unknown",
            true);

    /** */
    private final String msgKey;
    /** */
    private final boolean msgParms;

    ReqMessageEnum(final String key) {
        this.msgKey = key;
        this.msgParms = false;
    }

    ReqMessageEnum(final String key, final boolean parms) {
        this.msgKey = key;
        this.msgParms = parms;
    }

    /**
     * @return key in messages.xml file.
     */
    public String key() {
        return this.msgKey;
    }

    /**
     * @return {@code true} if message has parameters.
     */
    public boolean hasParms() {
        return this.msgParms;
    }

}
