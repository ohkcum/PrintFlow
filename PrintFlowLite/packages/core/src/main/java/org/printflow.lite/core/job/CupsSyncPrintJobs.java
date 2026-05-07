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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.concurrent.ReadWriteLockEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.PrintOutDao;
import org.printflow.lite.core.dao.helpers.DaoBatchCommitter;
import org.printflow.lite.core.ipp.IppJobStateEnum;
import org.printflow.lite.core.ipp.client.IppConnectException;
import org.printflow.lite.core.jpa.PrintOut;
import org.printflow.lite.core.print.proxy.JsonProxyPrintJob;
import org.printflow.lite.core.services.IppClientService;
import org.printflow.lite.core.services.ProxyPrintService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.SyncPrintJobsResult;
import org.printflow.lite.core.util.AppLogHelper;
import org.printflow.lite.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes CUPS job state with PrintOut jobs.
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsSyncPrintJobs extends AbstractJob {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CupsSyncPrintJobs.class);

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();
    /** */
    private static final IppClientService IPP_CLIENT_SERVICE =
            ServiceContext.getServiceFactory().getIppClientService();

    /**
     * The max number of {@link PrintOut} instances in list chunk.
     */
    private static final int MAX_RESULT_PRINT_OUT_LIST = 200;

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(false);
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final AdminPublisher publisher = AdminPublisher.instance();

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        final DaoBatchCommitter batchCommitter = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());

        publisher.publish(PubTopicEnum.CUPS, PubLevelEnum.INFO,
                localizeSysMsg("CupsSyncPrintJobs.start"));

        try {
            batchCommitter.lazyOpen();

            final SyncPrintJobsResult syncResult =
                    syncPrintJobs(batchCommitter);

            batchCommitter.close();

            if (syncResult.getJobsActive() > 0) {
                msg = AppLogHelper.logInfo(getClass(),
                        "CupsSyncPrintJobs.success",
                        String.valueOf(syncResult.getJobsActive()),
                        String.valueOf(syncResult.getJobsStateChange()),
                        String.valueOf(syncResult.getJobsIdentical()),
                        String.valueOf(syncResult.getJobsForcedCancel()),
                        String.valueOf(syncResult.getJobsNotFound()));
            } else {
                msg = localizeSysMsg("CupsSyncPrintJobs.success",
                        String.valueOf(syncResult.getJobsActive()),
                        String.valueOf(syncResult.getJobsStateChange()),
                        String.valueOf(syncResult.getJobsIdentical()),
                        String.valueOf(syncResult.getJobsForcedCancel()),
                        String.valueOf(syncResult.getJobsNotFound()));
            }

        } catch (Exception e) {

            batchCommitter.rollback();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            level = PubLevelEnum.ERROR;
            msg = AppLogHelper.logError(getClass(), "CupsSyncPrintJobs.error",
                    e.getMessage());
        }

        if (msg != null) {
            publisher.publish(PubTopicEnum.CUPS, level, msg);
        }
    }

    /**
     * Synchronizes (updates) the PrintOut jobs with the CUPS job state (if the
     * state changed). A match is made between printer, job-id and
     * creation-time. If there is no match, i.e. when creation times differs, no
     * update is done.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The {@link SyncPrintJobsResult}.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    private static SyncPrintJobsResult syncPrintJobs(
            final DaoBatchCommitter batchCommitter) throws IppConnectException {

        final PrintOutDao printOutDAO =
                ServiceContext.getDaoContext().getPrintOutDao();

        SpInfo.instance().log(String.format("| Syncing CUPS jobs ..."));

        /*
         * Init batch.
         */
        final long startTime = System.currentTimeMillis();

        final long nActiveCupsJobs = printOutDAO.countActiveCupsJobs(false);

        SpInfo.instance()
                .log(String.format("|   %s : %d Active PrintOut jobs.",
                        DateUtil.formatDuration(
                                System.currentTimeMillis() - startTime),
                        nActiveCupsJobs));

        int nJobsActive = 0;
        int nJobsStateChange = 0;
        int nJobsForceCancel = 0;
        int nJobsNotFound = 0;

        boolean hasNext = true;

        int cupsJobIdLast = 0;

        while (hasNext) {

            final List<PrintOut> list = printOutDAO.getActiveCupsJobsChunk(
                    Integer.valueOf(MAX_RESULT_PRINT_OUT_LIST), false);

            final SyncPrintJobsResult result = syncPrintJobs(printOutDAO,
                    cupsJobIdLast, list, batchCommitter);

            nJobsActive += result.getJobsActive();
            nJobsStateChange += result.getJobsStateChange();
            nJobsForceCancel += result.getJobsForcedCancel();
            nJobsNotFound += result.getJobsNotFound();

            cupsJobIdLast = result.getJobIdLast();

            hasNext = list.size() == MAX_RESULT_PRINT_OUT_LIST
                    && nJobsActive < nActiveCupsJobs;

            batchCommitter.commit(); // !!

            if (result.getJobsActive() > 0) {
                AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                        PubLevelEnum.INFO,
                        String.format(
                                "Print Job Sync %d/%d: "
                                        + "%d changed, %d identical, "
                                        + "%s cancel, %d unknown.",
                                nJobsActive, nActiveCupsJobs,
                                result.getJobsStateChange(),
                                result.getJobsIdentical(),
                                result.getJobsForcedCancel(),
                                result.getJobsNotFound()));
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Synced [{}] active PrintOut jobs: "
                            + "[{}] changed [{}] identical [{}] unknown.",
                    nJobsActive, nJobsStateChange,
                    nJobsActive - nJobsStateChange - nJobsNotFound,
                    nJobsNotFound);
        }

        if (nJobsActive > 0) {
            SpInfo.instance()
                    .log(String.format(
                            "|      : %d PrintOut present in CUPS (changed).",
                            nJobsStateChange));
            SpInfo.instance()
                    .log(String.format(
                            "|      : %d PrintOut present in CUPS (identical).",
                            nJobsActive - nJobsStateChange - nJobsNotFound));
            SpInfo.instance().log(String.format(
                    "|      : %d PrintOut stopped in CUPS (forced to cancel).",
                    nJobsForceCancel));
            SpInfo.instance()
                    .log(String.format(
                            "|      : %d PrintOut missing in CUPS (unknown).",
                            nJobsNotFound));
        }

        return new SyncPrintJobsResult(nJobsActive, nJobsStateChange,
                nJobsForceCancel, nJobsNotFound, cupsJobIdLast);
    }

    /**
     * Creates a look-up map on CUPS job id.
     *
     * @param printOutList
     *            {@link PrintOut} list, sorted by
     *            {@link PrintOut#getCupsJobId()} ascending an
     *            {@link PrintOut#getId()} descending.
     *
     * @return The lookup map.
     */
    private static Map<Integer, List<PrintOut>>
            createJobIdLookup(final List<PrintOut> printOutList) {

        final Map<Integer, List<PrintOut>> lookup = new HashMap<>();

        for (final PrintOut printOut : printOutList) {

            final Integer key = printOut.getCupsJobId();
            final List<PrintOut> list;

            if (lookup.containsKey(key)) {
                list = lookup.get(key);
            } else {
                list = new ArrayList<>();
            }

            list.add(printOut);
            lookup.put(key, list);
        }
        return lookup;
    }

    /**
     *
     * @param printOutDAO
     *            {@link PrintOutDao}.
     * @param cupsJobIdLast
     *            The last CUPS job id handled in previous chunk.
     * @param printOutList
     *            List of jobs ordered by CUPS printer name and job id.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The {@link SyncPrintJobsResult}.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    private static SyncPrintJobsResult syncPrintJobs(
            final PrintOutDao printOutDAO, final int cupsJobIdLast,
            final List<PrintOut> printOutList,
            final DaoBatchCommitter batchCommitter) throws IppConnectException {

        final boolean cancelIfStopped = ConfigManager.instance()
                .isConfigValue(Key.CUPS_IPP_JOBSTATE_CANCEL_IF_STOPPED_ENABLE);

        final int nJobsActive = printOutList.size();

        int nJobsStateChange = 0;
        int nJobsForcedCancel = 0;
        int nJobsNotFound = 0;

        final Map<Integer, List<PrintOut>> lookupPrintOut =
                createJobIdLookup(printOutList);

        for (final Entry<Integer, List<PrintOut>> entry : lookupPrintOut
                .entrySet()) {

            final Integer cupsJobId = entry.getKey();

            boolean firstEntry = true;

            for (final PrintOut printOut : entry.getValue()) {

                if (firstEntry) {

                    firstEntry = false;

                    final JsonProxyPrintJob cupsJob;

                    if (cupsJobIdLast == cupsJobId.intValue()) {
                        cupsJob = null;
                    } else {
                        final String printerName =
                                printOut.getPrinter().getPrinterName();

                        cupsJob = PROXY_PRINT_SERVICE
                                .retrievePrintJob(printerName, cupsJobId);
                    }

                    if (cupsJob != null) {

                        if (cancelIfStopped && cupsJob.getIppJobState()
                                .equals(IppJobStateEnum.IPP_JOB_STOPPED)) {

                            PROXY_PRINT_SERVICE.cancelPrintJob(printOut);

                            LOGGER.warn(
                                    "User [{}] CUPS Job #{} [{}]{} > CANCEL",
                                    printOut.getDocOut().getDocLog().getUser()
                                            .getUserId(),
                                    cupsJob.getJobId(),
                                    cupsJob.getIppJobState()
                                            .uiText(Locale.ENGLISH)
                                            .toUpperCase(),
                                    cupsJob.createStateMsgForLogging());

                            nJobsForcedCancel++;
                        }

                        // State change?
                        if (!printOut.getCupsJobState()
                                .equals(cupsJob.getJobState())) {

                            printOut.setCupsJobState(
                                    cupsJob.getIppJobState().asInteger());
                            printOut.setCupsCompletedTime(
                                    cupsJob.getCompletedTime());

                            printOutDAO.update(printOut);
                            nJobsStateChange++;
                            batchCommitter.increment();
                        }
                        continue;
                    }
                }

                /*
                 * Set job status to UNKNOWN, and set Time Completed to current
                 * time to mark as end-state, so this job won't be selected at
                 * the next sync.
                 */
                printOut.setCupsCompletedTime(
                        IPP_CLIENT_SERVICE.getCupsSystemTime());
                printOut.setCupsJobState(
                        IppJobStateEnum.IPP_JOB_UNKNOWN.asInteger());

                printOutDAO.update(printOut);
                nJobsNotFound++;
                batchCommitter.increment();
            }
        }

        final Integer cupsJobIdLastNew;

        if (printOutList.isEmpty()) {
            cupsJobIdLastNew = 0;
        } else {
            cupsJobIdLastNew =
                    printOutList.get(printOutList.size() - 1).getCupsJobId();
        }

        return new SyncPrintJobsResult(nJobsActive, nJobsStateChange,
                nJobsForcedCancel, nJobsNotFound, cupsJobIdLastNew.intValue());
    }

}
