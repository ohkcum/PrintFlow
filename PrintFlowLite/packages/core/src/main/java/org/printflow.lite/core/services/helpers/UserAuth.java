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
package org.printflow.lite.core.services.helpers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.config.WebAppTypeEnum;
import org.printflow.lite.core.dao.DeviceDao;
import org.printflow.lite.core.dao.enums.DeviceAttrEnum;
import org.printflow.lite.core.dao.enums.DeviceTypeEnum;
import org.printflow.lite.core.jpa.Device;
import org.printflow.lite.core.services.DeviceService;
import org.printflow.lite.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for User Authentication parameters of a Terminal {@link Device}.
 * <ul>
 * <li>The visible* members indicate if a Authentication Mode is visible.</li>
 * <li>The allow* members indicate if a Authentication Mode is allowed.</li>
 * </ul>
 * <p>
 * NOTE: An Authentication Mode may be <i>allowed</i> but <u>not</u> be
 * <i>visible</i>.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAuth {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserAuth.class);

    /** */
    private UserAuthModeEnum authModeDefault;

    /** */
    private boolean authIdPinReq;
    /** */
    private boolean authIdMasked;
    /** */
    private boolean authCardPinReq;

    /** */
    private boolean authCardSelfAssoc;
    /** */
    private Integer maxIdleSeconds;

    /** */
    private final Set<UserAuthModeEnum> allowedAuthModes = new HashSet<>();

    /** */
    private final Set<UserAuthModeEnum> visibleAuthModes = new HashSet<>();

    /**
     *
     */
    protected UserAuth() {
    }

    /**
     * Gets the UI text of an {@link Mode}.
     *
     * @param authMode
     *            The {@link UserAuthModeEnum} (can be {@code null}).
     * @return The UI text.
     */
    public static String getUiText(final UserAuthModeEnum authMode) {
        if (authMode == null) {
            // #21B7: CLOCKWISE TOP SEMICIRCLE ARROW
            return "↷";
        } else {
            switch (authMode) {
            case CARD_IP:
            case CARD_LOCAL:
                return "NFC";
            case OAUTH:
                return "OAuth";
            case YUBIKEY:
                return "YubiKey";
            case ID:
                return "ID";
            case EMAIL:
                return "Email";
            case NAME:
                return "~";
            default:
                throw new SpException(String.format("AuthMode %s not handled.",
                        authMode.toString()));
            }
        }
    }

    /**
     * Renders the User Authentication Mode parameters for a terminal.
     *
     * @param terminal
     *            The terminal where authentication is taken place.
     * @param authModeRequest
     *            The requested authentication mode (used to determine the
     *            default {@link Mode}). Can be {@code null}.
     * @param webAppType
     *            The type of Web App.
     * @param internetAccess
     *            If {@code true}, the Web App is requested from the Internet.
     */
    public UserAuth(final Device terminal, final String authModeRequest,
            final WebAppTypeEnum webAppType, final boolean internetAccess) {

        final boolean isUserWebAppContext = webAppType.isEndUserType();

        final boolean isAdminWebAppContext =
                webAppType.equals(WebAppTypeEnum.ADMIN);

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        if (terminal != null && !deviceDao.isTerminal(terminal)) {
            throw new SpException("Device [" + terminal.getDisplayName()
                    + "] is NOT of type " + DeviceTypeEnum.TERMINAL);
        }

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Defaults for member variables.
         */
        boolean visibleAuthName = false;
        boolean visibleAuthEmail = false;
        boolean visibleAuthId = false;
        boolean visibleAuthCardLocal = false;
        boolean visibleAuthCardIp = false;
        boolean visibleAuthYubikey = false;

        this.authModeDefault = null;
        this.maxIdleSeconds = null;

        this.authIdPinReq = cm.isConfigValue(Key.AUTH_MODE_ID_PIN_REQUIRED);
        this.authIdMasked = cm.isConfigValue(Key.AUTH_MODE_ID_IS_MASKED);

        this.authCardPinReq = cm.isConfigValue(Key.AUTH_MODE_CARD_PIN_REQUIRED);
        this.authCardSelfAssoc =
                cm.isConfigValue(Key.AUTH_MODE_CARD_SELF_ASSOCIATION);

        boolean allowAuthName = isAdminWebAppContext;
        boolean allowAuthEmail = false;
        boolean allowAuthId = false;
        boolean allowAuthCardLocal = false;
        boolean allowAuthCardIp = false;
        boolean allowAuthYubikey = false;
        /*
         * Intermediate parameters.
         */
        boolean showAuthName = false;
        boolean showAuthEmail = false;
        boolean showAuthId = false;
        boolean showAuthCardLocal = false;
        boolean showAuthCardIp = false;
        boolean showAuthYubiKey = false;

        UserAuthModeEnum authModeReq = null;

        if (authModeRequest != null) {
            authModeReq = UserAuthModeEnum.fromDbValue(authModeRequest);
        }

        /*
         * Check Device for CUSTOM authentication.
         */
        boolean isCustomAuth = false;
        DeviceService.DeviceAttrLookup customAuth = null;

        if (terminal != null && !terminal.getDisabled()) {
            /*
             * Device values.
             */
            customAuth = new DeviceService.DeviceAttrLookup(terminal);

            isCustomAuth = customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_IS_CUSTOM,
                    false);
        }

        if (customAuth != null && isCustomAuth) {
            /*
             * Custom Device values.
             */
            allowAuthName =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_NAME, false);
            allowAuthEmail =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_EMAIL, false);
            allowAuthId = customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_ID, false);
            allowAuthCardIp =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_IP, false);
            allowAuthCardLocal = customAuth
                    .isTrue(DeviceAttrEnum.AUTH_MODE_CARD_LOCAL, false);
            allowAuthYubikey =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_YUBIKEY, false);

            this.authModeDefault = UserAuthModeEnum.fromDbValue(
                    customAuth.get(DeviceAttrEnum.AUTH_MODE_DEFAULT,
                            UserAuthModeEnum.NAME.toDbValue()));

            showAuthName = allowAuthName;
            showAuthEmail = allowAuthEmail;
            showAuthId = allowAuthId;
            showAuthCardIp = allowAuthCardIp;
            showAuthCardLocal = allowAuthCardLocal;
            showAuthYubiKey = allowAuthYubikey;

            if (!isAdminWebAppContext) {
                this.maxIdleSeconds = Integer.valueOf(customAuth.get(
                        DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS,
                        cm.getConfigValue(Key.WEBAPP_USER_MAX_IDLE_SECS)));
            }

            this.authIdPinReq = customAuth.isTrue(
                    DeviceAttrEnum.AUTH_MODE_ID_PIN_REQ, this.authIdPinReq);

            this.authIdMasked = customAuth.isTrue(
                    DeviceAttrEnum.AUTH_MODE_ID_IS_MASKED, this.authIdMasked);

            this.authCardPinReq = customAuth.isTrue(
                    DeviceAttrEnum.AUTH_MODE_CARD_PIN_REQ, this.authCardPinReq);

            this.authCardSelfAssoc =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_SELF_ASSOC,
                            this.authCardSelfAssoc);

            /*
             * Correct for disabled Network Card Reader.
             */
            if (allowAuthCardIp && terminal.getCardReader() != null) {
                allowAuthCardIp = !terminal.getCardReader().getDisabled();
            }

        } else {
            /*
             * Use dedicated Web App values when accessed from the Internet.
             */
            Key keyInternetAuthEnable = null;
            Key keyInternetAuthModes = null;

            if (internetAccess) {
                switch (webAppType) {
                case ADMIN:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_ADMIN_AUTH_MODE_ENABLE;
                    keyInternetAuthModes = Key.WEBAPP_INTERNET_ADMIN_AUTH_MODES;
                    break;
                case PRINTSITE:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_PRINTSITE_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_PRINTSITE_AUTH_MODES;
                    break;
                case JOBTICKETS:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_JOBTICKETS_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_JOBTICKETS_AUTH_MODES;
                    break;
                case MAILTICKETS:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_MAILTICKETS_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_MAILTICKETS_AUTH_MODES;
                    break;
                case PAYMENT:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_PAYMENT_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_PAYMENT_AUTH_MODES;
                    break;
                case POS:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_POS_AUTH_MODE_ENABLE;
                    keyInternetAuthModes = Key.WEBAPP_INTERNET_POS_AUTH_MODES;
                    break;
                case USER:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_USER_AUTH_MODE_ENABLE;
                    keyInternetAuthModes = Key.WEBAPP_INTERNET_USER_AUTH_MODES;
                    break;
                default:
                    break;
                }
            }

            final boolean useInternetValues = keyInternetAuthEnable != null
                    && cm.isConfigValue(keyInternetAuthEnable);

            if (useInternetValues) {

                allowAuthName = false;

                final List<UserAuthModeEnum> internetAuthModes =
                        UserAuthModeEnum.parseList(
                                cm.getConfigValue(keyInternetAuthModes));

                int iMode = 0;

                for (final UserAuthModeEnum mode : internetAuthModes) {

                    if (mode == UserAuthModeEnum.OAUTH) {
                        if (internetAuthModes.size() == 1) {
                            this.authModeDefault = mode;
                        }
                        continue;
                    }

                    if (iMode == 0) {
                        this.authModeDefault = mode;
                    }
                    iMode++;

                    switch (mode) {
                    case CARD_LOCAL:
                        allowAuthCardLocal = true;
                        showAuthCardLocal = true;
                        break;
                    case CARD_IP:
                        allowAuthCardIp = true;
                        showAuthCardIp = true;
                        break;
                    case ID:
                        allowAuthId = true;
                        showAuthId = true;
                        break;
                    case EMAIL:
                        allowAuthEmail = true;
                        showAuthEmail = true;
                        break;
                    case NAME:
                        allowAuthName = true;
                        showAuthName = true;
                        break;
                    case YUBIKEY:
                        allowAuthYubikey = true;
                        showAuthYubiKey = true;
                        break;
                    default:
                        break;
                    }
                }

                if (!allowAuthName) {
                    this.authCardSelfAssoc = false;
                }

            } else {

                /*
                 * Global values.
                 */
                allowAuthName = cm.isConfigValue(Key.AUTH_MODE_NAME);
                allowAuthEmail = cm.isConfigValue(Key.AUTH_MODE_EMAIL);
                allowAuthId = cm.isConfigValue(Key.AUTH_MODE_ID);
                allowAuthCardLocal = cm.isConfigValue(Key.AUTH_MODE_CARD_LOCAL);
                allowAuthYubikey = cm.isConfigValue(Key.AUTH_MODE_YUBIKEY);
                allowAuthCardIp = false;

                showAuthName = cm.isConfigValue(Key.AUTH_MODE_NAME_SHOW);
                showAuthEmail = cm.isConfigValue(Key.AUTH_MODE_EMAIL_SHOW);
                showAuthId = cm.isConfigValue(Key.AUTH_MODE_ID_SHOW);
                showAuthCardLocal =
                        cm.isConfigValue(Key.AUTH_MODE_CARD_LOCAL_SHOW);
                showAuthYubiKey = cm.isConfigValue(Key.AUTH_MODE_YUBIKEY_SHOW);
                showAuthCardIp = false;

                this.authModeDefault = UserAuthModeEnum
                        .fromDbValue(cm.getConfigValue(Key.AUTH_MODE_DEFAULT));

                /*
                 * This can only occur when we refactored the URL API (URL
                 * parameter names). Just in case we fall-back to basic Username
                 * login.
                 */
                if (this.authModeDefault == null) {
                    LOGGER.warn("System AuthModeDefault ["
                            + cm.getConfigValue(Key.AUTH_MODE_DEFAULT)
                            + "] not found: using Username "
                            + "as default login method");
                    this.authModeDefault = UserAuthModeEnum.NAME;
                }

                boolean isValidDefault = false;

                switch (this.authModeDefault) {
                case CARD_LOCAL:
                    isValidDefault = allowAuthCardLocal && showAuthCardLocal;
                    break;
                case ID:
                    isValidDefault = allowAuthId && showAuthId;
                    break;
                case EMAIL:
                    isValidDefault = allowAuthEmail && showAuthEmail;
                    break;
                case NAME:
                    isValidDefault = allowAuthName && showAuthName;
                    break;
                case YUBIKEY:
                    isValidDefault = allowAuthYubikey && showAuthYubiKey;
                    break;
                default:
                    break;
                }

                if (!isValidDefault) {
                    this.authModeDefault = UserAuthModeEnum.NAME;
                }
            }
            //
            if (!isAdminWebAppContext) {
                this.maxIdleSeconds =
                        cm.getConfigInt(Key.WEBAPP_USER_MAX_IDLE_SECS);
            }
        }

        /*
         * Just in case.
         */
        if (this.authModeDefault == null) {
            this.authModeDefault = UserAuthModeEnum.NAME;
        }

        /*
         * Mode requested?
         */
        if (authModeReq == null) {

            visibleAuthName = allowAuthName && showAuthName;
            visibleAuthEmail = allowAuthEmail && showAuthEmail;
            visibleAuthId = allowAuthId && showAuthId;
            visibleAuthCardLocal = allowAuthCardLocal && showAuthCardLocal;
            visibleAuthCardIp = allowAuthCardIp && showAuthCardIp;
            visibleAuthYubikey = allowAuthYubikey && showAuthYubiKey;

            /*
             * INVARIANT: All WebApps except User WebApp SHOULD always be able
             * to login with user/password ...
             */
            if (!isUserWebAppContext) {
                if (internetAccess) {
                    /*
                     * ... in INTERNET context if all auth methods are disabled.
                     */
                    if (!visibleAuthName && !visibleAuthEmail && !visibleAuthId
                            && !visibleAuthCardLocal && !visibleAuthCardIp
                            && !visibleAuthYubikey) {
                        visibleAuthName = true;
                    }
                } else {
                    /*
                     * ... in INTRANET context.
                     */
                    visibleAuthName = true;
                }
            }

        } else {
            /*
             * Assign requested mode.
             */
            visibleAuthName = false;
            visibleAuthEmail = false;
            visibleAuthId = false;
            visibleAuthCardLocal = false;
            visibleAuthCardIp = false;
            visibleAuthYubikey = false;

            switch (authModeReq) {
            case CARD_IP:
                visibleAuthCardIp = allowAuthCardIp;
                break;
            case CARD_LOCAL:
                visibleAuthCardLocal = allowAuthCardLocal;
                break;
            case ID:
                visibleAuthId = allowAuthId;
                break;
            case EMAIL:
                visibleAuthEmail = allowAuthEmail;
                break;
            case NAME:
                visibleAuthName = allowAuthName;
                break;
            case YUBIKEY:
                visibleAuthYubikey = allowAuthYubikey;
                break;
            default:
                break;
            }
        }

        /*
         * INVARIANT: Admin WebApp does NOT support Network Card Reader
         * Authentication.
         */
        if (isAdminWebAppContext) {
            visibleAuthCardIp = false;
            allowAuthCardIp = false;
            allowAuthName = true;
        }

        /*
         * INVARIANT: The default MUST match a valid authentication method. If
         * not it should be corrected.
         */
        final boolean incorrectDefault =
                (this.authModeDefault == UserAuthModeEnum.NAME
                        && !visibleAuthName)
                        || (this.authModeDefault == UserAuthModeEnum.EMAIL
                                && !visibleAuthEmail)
                        || (this.authModeDefault == UserAuthModeEnum.ID
                                && !visibleAuthId)
                        || (this.authModeDefault == UserAuthModeEnum.CARD_LOCAL
                                && !visibleAuthCardLocal)
                        || (this.authModeDefault == UserAuthModeEnum.CARD_IP
                                && !visibleAuthCardIp)
                        || (this.authModeDefault == UserAuthModeEnum.YUBIKEY
                                && !visibleAuthYubikey);

        if (incorrectDefault) {
            /*
             * Assign the more advanced methods first.
             */
            if (visibleAuthYubikey) {
                this.authModeDefault = UserAuthModeEnum.YUBIKEY;
            } else if (visibleAuthCardIp) {
                this.authModeDefault = UserAuthModeEnum.CARD_IP;
            } else if (visibleAuthCardLocal) {
                this.authModeDefault = UserAuthModeEnum.CARD_LOCAL;
            } else if (visibleAuthId) {
                this.authModeDefault = UserAuthModeEnum.ID;
            } else if (visibleAuthEmail) {
                this.authModeDefault = UserAuthModeEnum.EMAIL;
            } else if (visibleAuthName) {
                this.authModeDefault = UserAuthModeEnum.NAME;
            }
        }

        /*
         * Wrap up.
         */
        if (allowAuthName) {
            this.allowedAuthModes.add(UserAuthModeEnum.NAME);
        }
        if (allowAuthEmail) {
            this.allowedAuthModes.add(UserAuthModeEnum.EMAIL);
        }
        if (allowAuthId) {
            this.allowedAuthModes.add(UserAuthModeEnum.ID);
        }
        if (allowAuthCardLocal) {
            this.allowedAuthModes.add(UserAuthModeEnum.CARD_LOCAL);
        }
        if (allowAuthCardIp) {
            this.allowedAuthModes.add(UserAuthModeEnum.CARD_IP);
        }
        if (allowAuthYubikey) {
            this.allowedAuthModes.add(UserAuthModeEnum.YUBIKEY);
        }
        //
        if (visibleAuthName) {
            this.visibleAuthModes.add(UserAuthModeEnum.NAME);
        }
        if (visibleAuthEmail) {
            this.visibleAuthModes.add(UserAuthModeEnum.EMAIL);
        }
        if (visibleAuthId) {
            this.visibleAuthModes.add(UserAuthModeEnum.ID);
        }
        if (visibleAuthCardLocal) {
            this.visibleAuthModes.add(UserAuthModeEnum.CARD_LOCAL);
        }
        if (visibleAuthCardIp) {
            this.visibleAuthModes.add(UserAuthModeEnum.CARD_IP);
        }
        if (visibleAuthYubikey) {
            this.visibleAuthModes.add(UserAuthModeEnum.YUBIKEY);
        }
    }

    /**
     * @return Allowed authentications.
     */
    public Set<UserAuthModeEnum> getAuthModesAllowed() {
        return this.allowedAuthModes;
    }

    /**
     * Check if an authentication mode is allowed.
     * <p>
     * NOTE: when Card Self Association is enabled, {@link Mode#ID} is ALWAYS
     * allowed.
     * </p>
     *
     * @param mode
     *            The authentication mode.
     * @return {@code true} when allowed.
     */
    public boolean isAuthModeAllowed(final UserAuthModeEnum mode) {

        if (mode == UserAuthModeEnum.NAME && authCardSelfAssoc) {
            return true;
        }
        return this.allowedAuthModes.contains(mode);
    }

    public boolean isVisibleAuthName() {
        return this.visibleAuthModes.contains(UserAuthModeEnum.NAME);
    }

    public boolean isVisibleAuthEmail() {
        return this.visibleAuthModes.contains(UserAuthModeEnum.EMAIL);
    }

    public boolean isVisibleAuthId() {
        return this.visibleAuthModes.contains(UserAuthModeEnum.ID);
    }

    public boolean isVisibleAuthCardLocal() {
        return this.visibleAuthModes.contains(UserAuthModeEnum.CARD_LOCAL);
    }

    public boolean isVisibleAuthCardIp() {
        return this.visibleAuthModes.contains(UserAuthModeEnum.CARD_IP);
    }

    public boolean isVisibleAuthYubikey() {
        return this.visibleAuthModes.contains(UserAuthModeEnum.YUBIKEY);
    }

    public UserAuthModeEnum getAuthModeDefault() {
        return authModeDefault;
    }

    public boolean isAuthIdPinReq() {
        return authIdPinReq;
    }

    public boolean isAuthCardPinReq() {
        return authCardPinReq;
    }

    public boolean isAuthIdMasked() {
        return authIdMasked;
    }

    public boolean isAuthCardSelfAssoc() {
        return authCardSelfAssoc;
    }

    public Integer getMaxIdleSeconds() {
        return maxIdleSeconds;
    }

    public boolean isAllowAuthName() {
        return this.allowedAuthModes.contains(UserAuthModeEnum.NAME);
    }

    public boolean isAllowAuthEmail() {
        return this.allowedAuthModes.contains(UserAuthModeEnum.EMAIL);
    }

    public boolean isAllowAuthId() {
        return this.allowedAuthModes.contains(UserAuthModeEnum.ID);
    }

    public boolean isAllowAuthCardLocal() {
        return this.allowedAuthModes.contains(UserAuthModeEnum.CARD_LOCAL);
    }

    public boolean isAllowAuthCardIp() {
        return this.allowedAuthModes.contains(UserAuthModeEnum.CARD_IP);
    }

    public boolean isAllowAuthYubikey() {
        return this.allowedAuthModes.contains(UserAuthModeEnum.YUBIKEY);
    }

}
