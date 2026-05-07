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
package org.printflow.lite.core.job;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import javax.mail.MessagingException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.circuitbreaker.CircuitBreaker;
import org.printflow.lite.core.circuitbreaker.CircuitBreakerException;
import org.printflow.lite.core.circuitbreaker.CircuitBreakerOperation;
import org.printflow.lite.core.circuitbreaker.CircuitDamagingException;
import org.printflow.lite.core.circuitbreaker.CircuitNonTrippingException;
import org.printflow.lite.core.circuitbreaker.CircuitTrippingException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.CircuitBreakerEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.print.imap.MailPrintListener;
import org.printflow.lite.core.print.imap.MailPrinter;
import org.printflow.lite.core.util.BigDecimalUtil;
import org.printflow.lite.core.util.DateUtil;
import org.printflow.lite.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class MailPrintListenerJob extends AbstractJob {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailPrintListenerJob.class);

    /**
     * Number of seconds after restarting this job after an exception occurs.
     */
    private static final int RESTART_SECS_AFTER_EXCEPTION = 60;

    /**
     * .
     */
    private MailPrintCircuitOperation circuitOperation = null;

    /**
     * Milliseconds to wait before starting this job again.
     */
    private long millisUntilNextInvocation;

    /**
     * The {@link CircuitBreaker}.
     */
    private CircuitBreaker breaker;

    /**
     *
     * @author Datraverse B.V.
     *
     */
    private static class MailPrintCircuitOperation
            implements CircuitBreakerOperation {

        private final MailPrintListenerJob parentJob;

        private MailPrintListener listener = null;

        /**
         *
         * @param parentJob
         */
        public MailPrintCircuitOperation(final MailPrintListenerJob parentJob) {
            this.parentJob = parentJob;
        }

        @Override
        public Object execute(final CircuitBreaker circuitBreaker) {

            try {
                final ConfigManager cm = ConfigManager.instance();

                final int sessionHeartbeatSecs =
                        cm.getConfigInt(Key.PRINT_IMAP_SESSION_HEARTBEAT_SECS);
                final int sessionDurationSecs =
                        cm.getConfigInt(Key.PRINT_IMAP_SESSION_DURATION_SECS);

                this.listener = new MailPrintListener(cm);

                if (ConfigManager.hasImapPrintOAuthTokenRetriever()) {
                    this.listener.connect(
                            ConfigManager.getImapPrintOAuthTokenRetriever());
                } else {
                    this.listener.connect(
                            cm.getConfigValue(Key.PRINT_IMAP_USER_NAME),
                            cm.getConfigValue(Key.PRINT_IMAP_PASSWORD));
                }

                AdminPublisher.instance().publish(PubTopicEnum.MAILPRINT,
                        PubLevelEnum.INFO,
                        this.parentJob.localizeSysMsg("ImapListener.started"));

                this.listener.processInbox();

                /*
                 * At this point we can inform the breaker we are up and
                 * running.
                 */
                circuitBreaker.closeCircuit();

                /*
                 * Blocking...
                 */
                this.listener.listen(sessionHeartbeatSecs, sessionDurationSecs);

                if (!this.listener.isIdleSupported()) {

                    final String msg =
                            localizedMessage("ImapListener.idle-not-supported",
                                    this.listener.getHost());

                    throw new CircuitDamagingException(msg);
                }

            } catch (MessagingException | IOException e) {

                throw new CircuitTrippingException(e);

            } catch (InterruptedException e) {

                throw new CircuitNonTrippingException(e);

            } catch (CircuitDamagingException e) {

                throw e;

            } catch (Exception t) {

                throw new CircuitDamagingException(t);

            } finally {

                if (this.listener != null) {
                    try {
                        this.listener.disconnect();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
            //
            return null;
        }

        /**
         * Creates localized message.
         *
         * @param messageKey
         *            key
         * @param args
         *            arguments
         * @return localized string
         */
        private String localizedMessage(final String messageKey,
                final String... args) {
            return Messages.getMessage(this.parentJob.getClass(),
                    ConfigManager.getDefaultLocale(), messageKey, args);
        }

        /**
         * @throws InterruptedException
         * @throws MessagingException
         *
         */
        public void onInterrupt()
                throws MessagingException, InterruptedException {
            if (this.listener != null) {
                this.listener.disconnect();
            }

        }
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {

        this.breaker = ConfigManager
                .getCircuitBreaker(CircuitBreakerEnum.MAILPRINT_CONNECTION);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("ImapListener.started"));
        }

    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {

        LOGGER.debug("Interrupted.");

        if (this.circuitOperation != null) {
            try {
                this.circuitOperation.onInterrupt();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        if (!MailPrinter.isOnline()) {
            return;
        }

        try {

            this.circuitOperation = new MailPrintCircuitOperation(this);
            breaker.execute(this.circuitOperation);
            this.millisUntilNextInvocation = 1 * DateUtil.DURATION_MSEC_SECOND;

        } catch (CircuitBreakerException t) {

            this.millisUntilNextInvocation = breaker.getMillisUntilRetry();

        } catch (Exception t) {

            this.millisUntilNextInvocation = RESTART_SECS_AFTER_EXCEPTION
                    * DateUtil.DURATION_MSEC_SECOND;

            AdminPublisher.instance().publish(PubTopicEnum.SMTP,
                    PubLevelEnum.ERROR,
                    localizeSysMsg("ImapListener.error", t.getMessage()));
            LOGGER.error(t.getMessage(), t);
        }
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(localizeLogMsg("ImapListener.stopped"));
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        if (this.isInterrupted() || !MailPrinter.isOnline()
                || !ConfigManager.isMailPrintEnabled()) {

            publisher.publish(PubTopicEnum.MAILPRINT, PubLevelEnum.INFO,
                    localizeSysMsg("ImapListener.stopped"));

        } else if (this.breaker.isCircuitDamaged()) {

            publisher.publish(PubTopicEnum.MAILPRINT, PubLevelEnum.ERROR,
                    localizeSysMsg("ImapListener.stopped"));

        } else {

            final PubLevelEnum pubLevel;
            final String pubMsg;

            if (this.breaker.isCircuitClosed()) {
                pubLevel = PubLevelEnum.INFO;
            } else {
                pubLevel = PubLevelEnum.WARN;
                this.millisUntilNextInvocation =
                        this.breaker.getMillisUntilRetry();
            }

            if (this.millisUntilNextInvocation > DateUtil.DURATION_MSEC_SECOND) {

                try {
                    final double seconds =
                            (double) this.millisUntilNextInvocation
                                    / DateUtil.DURATION_MSEC_SECOND;

                    pubMsg = localizeSysMsg("ImapListener.restart",
                            BigDecimalUtil.localize(BigDecimal.valueOf(seconds),
                                    Locale.getDefault(), false));
                } catch (ParseException e) {
                    throw new SpException(e.getMessage());
                }

            } else {
                pubMsg = localizeSysMsg("ImapListener.stopped");
            }

            publisher.publish(PubTopicEnum.MAILPRINT, pubLevel, pubMsg);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting again after ["
                        + this.millisUntilNextInvocation + "] milliseconds");
            }

            SpJobScheduler.instance().scheduleOneShotMailPrintListener(
                    this.millisUntilNextInvocation);
        }

    }

}
