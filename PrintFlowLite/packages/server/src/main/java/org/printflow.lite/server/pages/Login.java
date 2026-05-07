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
package org.printflow.lite.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.config.WebAppTypeEnum;
import org.printflow.lite.core.dao.DeviceDao;
import org.printflow.lite.core.dao.enums.ACLRoleEnum;
import org.printflow.lite.core.dao.enums.DeviceTypeEnum;
import org.printflow.lite.core.dao.enums.ExternalSupplierEnum;
import org.printflow.lite.core.i18n.LabelEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.i18n.SystemModeEnum;
import org.printflow.lite.core.jpa.Device;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.UserAuth;
import org.printflow.lite.core.services.helpers.UserAuthModeEnum;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.core.util.LocaleHelper;
import org.printflow.lite.ext.oauth.OAuthClientPlugin;
import org.printflow.lite.ext.oauth.OAuthProviderEnum;
import org.printflow.lite.ext.telegram.TelegramHelper;
import org.printflow.lite.server.WebApp;
import org.printflow.lite.server.WebAppParmEnum;
import org.printflow.lite.server.ext.ServerPluginHelper;
import org.printflow.lite.server.ext.ServerPluginManager;
import org.printflow.lite.server.helpers.HtmlButtonEnum;
import org.printflow.lite.server.session.SpSession;

/**
 * Note that this Page is not extended from Page.
 *
 * @author Rijk Ravestein
 *
 */
public final class Login extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_MAINTENANCE_HEADER = "maintenance-header";
    /** */
    private static final String WID_MAINTENANCE_BODY = "maintenance-body";

    /** */
    private static final String WID_RESET_HEADER = "reset-header";
    /** */
    private static final String WID_RESET_BODY = "reset-body";

    /** */
    private void handlePasswordPanels() {

        for (final String wid : new String[] { "sp-login-user-password",
                "sp-login-email-password", "sp-login-user-password-assoc",
                "sp-login-card-local-pin", "sp-login-card-ip-pin" }) {
            this.add(PasswordPanel.createPopulate(wid + "-panel", wid, null));
        }
    }

    /**
     * @param helper
     * @param parameters
     */
    private void handleRegistration(final MarkupHelper helper,
            final PageParameters parameters) {

        final boolean isEnabled =
                this.getWebAppTypeEnum(parameters) == WebAppTypeEnum.USER
                        && ConfigManager.isUserRegistrationAllowed(
                                this.getClientIpAddr());

        helper.encloseLabel("btn-registration",
                HtmlButtonEnum.REGISTER.uiText(getLocale()), isEnabled);

        helper.encloseLabel("label-registration-register",
                HtmlButtonEnum.REGISTER.uiText(getLocale()), isEnabled);

        if (isEnabled) {

            final String regNamePfx = StringUtils.defaultString(ConfigManager
                    .instance().getConfigValue(Key.INTERNAL_USERS_NAME_PREFIX));

            helper.encloseLabel("label-registration-name-prefix", regNamePfx,
                    regNamePfx.length() > 0);

            helper.addLabel("label-registration-name",
                    this.localized("label-user-name") + " / "
                            + NounEnum.ID.uiText(this.getLocale()));

            helper.addLabel("label-registration-fullname", NounEnum.NAME);
            helper.addLabel("label-registration-email", NounEnum.EMAIL);

            helper.addLabel("label-registration-password-confirm",
                    HtmlButtonEnum.CONFIRM.uiText(getLocale()));

            for (final String wid : new String[] { //
                    "sp-registration-user-password",
                    "sp-registration-user-password-confirm" }) {
                this.add(PasswordPanel.createPopulate(wid + "-panel", wid,
                        null));
            }

            helper.addLabel("btn-registration-send",
                    HtmlButtonEnum.SEND.uiText(getLocale()));
            helper.addLabel("btn-registration-cancel",
                    HtmlButtonEnum.CANCEL.uiText(getLocale()));
        }
    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public Login(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        this.handlePasswordPanels();
        this.handleRegistration(helper, parameters);

        final SpSession session = SpSession.get();

        final Set<Locale> availableLocales = LocaleHelper.getI18nAvailable();

        if (availableLocales.size() == 1) {
            session.setLocale(availableLocales.iterator().next());
        }

        helper.encloseLabel("button-lang", getString("button-lang"),
                availableLocales.size() > 1);

        helper.addLabel("label-email-1", NounEnum.EMAIL);
        helper.addLabel("label-email-2", NounEnum.EMAIL);

        //
        helper.addLabel("header-2-step", LabelEnum.TWO_STEP_VERIFICATION);

        //
        final TOTPInputPanel.ToolTip ttKey;
        if (TelegramHelper.isTOTPEnabled()) {
            ttKey = TOTPInputPanel.ToolTip.AUTH_APP_TELEGRAM;
        } else {
            ttKey = TOTPInputPanel.ToolTip.AUTH_APP;
        }
        this.add(new TOTPInputPanel("login-user-totp-code-panel",
                "sp-login-user-totp-code", ttKey));
        //
        final boolean showTotpSend =
                this.getWebAppTypeEnum(parameters) == WebAppTypeEnum.USER;
        helper.encloseLabel("btn-login-totp-send",
                HtmlButtonEnum.SEND.uiText(getLocale()), showTotpSend);

        if (showTotpSend) {
            /* A random 'name' to tell Bitwarden 'code-sent' is NOT "totp". */
            final Label totpSent = helper.addModifyLabelAttr(
                    "login-user-totp-code-sent", MarkupHelper.ATTR_NAME,
                    UUID.randomUUID().toString());
            /* For other password managers that generate TOTP codes. */
            MarkupHelper.modifyLabelAttr(totpSent,
                    MarkupHelper.ATTR_AUTOCOMPLETE,
                    MarkupHelper.ATTR_AUTOCOMPLETE_OFF);
        }
        //
        add(new Label("title",
                localized("title", CommunityDictEnum.PrintFlowLite.getWord())));

        add(new Label("title-assoc", CommunityDictEnum.PrintFlowLite.getWord()));

        final String loginDescript;

        /*
         * At this point we can NOT use the authenticated Web App Type from the
         * session, since this is the first Web App in the browser, or a new
         * browser tab with another Web App Type. So, we use a request parameter
         * to determine the Web App Type.
         */
        final IRequestParameters parms =
                getRequestCycle().getRequest().getPostParameters();

        final WebAppTypeEnum webAppType =
                EnumUtils.getEnum(WebAppTypeEnum.class,
                        parms.getParameterValue(POST_PARM_WEBAPPTYPE)
                                .toString(WebAppTypeEnum.UNDEFINED.toString()));

        final HtmlInjectComponent htmlInject = new HtmlInjectComponent(
                "login-inject", webAppType, HtmlInjectEnum.LOGIN);

        if (htmlInject.isInjectAvailable()) {
            add(htmlInject);
            loginDescript = null;
        } else {
            helper.discloseLabel("login-inject");
            switch (webAppType) {
            case ADMIN:
                loginDescript = localized("login-descript-admin");
                break;
            case PRINTSITE:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.PRINT_SITE_OPERATOR.uiText(getLocale()));
                break;
            case JOBTICKETS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.JOB_TICKET_OPERATOR.uiText(getLocale()));
                break;
            case MAILTICKETS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.MAIL_TICKET_OPERATOR.uiText(getLocale()));
                break;
            case POS:
                loginDescript = localized("login-descript-role",
                        ACLRoleEnum.WEB_CASHIER.uiText(getLocale()));
                break;
            case PAYMENT:
                loginDescript = localized("login-descript-user-payment");
                break;
            default:
                loginDescript = localized("login-descript-user");
                break;
            }
        }

        helper.encloseLabel("login-descript", loginDescript,
                loginDescript != null);

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final String clientIpAddr = this.getClientIpAddr();

        final Device terminal = deviceDao.findByHostDeviceType(clientIpAddr,
                DeviceTypeEnum.TERMINAL);

        final UserAuth userAuth = new UserAuth(terminal, null, webAppType,
                InetUtils.isPublicAddress(clientIpAddr));

        //
        if (userAuth.isAuthIdMasked()) {
            helper.discloseLabel("sp-login-id-number");
            this.add(PasswordPanel.createPopulate("sp-login-id-number-panel",
                    "sp-login-id-number", null));
        } else {
            this.addVisible(false, "sp-login-id-number-panel", "");
            final Label labelIdNumber = new Label("sp-login-id-number");
            this.add(labelIdNumber);
        }

        //
        if (userAuth.isAuthIdPinReq()) {
            this.add(PasswordPanel.createPopulate("sp-login-id-pin-panel",
                    "sp-login-id-pin", null));
        } else {
            this.addVisible(false, "sp-login-id-pin-panel", "");
        }

        // For now, restrict OAuth to User|Payment Web App only...
        final boolean restrictTolocalLogin = !webAppType.isEndUserType()
                || parms.getParameterValue(WebAppParmEnum.PFL_LOGIN_LOCAL.parm())
                        .toString() != null;

        this.addOAuthButtons(restrictTolocalLogin, userAuth);

        //
        if (ConfigManager.getSystemMode() == SystemModeEnum.MAINTENANCE) {
            helper.addLabel(WID_MAINTENANCE_HEADER,
                    localized("maintenance-header"));
            helper.addLabel(WID_MAINTENANCE_BODY,
                    localized("maintenance-body"));
        } else {
            helper.discloseLabel(WID_MAINTENANCE_HEADER);
        }

        final boolean anotherSessionActive = session.getAuthWebAppCount() != 0;
        final String btnTextReset = HtmlButtonEnum.RESET.uiText(getLocale());
        helper.encloseLabel("button-reset", btnTextReset, anotherSessionActive);
        if (anotherSessionActive) {
            helper.addLabel(WID_RESET_HEADER,
                    PhraseEnum.ANOTHER_BROWSER_SESSION_ACTIVE
                            .uiText(getLocale()));
            helper.addLabel(WID_RESET_BODY,
                    localized("reset-body", btnTextReset));
        } else {
            helper.discloseLabel(WID_RESET_HEADER);
        }
    }

    /**
     *
     * @param localLoginRestricted
     *            If {@code true}, login is restricted to local methods.
     * @param userAuth
     * @return {@code true} if OAuth is the only login method available.
     */
    private boolean addOAuthButtons(final boolean localLoginRestricted,
            final UserAuth userAuth) {

        final List<OAuthClientPlugin> pluginList = new ArrayList<>();
        final List<Pair<OAuthProviderEnum, String>> oauthList =
                new ArrayList<>();

        final ServerPluginManager mgr = WebApp.get().getPluginManager();

        if (!localLoginRestricted) {

            for (final Entry<OAuthProviderEnum, //
                    Map<String, OAuthClientPlugin>> entry : mgr
                            .getOAuthClientPlugins().entrySet()) {

                final OAuthProviderEnum provider = entry.getKey();

                for (final Entry<String, OAuthClientPlugin> entry2 : entry
                        .getValue().entrySet()) {
                    final String instanceId = entry2.getKey();
                    oauthList.add(new ImmutablePair<OAuthProviderEnum, String>(
                            provider, instanceId));
                    pluginList.add(entry2.getValue());
                }
            }
        }

        add(new PropertyListView<OAuthClientPlugin>("ext-supplier-icon-styles",
                pluginList) {

            private static final long serialVersionUID = 1L;

            private static final String OAUTH_PROVIDER_ICON_FORMAT =
                    ".ui-icon-ext-oauth-provider-%s%s:after { " + "background: "
                            + "url(%s) " + "50%% 50%% no-repeat; "
                            + "background-size: 22px 22px; "
                            + "padding-left: 15px; "
                            + "-webkit-border-radius: 0 !important; "
                            + "border-radius: 0 !important; }";

            @Override
            protected void
                    populateItem(final ListItem<OAuthClientPlugin> item) {

                final OAuthClientPlugin plugin = item.getModelObject();

                @SuppressWarnings("unused")
                final ExternalSupplierEnum supplier =
                        ServerPluginHelper.getEnum(plugin.getProvider());

                final Label label = new Label("ext-supplier-icon-style",
                        String.format(OAUTH_PROVIDER_ICON_FORMAT,
                                plugin.getProvider().toString().toLowerCase(),
                                StringUtils
                                        .defaultString(plugin.getInstanceId()),
                                ServerPluginManager
                                        .getOAuthClientIconPath(plugin)));

                label.setEscapeModelStrings(false);
                item.add(label);
            }
        });

        final boolean isOAuthOnly;
        if (oauthList.isEmpty()) {
            isOAuthOnly = false;
        } else {
            final Set<UserAuthModeEnum> setAllowed =
                    userAuth.getAuthModesAllowed();
            isOAuthOnly = setAllowed.isEmpty() || (setAllowed.size() == 1
                    && setAllowed.contains(UserAuthModeEnum.OAUTH));
        }

        add(new PropertyListView<Pair<OAuthProviderEnum, String>>(
                "oauth-buttons", oauthList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                    final ListItem<Pair<OAuthProviderEnum, String>> item) {

                final Pair<OAuthProviderEnum, String> pair =
                        item.getModelObject();

                final OAuthProviderEnum provider = pair.getLeft();
                final String instanceId = pair.getRight();

                final boolean multipleOAuthProviderInstances =
                        instanceId != OAuthClientPlugin.ID_ONE_OAUTH_PROVIDER;

                final OAuthClientPlugin plugin =
                        mgr.getOAuthClient(provider, instanceId);

                final boolean showButtonIcon = plugin.showLoginButtonIcon();
                final boolean hasButtonText =
                        StringUtils.isNotBlank(plugin.getLoginButtonText());

                final StringBuilder btnText = new StringBuilder();

                if (hasButtonText) {
                    btnText.append("<span class=\"");
                    btnText.append(MarkupHelper.CSS_TXT_INFO);
                    btnText.append("\">");
                    if (showButtonIcon) {
                        btnText.append("&nbsp;&nbsp;");
                    }
                    btnText.append(plugin.getLoginButtonText())
                            .append("</span>");
                } else {
                    btnText.append("&nbsp;");
                }

                final Label label = new Label("oauth-button", btnText);

                if (showButtonIcon) {
                    MarkupHelper.appendLabelAttr(label, MarkupHelper.ATTR_CLASS,
                            String.format("ui-icon-ext-oauth-provider-%s%s",
                                    provider.toString().toLowerCase(),
                                    StringUtils.defaultString(instanceId)));
                }

                if (!isOAuthOnly) {
                    MarkupHelper.appendLabelAttr(label, MarkupHelper.ATTR_CLASS,
                            MarkupHelper.CSS_UI_MINI);
                }

                MarkupHelper.modifyLabelAttr(label,
                        MarkupHelper.ATTR_DATA_PrintFlowLite, provider.toString());

                final StringBuilder title = new StringBuilder();
                title.append(provider.uiText());

                if (multipleOAuthProviderInstances) {
                    MarkupHelper.modifyLabelAttr(label,
                            MarkupHelper.ATTR_DATA_PrintFlowLite_TYPE, instanceId);
                    title.append(" | ").append(instanceId);
                }

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_TITLE,
                        title.toString());

                label.setEscapeModelStrings(false);
                item.add(label);
            }

        });

        return isOAuthOnly;
    }
}
