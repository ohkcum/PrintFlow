/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.printflow.lite.core.services.impl;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.printflow.lite.core.community.MemberCard;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.config.SslCertInfo;
import org.printflow.lite.core.dao.PrinterDao;
import org.printflow.lite.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.printflow.lite.core.dto.UserHomeStatsDto;
import org.printflow.lite.core.jpa.Printer;
import org.printflow.lite.core.json.JsonRollingTimeSeries;
import org.printflow.lite.core.json.TimeSeriesInterval;
import org.printflow.lite.core.services.AtomFeedService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.PrinterAttrLookup;
import org.printflow.lite.core.services.helpers.feed.AdminAtomFeedWriter;
import org.printflow.lite.core.snmp.SnmpPrinterErrorStateEnum;
import org.printflow.lite.core.snmp.SnmpPrtMarkerColorantValueEnum;
import org.printflow.lite.core.system.SystemInfo;
import org.printflow.lite.core.template.dto.TemplateAdminFeedDto;
import org.printflow.lite.core.template.dto.TemplateAmountTotalDto;
import org.printflow.lite.core.template.dto.TemplateDoSFilterDto;
import org.printflow.lite.core.template.dto.TemplateFinancialTrxDto;
import org.printflow.lite.core.template.dto.TemplatePrintInTotalsDto;
import org.printflow.lite.core.template.dto.TemplatePrinterSnmpDto;
import org.printflow.lite.core.template.dto.TemplateSslCertDto;
import org.printflow.lite.core.template.dto.TemplateUserHomeStatsDto;
import org.printflow.lite.core.template.feed.AdminFeedTemplate;
import org.printflow.lite.core.util.DateUtil;
import org.printflow.lite.core.util.DeadlockedThreadsDetector;
import org.printflow.lite.core.util.JsonHelper;
import org.printflow.lite.lib.feed.AtomFeedWriter;
import org.printflow.lite.lib.feed.FeedEntryDto;
import org.printflow.lite.lib.feed.FeedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AtomFeedServiceImpl extends AbstractService
        implements AtomFeedService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AtomFeedServiceImpl.class);

    /** */
    public static final String FEED_FILE_EXT_JSON = "json";

    /** */
    public static final String FEED_FILE_EXT_XHTML = "xhtml";

    /** */
    public static final String FEED_FILE_BASENAME = "admin";

    /** */
    public static final String FEED_FILE_JSON =
            FEED_FILE_BASENAME + "." + FEED_FILE_EXT_JSON;

    /** */
    public static final String FEED_FILE_XHTML =
            FEED_FILE_BASENAME + "." + FEED_FILE_EXT_XHTML;

    /** */
    private static final long BACKUP_WARN_THRESHOLD_DAYS = 6;

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void refreshAdminFeed() throws FeedException {

        final String feedHome = ConfigManager.getAtomFeedsHome().toString();

        final Path pathXhtml = Paths.get(feedHome, FEED_FILE_XHTML);
        final Path pathJson = Paths.get(feedHome, FEED_FILE_JSON);

        final StringBuilder xhtml = new StringBuilder();
        this.createAdminFeedXhtml(Locale.ENGLISH, xhtml);

        //
        final FeedEntryDto dto = new FeedEntryDto();

        dto.setUuid(UUID.randomUUID());
        dto.setTitle("Metrics");
        dto.setAuthor(ServiceContext.getActor());
        dto.setCategory("statistics");
        dto.setSummary("Daily Data");
        dto.setUpdated(ServiceContext.getTransactionDate());

        try (FileWriter jsonWriter = new FileWriter(pathJson.toFile());) {

            // 1.
            FileUtils.writeStringToFile(pathXhtml.toFile(), xhtml.toString(),
                    Charset.forName("UTF-8"));
            // 2.
            JsonHelper.write(dto, jsonWriter);

        } catch (IOException e) {
            throw new FeedException(e.getMessage());
        }
    }

    /**
     * Gets sum of two rolling totals over the past 2 days.
     *
     * @param configKey1
     *            The type of total #1.
     * @param configKey2
     *            The type of total #2.
     * @return {@null} if no totals found.
     */
    private static Long getRollingTotals(final IConfigProp.Key configKey1,
            final IConfigProp.Key configKey2) {
        final Long total1 = getRollingTotal(configKey1);
        final Long total2 = getRollingTotal(configKey2);
        final Long sum =
                (total1 == null ? 0 : total1) + (total2 == null ? 0 : total2);
        return sum == 0 ? null : sum;
    }

    /**
     * Gets rolling total over the past 2 days.
     *
     * @param configKey
     *            The type of total.
     * @return {@code null} if no total found.
     */
    private static Long getRollingTotal(final IConfigProp.Key configKey) {

        final String jsonSeries =
                ConfigManager.instance().getConfigValue(configKey);

        final JsonRollingTimeSeries<Long> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 2, 0L);

        try {
            data.init(ServiceContext.getTransactionDate(), jsonSeries);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return null;
        }

        Long pages = 0L;

        if (!data.getData().isEmpty()) {

            pages = data.getData().get(0);

            if (data.getData().size() > 1) {
                pages += data.getData().get(1);
            }
        }

        if (pages == 0L) {
            return null;
        }
        return pages;
    }

    /**
     * @param locale
     * @return List of {@link TemplatePrinterSnmpDto} objects.
     */
    private List<TemplatePrinterSnmpDto> getPrintersSnmp(final Locale locale) {

        final PrinterDao.ListFilter filter = new PrinterDao.ListFilter();

        filter.setDeleted(Boolean.FALSE);
        filter.setDisabled(Boolean.FALSE);
        filter.setSnmp(Boolean.TRUE);

        final List<Printer> list = printerDAO().getListChunk(filter, null, null,
                PrinterDao.Field.DISPLAY_NAME, true);

        if (list.isEmpty()) {
            return null;
        }

        final List<TemplatePrinterSnmpDto> printers = new ArrayList<>();

        final Map<String, TemplatePrinterSnmpDto> printerMap = new HashMap<>();

        for (final Printer printer : list) {

            final PrinterAttrLookup attrLookup = new PrinterAttrLookup(printer);
            final Date snmpDate = printerAttrDAO().getSnmpDate(attrLookup);

            final String json = printerAttrDAO().getSnmpJson(attrLookup);
            final ProxyPrinterSnmpInfoDto snmpInfo;

            if (json == null) {
                snmpInfo = null;
            } else {
                snmpInfo = printerService().getSnmpInfo(json);
            }

            final String mapKey;
            if (snmpInfo == null || StringUtils.isBlank(snmpInfo.getModel())
                    || StringUtils.isBlank(snmpInfo.getSerial())) {
                mapKey = printer.getPrinterName();
            } else {
                mapKey = String.format("%s%s", snmpInfo.getModel(),
                        snmpInfo.getSerial());
            }

            final TemplatePrinterSnmpDto wlk;
            final List<String> wlkNames;

            if (printerMap.containsKey(mapKey)) {
                wlk = printerMap.get(mapKey);
                wlkNames = wlk.getNames();
            } else {
                wlk = new TemplatePrinterSnmpDto();
                printerMap.put(mapKey, wlk);
                wlkNames = new ArrayList<>();
                wlk.setNames(wlkNames);
            }

            wlkNames.add(printer.getDisplayName());

            if (wlkNames.size() > 1) {
                continue;
            }

            wlk.setDate(snmpDate);

            final List<String> alerts = new ArrayList<>();

            if (snmpInfo != null) {

                wlk.setModel(snmpInfo.getModel());
                wlk.setSerial(snmpInfo.getSerial());

                final long duration =
                        snmpDate.getTime() - snmpInfo.getDate().getTime();

                if (duration == 0) {
                    if (snmpInfo.getErrorStates() != null) {
                        for (final SnmpPrinterErrorStateEnum error : snmpInfo
                                .getErrorStates()) {
                            alerts.add(error.uiText(locale));
                        }
                    }
                } else {
                    alerts.add(String.format("%s (%s)",
                            SnmpPrinterErrorStateEnum.OFFLINE.uiText(locale),
                            DateUtil.formatDuration(duration)));
                }

                if (snmpInfo.getMarkers() != null
                        && !snmpInfo.getMarkers().isEmpty()) {

                    final List<String> markerNames = new ArrayList<>();
                    final List<Integer> markerPercs = new ArrayList<>();

                    for (final Entry<SnmpPrtMarkerColorantValueEnum, Integer> entry : snmpInfo
                            .getMarkers().entrySet()) {

                        markerNames.add(entry.getKey().uiText(locale));
                        markerPercs.add(entry.getValue());
                    }

                    wlk.setMarkerNames(markerNames);
                    wlk.setMarkerPercs(markerPercs);
                }

            } else {
                alerts.add(SnmpPrinterErrorStateEnum.OFFLINE.uiText(locale));
            }

            if (!alerts.isEmpty()) {
                wlk.setAlerts(alerts);
            }
        }

        printers.addAll(printerMap.values());

        return printers;
    }

    /**
     * @param locale
     *            The locale.
     * @param xhtml
     *            {@link StringBuilder} to append XHTML on.
     */
    private void createAdminFeedXhtml(final Locale locale,
            final StringBuilder xhtml) {

        final TemplateAdminFeedDto dto = new TemplateAdminFeedDto();

        //
        final MemberCard card = MemberCard.instance();

        dto.setMember(card.getMemberOrganization());
        dto.setMembership(card.getStatusUserText(locale));
        dto.setMembershipWarning(card.isMembershipDesirable());
        dto.setParticipants(String.valueOf(card.getMemberParticipants()));
        dto.setDaysTillExpiry(card.getDaysTillExpiry());
        dto.setDaysTillExpiryWarning(card.isDaysTillExpiryWarning());

        dto.setUserCount(String.valueOf(userDAO().count()));
        dto.setActiveUserCount(String.valueOf(userDAO().countActiveUsers()));

        dto.setSystemMode(ConfigManager.getSystemMode().uiText(locale));
        dto.setUptime(DateUtil.formatDuration(SystemInfo.getUptime()));

        final Date now = new Date();
        final Date oneDayAgo = DateUtils.addDays(now, -1);
        final long deadlocks =
                DeadlockedThreadsDetector.getDeadlockedThreadsCount();
        final long errors = appLogService().countErrors(oneDayAgo);
        final long warnings = appLogService().countWarnings(oneDayAgo);

        if (deadlocks != 0) {
            dto.setDeadlockCount(deadlocks);
        }
        if (errors != 0) {
            dto.setErrorCount(errors);
        }
        if (warnings != 0) {
            dto.setWarningCount(warnings);
        }

        final long tickets = jobTicketService().getJobTicketQueueSize();
        if (tickets != 0) {
            dto.setTicketCount(tickets);
        }
        //
        dto.setPagesReceived(
                getRollingTotal(Key.STATS_PRINT_IN_ROLLING_DAY_PAGES));
        dto.setPagesPrinted(
                getRollingTotal(Key.STATS_PRINT_OUT_ROLLING_DAY_PAGES));
        dto.setPagesDownloaded(
                getRollingTotal(Key.STATS_PDF_OUT_ROLLING_DAY_PAGES));

        //
        final TemplatePrintInTotalsDto dtoPDF = new TemplatePrintInTotalsDto();
        dto.setPrintinPDF(dtoPDF);

        dtoPDF.setTotal(getRollingTotal(Key.STATS_PRINT_IN_ROLLING_DAY_PDF));

        dtoPDF.setRepaired(
                getRollingTotals(Key.STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR,
                        Key.STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR_FONT));
        dtoPDF.setRejected(
                getRollingTotals(Key.STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR_FAIL,
                        Key.STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR_FONT_FAIL));

        //
        final TemplateFinancialTrxDto finTrxDto = new TemplateFinancialTrxDto();

        Long countTotal = 0L;
        Long countWlk;
        Long centsWlk;

        countWlk = getRollingTotal(Key.STATS_POS_DEPOSIT_ROLLING_DAY_COUNT);
        centsWlk = getRollingTotal(Key.STATS_POS_DEPOSIT_ROLLING_DAY_CENTS);
        if (countWlk != null && centsWlk != null) {
            finTrxDto.setDeposit(
                    new TemplateAmountTotalDto(countWlk, centsWlk, locale));
            countTotal += countWlk;
        }

        countWlk = getRollingTotal(Key.STATS_POS_PURCHASE_ROLLING_DAY_COUNT);
        centsWlk = getRollingTotal(Key.STATS_POS_PURCHASE_ROLLING_DAY_CENTS);
        if (countWlk != null && centsWlk != null) {
            finTrxDto.setPurchase(
                    new TemplateAmountTotalDto(countWlk, centsWlk, locale));
            countTotal += countWlk;
        }

        countWlk = getRollingTotal(Key.STATS_PAYMENT_GATEWAY_ROLLING_DAY_COUNT);
        centsWlk = getRollingTotal(Key.STATS_PAYMENT_GATEWAY_ROLLING_DAY_CENTS);
        if (countWlk != null && centsWlk != null) {
            finTrxDto.setExternal(
                    new TemplateAmountTotalDto(countWlk, centsWlk, locale));
            countTotal += countWlk;
        }

        if (countTotal > 0L) {
            dto.setFinancialTrx(finTrxDto);
        }

        //
        final ConfigManager cm = ConfigManager.instance();

        if (cm.isConfigValue(Key.PRINTER_SNMP_ENABLE)) {
            dto.setPrintersSnmp(this.getPrintersSnmp(locale));
        }

        //
        final long timeBackup =
                cm.getConfigLong(IConfigProp.Key.SYS_BACKUP_LAST_RUN_TIME);
        if (timeBackup > 0) {
            final long backupDays = ChronoUnit.DAYS.between(
                    new Date(timeBackup).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now());

            dto.setDaysSinceLastBackup(Long.valueOf(backupDays));
            dto.setBackupWarning(backupDays > BACKUP_WARN_THRESHOLD_DAYS);
        }

        //
        final SslCertInfo sslCert = ConfigManager.getSslCertInfoCustom();
        if (sslCert != null && sslCert.isComplete()) {
            dto.setSslCert(TemplateSslCertDto.create(sslCert));
            dto.getSslCert().setNotAfterError(sslCert.isNotAfterWithinDay(now));
            dto.getSslCert()
                    .setNotAfterWarning(sslCert.isNotAfterWithinMonth(now));
        }

        //
        final UserHomeStatsDto userHomeStats =
                UserHomeStatsDto.create(cm.getConfigValue(Key.STATS_USERHOME));

        if (userHomeStats != null) {
            dto.setUserHomeStats(
                    TemplateUserHomeStatsDto.create(userHomeStats));
        }

        //
        if (cm.isConfigValue(Key.SYS_DOSFILTER_ENABLE)) {
            dto.setDoSFilter(TemplateDoSFilterDto
                    .create(ConfigManager.getDoSFilterStatistics()));
        }
        //
        xhtml.append(new AdminFeedTemplate(dto).render(locale));
    }

    @Override
    public AtomFeedWriter getAdminFeedWriter(final URI requestURI,
            final OutputStream ostr) throws FeedException {

        final List<Path> feedEntryFiles = new ArrayList<>();

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {

                final String filePath = file.toString();

                if (!FilenameUtils.getExtension(filePath)
                        .equalsIgnoreCase(FEED_FILE_EXT_JSON)) {
                    return CONTINUE;
                }

                feedEntryFiles.add(file);
                return CONTINUE;
            }
        };

        final Path feedPath = ConfigManager.getAtomFeedsHome();

        if (feedPath.toFile().exists()) {
            try {
                Files.walkFileTree(feedPath, visitor);
            } catch (IOException e) {
                throw new FeedException(e.getMessage());
            }
        } else {
            LOGGER.warn("Directory [{}] does not exist.", feedPath);
        }

        return new AdminAtomFeedWriter(requestURI, ostr, feedEntryFiles);
    }
}
