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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.circuitbreaker.CircuitBreakerException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.config.validator.EmailDomainSetValidator;
import org.printflow.lite.core.dto.UserRegistrationDto;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.printflow.lite.core.json.rpc.ErrorDataBasic;
import org.printflow.lite.core.services.EmailService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.email.EmailMsgParms;
import org.printflow.lite.core.template.dto.TemplateUserRegistrationDto;
import org.printflow.lite.core.template.email.UserRegVerification;
import org.printflow.lite.core.util.EmailValidator;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.lib.pgp.PGPBaseException;
import org.printflow.lite.server.WebApp;
import org.printflow.lite.server.WebAppParmEnum;

/**
 * User Registration request.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserRegistration extends ApiRequestMixin {

    /** */
    private static final EmailService EMAIL_SERVICE =
            ServiceContext.getServiceFactory().getEmailService();

    /**
     * @return request
     */
    private org.eclipse.jetty.server.Request getJettyRequest() {

        final Object reqObj =
                RequestCycle.get().getRequest().getContainerRequest();

        if (reqObj instanceof org.eclipse.jetty.server.Request) {
            return (org.eclipse.jetty.server.Request) reqObj;
        }
        throw new SpException(String.format("Unexpected class %s",
                reqObj.getClass().getName()));
    }

    /**
     * @param uuid
     * @return URL
     */
    private URL getVerificationURL(final UUID uuid) {

        final org.eclipse.jetty.server.Request req = this.getJettyRequest();

        final String scheme;
        String uriAuthority = ConfigManager.instance().getConfigValue(
                Key.INTERNAL_USERS_REGISTRATION_VERIFY_URL_AUTHORITY);

        if (StringUtils.isBlank(uriAuthority)) {
            uriAuthority = req.getHttpURI().getAuthority();
            scheme = req.getScheme();
        } else {
            scheme = InetUtils.URL_PROTOCOL_HTTPS;
        }

        try {
            return new URI(scheme, uriAuthority, WebApp.MOUNT_PATH_WEBAPP_USER,
                    WebAppParmEnum.PFL_VERIFY.parm() + "=" + uuid, null).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserRegistrationDto userDto = UserRegistrationDto
                .create(UserRegistrationDto.class, this.getParmValueDto());

        // Remove whitespace at start/end.
        userDto.trim();

        // INVARIANT: User ID filled
        if (userDto.getUserName().isEmpty()) {
            this.setInvalidMsg(ApiResultCodeEnum.WARN,
                    ReqMessageEnum.USER_REGISTRATION_USERNAME_MISSING);
            return;
        }

        final String userNameComposed = StringUtils
                .defaultString(ConfigManager.instance()
                        .getConfigValue(Key.INTERNAL_USERS_NAME_PREFIX))
                .trim().concat(userDto.getUserName());

        userDto.setUserName(userNameComposed);

        if (!this.validate(userDto)) {
            return;
        }

        final UUID uuid = UUID.randomUUID();
        userDto.setRegistrationUUID(uuid);

        try {
            final AbstractJsonRpcMethodResponse rpcResponse =
                    USER_SERVICE.setUser(userDto, true);

            if (rpcResponse.isResult()) {

                final TemplateUserRegistrationDto templateDto =
                        new TemplateUserRegistrationDto();

                templateDto.setUserName(userDto.getUserName());
                templateDto.setFullName(userDto.getFullName());
                templateDto.setLink(this.getVerificationURL(uuid));

                final UserRegVerification tpl = new UserRegVerification(
                        ConfigManager.getServerCustomTemplateHome(),
                        templateDto);

                final EmailMsgParms emailParms = EmailMsgParms.create(
                        userDto.getEmail(), tpl, this.getLocale(), true);

                EMAIL_SERVICE.sendEmail(emailParms);

                this.setApiResultText(ApiResultCodeEnum.OK,
                        PhraseEnum.MAIL_CHECK_FOR_FURTHER_INSTRUCTIONS
                                .uiText(this.getLocale()));

                AdminPublisher.instance().publish(PubTopicEnum.USER,
                        PubLevelEnum.INFO,
                        String.format(
                                "Registration verification for user [%s] "
                                        + "sent to %s",
                                userDto.getUserName(), userDto.getEmail()));
            } else {
                setApiResultText(ApiResultCodeEnum.ERROR, rpcResponse.asError()
                        .getError().data(ErrorDataBasic.class).getReason());
            }

        } catch (MessagingException | IOException | InterruptedException
                | CircuitBreakerException | PGPBaseException e) {

            String msg = e.getMessage();

            if (e.getCause() != null) {
                msg += " (" + e.getCause().getMessage() + ")";
            }
            setApiResult(ApiResultCodeEnum.ERROR, "msg-single-parm", msg);
        }

    }

    /**
     * @param code
     * @param text
     * @return false
     */
    private boolean setInvalidMsg(final ApiResultCodeEnum code,
            final String text) {
        this.setApiResultText(code, text);
        return false;
    }

    /**
     * @param code
     * @param msgEnum
     * @return false
     */
    private boolean setInvalidMsg(final ApiResultCodeEnum code,
            final ReqMessageEnum msgEnum) {
        this.setApiResult(code, msgEnum);
        return false;
    }

    /**
     * @param userDto
     * @return {@code true} if valid input.
     */
    private boolean validate(final UserRegistrationDto userDto) {

        final ConfigManager cm = ConfigManager.instance();

        // INVARIANT: is IP address allowed?
        if (!ConfigManager.isUserRegistrationAllowed(this.getClientIP())) {
            return this.setInvalidMsg(ApiResultCodeEnum.ERROR,
                    ReqMessageEnum.USER_REGISTRATION_LOCATION_NOT_PERMITTED);
        }

        // INVARIANT: unique User ID
        if (USER_DAO.findActiveUserByUserId(userDto.getUserName()) != null) {
            return this.setInvalidMsg(ApiResultCodeEnum.WARN,
                    ReqMessageEnum.USER_REGISTRATION_USERNAME_TAKEN);
        }

        // INVARIANT: full name
        if (StringUtils.isBlank(userDto.getFullName())) {
            return this.setInvalidMsg(ApiResultCodeEnum.WARN,
                    ReqMessageEnum.USER_REGISTRATION_FULLNAME_MISSING);
        }

        // INVARIANT: syntax email
        if (!EmailValidator.validate(userDto.getEmail())) {
            return this.setInvalidMsg(ApiResultCodeEnum.ERROR,
                    ReqMessageEnum.USER_REGISTRATION_EMAIL_INVALID);
        }

        // INVARIANT: is email domain allowed?
        final Set<String> domainWhitelist =
                EmailDomainSetValidator.getSet(cm.getConfigValue(
                        Key.INTERNAL_USERS_REGISTRATION_EMAIL_DOMAIN_WHITELIST));
        final String mailDomain = userDto.getEmail().split("@")[1];

        if (!domainWhitelist.contains(mailDomain)) {
            return this.setInvalidMsg(ApiResultCodeEnum.ERROR,
                    ReqMessageEnum.USER_REGISTRATION_EMAIL_NOT_PERMITTED);
        }

        // INVARIANT: unique email (no registration pending)
        if (USER_SERVICE.findActiveUserByEmail(userDto.getEmail()) != null) {
            return this.setInvalidMsg(ApiResultCodeEnum.WARN,
                    ReqMessageEnum.USER_REGISTRATION_EMAIL_TAKEN);
        }

        // INVARIANT: password confirmed
        if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
            return this.setInvalidMsg(ApiResultCodeEnum.ERROR,
                    PhraseEnum.PASSWORD_MISMATCH.uiText(getLocale()));
        }

        return true;
    }
}
