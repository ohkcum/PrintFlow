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
package org.printflow.lite.core.services.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.IAttrDao;
import org.printflow.lite.core.dao.PrinterDao;
import org.printflow.lite.core.dao.enums.ACLOidEnum;
import org.printflow.lite.core.dao.enums.PrinterAttrEnum;
import org.printflow.lite.core.doc.DocContent;
import org.printflow.lite.core.doc.DocContentToPdfException;
import org.printflow.lite.core.doc.DocContentTypeEnum;
import org.printflow.lite.core.doc.IPdfConverter;
import org.printflow.lite.core.doc.PdfToGrayscale;
import org.printflow.lite.core.doc.PdfToRasterPdf;
import org.printflow.lite.core.doc.PdfToSpoolFile;
import org.printflow.lite.core.doc.SpoolFileTransformer;
import org.printflow.lite.core.doc.store.DocStoreBranchEnum;
import org.printflow.lite.core.doc.store.DocStoreException;
import org.printflow.lite.core.doc.store.DocStoreTypeEnum;
import org.printflow.lite.core.dto.IppMediaCostDto;
import org.printflow.lite.core.dto.IppMediaSourceCostDto;
import org.printflow.lite.core.dto.MediaCostDto;
import org.printflow.lite.core.dto.MediaPageCostDto;
import org.printflow.lite.core.dto.ProxyPrinterCostDto;
import org.printflow.lite.core.dto.ProxyPrinterDto;
import org.printflow.lite.core.dto.ProxyPrinterMediaSourcesDto;
import org.printflow.lite.core.i18n.PrintOutNounEnum;
import org.printflow.lite.core.ipp.IppMediaSizeEnum;
import org.printflow.lite.core.ipp.attribute.AbstractIppDict;
import org.printflow.lite.core.ipp.attribute.IppAttrGroup;
import org.printflow.lite.core.ipp.attribute.IppDictJobDescAttr;
import org.printflow.lite.core.ipp.attribute.IppDictJobTemplateAttr;
import org.printflow.lite.core.ipp.attribute.IppDictOperationAttr;
import org.printflow.lite.core.ipp.attribute.syntax.IppKeyword;
import org.printflow.lite.core.ipp.client.IppConnectException;
import org.printflow.lite.core.ipp.client.IppReqPrintJob;
import org.printflow.lite.core.ipp.helpers.IppOptionMap;
import org.printflow.lite.core.jpa.DocLog;
import org.printflow.lite.core.jpa.PrintOut;
import org.printflow.lite.core.jpa.Printer;
import org.printflow.lite.core.jpa.PrinterAttr;
import org.printflow.lite.core.jpa.PrinterGroup;
import org.printflow.lite.core.jpa.PrinterGroupMember;
import org.printflow.lite.core.json.JsonAbstractBase;
import org.printflow.lite.core.json.JsonPrinterDetail;
import org.printflow.lite.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.printflow.lite.core.json.rpc.JsonRpcError.Code;
import org.printflow.lite.core.json.rpc.JsonRpcMethodError;
import org.printflow.lite.core.json.rpc.JsonRpcMethodResult;
import org.printflow.lite.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.printflow.lite.core.pdf.PdfCreateInfo;
import org.printflow.lite.core.pdf.PdfDocumentFonts;
import org.printflow.lite.core.pdf.PdfPrintCollector;
import org.printflow.lite.core.pdf.PdfResolutionEnum;
import org.printflow.lite.core.print.proxy.AbstractProxyPrintReq;
import org.printflow.lite.core.print.proxy.JsonProxyPrintJob;
import org.printflow.lite.core.print.proxy.JsonProxyPrinter;
import org.printflow.lite.core.print.proxy.JsonProxyPrinterOpt;
import org.printflow.lite.core.print.proxy.JsonProxyPrinterOptChoice;
import org.printflow.lite.core.print.proxy.JsonProxyPrinterOptGroup;
import org.printflow.lite.core.print.proxy.ProxyPrinterOptGroupEnum;
import org.printflow.lite.core.print.proxy.SpoolFileTransformRules;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.helpers.ThirdPartyEnum;
import org.printflow.lite.core.util.BigDecimalUtil;
import org.printflow.lite.core.util.DateUtil;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.core.util.JsonHelper;
import org.printflow.lite.core.util.Messages;
import org.printflow.lite.ext.papercut.PaperCutHelper;
import org.printflow.lite.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintServiceImpl extends AbstractProxyPrintService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintServiceImpl.class);

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /** */
    private static final String CUSTOM_IPP_I18N_RESOURCE_NAME = "ipp-i18n";

    /** Key prefix of IPP option (choice) text. */
    private static final String LOCALIZE_IPP_ATTR_PREFIX = "ipp-attr-";

    /** Key prefix of IPP option choice(s) icon CSS class. */
    private static final String LOCALIZE_IPP_ICON_PREFIX = "ipp-icon-";

    /** */
    @SuppressWarnings("unused")
    private static final String NOTIFY_PULL_METHOD = "ippget";

    /** */
    private static final String URL_PATH_CUPS_PRINTERS = "/printers";

    /** */
    private static final String URL_PATH_CUPS_ADMIN = "/admin";

    /**
     * {@code true} when custom IPP i18n files are present.
     */
    private final boolean hasCustomIppI18n;

    /**
     *
     * @throws MalformedURLException
     */
    public ProxyPrintServiceImpl() {
        super();
        this.hasCustomIppI18n = hasCustomIppI18n();
    }

    /**
     * Initializes the custom IPP i18n.
     *
     * @return {@code true} when custom CUPS i18n files are present.
     */
    private static boolean hasCustomIppI18n() {

        final File[] files =
                ConfigManager.getServerCustomCupsI18nHome().listFiles();

        if (files == null || files.length == 0) {
            return false;
        }

        for (final File file : files) {
            if (FilenameUtils.getExtension(file.getName())
                    .equals(DocContent.FILENAME_EXT_XML)
                    && file.getName()
                            .startsWith(CUSTOM_IPP_I18N_RESOURCE_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the localized IPP key string from custom resource.
     *
     * @param key
     *            The key.
     * @param locale
     *            The locale.
     * @return {@code null} if not found.
     */
    private String localizeCustomIpp(final String key, final Locale locale) {

        if (this.hasCustomIppI18n) {

            final ResourceBundle bundle = Messages.loadXmlResource(
                    ConfigManager.getServerCustomCupsI18nHome(),
                    CUSTOM_IPP_I18N_RESOURCE_NAME, locale);

            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return null;
    }

    @Override
    protected ArrayList<JsonProxyPrinterOptGroup> createCommonCupsOptions() {
        return null;
    }

    /**
     * Gets the CUPS server {@link URL} of the printer {@link URI}. Example:
     * http://192.168.1.35:631
     *
     * @param uriPrinter
     *            The {@link URI} of the printer.
     * @return The CUPS server {@link URL}.
     * @throws MalformedURLException
     *             When the input is malformed.
     */
    private URL getCupsServerUrl(final URI uriPrinter)
            throws MalformedURLException {
        return new URL(InetUtils.URL_PROTOCOL_HTTP, uriPrinter.getHost(),
                uriPrinter.getPort(), "");
    }

    @Override
    public ProxyPrinterOptGroupEnum getUiOptGroup(final String keywordIpp) {

        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_UI_ADVANCED,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.ADVANCED;
        }
        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_UI_JOB,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.JOB;
        }
        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_UI_PAGE_SETUP,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.PAGE_SETUP;
        }
        if (isIppKeywordPresent(IppDictJobTemplateAttr.ATTR_SET_REFERENCE_ONLY,
                keywordIpp)) {
            return ProxyPrinterOptGroupEnum.REFERENCE_ONLY;
        }
        return null;
    }

    /**
     * Checks if a keyword is present in array.
     *
     * @param keywords
     *            The keyword array.
     * @param keywordIpp
     *            The IPP keyword.
     * @return {@code null} when IPP keyword is not present in the array.
     */
    private boolean isIppKeywordPresent(final String[] keywords,
            final String keywordIpp) {
        for (final String keyword : keywords) {
            if (keyword.equals(keywordIpp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void localizePrinterOptChoices(final Locale locale,
            final String attrKeyword,
            final List<JsonProxyPrinterOptChoice> choices) {

        final boolean isMedia = IppDictJobTemplateAttr.isMediaAttr(attrKeyword);
        for (final JsonProxyPrinterOptChoice optChoice : choices) {
            localizePrinterOptChoice(locale, attrKeyword, isMedia, optChoice);
        }
    }

    @Override
    public void localizePrinterOptChoice(final Locale locale,
            final String attrKeyword,
            final JsonProxyPrinterOptChoice optChoice) {

        localizePrinterOptChoice(locale, attrKeyword,
                IppDictJobTemplateAttr.isMediaAttr(attrKeyword), optChoice);
    }

    @Override
    public String localizePrinterOptValue(final Locale locale,
            final String attrKeyword, final String value) {
        return localizePrinterOptChoice(locale, attrKeyword,
                IppDictJobTemplateAttr.isMediaAttr(attrKeyword), value);
    }

    @Override
    public String localizePrinterOpt(final Locale locale,
            final String attrKeyword) {

        final String msgKey =
                String.format("%s%s", LOCALIZE_IPP_ATTR_PREFIX, attrKeyword);

        final String customOpt = localizeCustomIpp(msgKey, locale);

        if (customOpt == null) {
            return localizeWithDefault(locale, msgKey, attrKeyword);
        } else {
            return customOpt;
        }
    }

    @Override
    public void localizePrinterOpt(final Locale locale,
            final JsonProxyPrinterOpt option) {

        final String attrKeyword = option.getKeyword();

        option.setUiText(localizePrinterOpt(locale, attrKeyword));

        localizePrinterOptChoices(locale, attrKeyword, option.getChoices());
    }

    @Override
    public List<Pair<String, String>> getJobTicketOptionsExtUiText(
            final Locale locale, final Map<String, String> optionMap) {

        if (optionMap == null) {
            return null;
        }

        List<Pair<String, String>> list = new ArrayList<>();

        for (final Entry<String, String> entry : optionMap.entrySet()) {

            if (!IppDictJobTemplateAttr.isCustomExtAttr(entry.getKey())
                    || IppDictJobTemplateAttr
                            .isCustomExtAttrValueNone(entry.getValue())) {
                continue;
            }

            if (list == null) {
                list = new ArrayList<>();
            }

            list.add(new ImmutablePair<String, String>(
                    localizePrinterOpt(locale, entry.getKey()),
                    localizePrinterOptValue(locale, entry.getKey(),
                            entry.getValue())));
        }

        return list;
    }

    @Override
    public List<Pair<String, String>> getJobCopyOptionsUiText(
            final Locale locale, final Map<String, String> optionMap) {

        if (optionMap == null) {
            return null;
        }

        List<Pair<String, String>> list = new ArrayList<>();

        for (final Entry<String, String> entry : optionMap.entrySet()) {

            if (!IppDictJobTemplateAttr.isJobCoverAttr(entry.getKey())
                    || IppDictJobTemplateAttr
                            .isJobCoverAttrValueNoCover(entry.getValue())) {
                continue;
            }

            if (list == null) {
                list = new ArrayList<>();
            }

            list.add(
                    new ImmutablePair<String, String>(
                            localizePrinterOpt(locale, entry.getKey()),
                            String.format("(%s)",
                                    localizePrinterOptValue(locale,
                                            entry.getKey(), entry.getValue())
                                                    .toLowerCase())));
        }

        return list;
    }

    @Override
    public String getJobCopyOptionsHtml(final Locale locale,
            final Map<String, String> optionMap) {

        return this.getIppOptionsHtml(
                this.getJobCopyOptionsUiText(locale, optionMap));
    }

    @Override
    public String getJobTicketOptionsExtHtml(final Locale locale,
            final Map<String, String> optionMap) {

        return this.getIppOptionsHtml(
                this.getJobTicketOptionsExtUiText(locale, optionMap));
    }

    /**
     * Returns HTML representation of localized IPP key/value pairs.
     *
     * @param pairs
     *            Localized IPP key/value pairs.
     * @return {@code null} when no options keys found in the map.
     */
    private String getIppOptionsHtml(final List<Pair<String, String>> pairs) {

        if (pairs == null || pairs.isEmpty()) {
            return null;
        }

        final StringBuilder extOpts = new StringBuilder();

        for (final Pair<String, String> pair : pairs) {

            if (extOpts.length() > 0) {
                extOpts.append(" • ");
            }
            extOpts.append(pair.getKey()).append(" ").append(pair.getValue());
        }
        return extOpts.toString();
    }

    @Override
    public String getJobTicketOptionsUiText(final Locale locale,
            final String[] ippOptionKeys, final IppOptionMap optionMap) {

        if (optionMap == null) {
            return null;
        }

        final StringBuilder uiText = new StringBuilder();

        for (final String optKey : ippOptionKeys) {

            final String optValue = optionMap.getOptionValue(optKey);

            if (optValue == null) {
                continue;
            }

            if (IppDictJobTemplateAttr.ORG_PRINTFLOWLITE_ATTR_FINISHINGS_EXT
                    .equals(optKey)
                    && IppKeyword.ORG_PRINTFLOWLITE_ATTR_FINISHINGS_EXTERNAL_NONE
                            .equals(optValue)) {
                continue;
            }
            if (IppDictJobTemplateAttr.ORG_PRINTFLOWLITE_ATTR_COVER_TYPE
                    .equals(optKey)
                    && IppKeyword.ORG_PRINTFLOWLITE_ATTR_COVER_TYPE_NO_COVER
                            .equals(optValue)) {
                continue;
            }
            if (IppDictJobTemplateAttr.ORG_PRINTFLOWLITE_ATTR_COVER_TYPE_COLOR
                    .equals(optKey) && !optionMap.hasCoverType()) {
                continue;
            }

            final JsonProxyPrinterOptChoice choice =
                    new JsonProxyPrinterOptChoice();
            choice.setChoice(optValue);

            this.localizePrinterOptChoice(locale, optKey, choice);

            uiText.append(" ").append(choice.getUiText());

        }
        if (uiText.length() > 0) {
            return uiText.toString().trim();
        }

        return null;
    }

    /**
     *
     * Localizes the text in a printer option choice.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param isMedia
     *            {@code true} when this the "media" attribute.
     * @param optChoice
     *            The {@link JsonProxyPrinterOptChoice} object.
     */
    private void localizePrinterOptChoice(final Locale locale,
            final String attrKeyword, final boolean isMedia,
            final JsonProxyPrinterOptChoice optChoice) {

        optChoice.setUiText(localizePrinterOptChoice(locale, attrKeyword,
                isMedia, optChoice.getChoice()));
    }

    /**
     * Localizes the text in a printer option choice.
     *
     * @param locale
     *            The {@link Locale}.
     * @param attrKeyword
     *            The IPP option keyword.
     * @param isMedia
     *            {@code true} when this the "media" attribute.
     * @param choice
     *            The {@link JsonProxyPrinterOptChoice} object.
     * @return The localized choice text.
     */
    private String localizePrinterOptChoice(final Locale locale,
            final String attrKeyword, final boolean isMedia,
            final String choice) {

        String choiceTextDefault = choice;

        final String attrKeywordWrk;

        if (isMedia) {

            attrKeywordWrk = IppDictJobTemplateAttr.ATTR_MEDIA;

            final IppMediaSizeEnum ippMediaSize = IppMediaSizeEnum.find(choice);

            if (ippMediaSize == null) {
                return choiceTextDefault;
            }

            final MediaSizeName mediaSizeName = ippMediaSize.getMediaSizeName();

            choiceTextDefault = localizeWithDefault(locale,
                    String.format("%s%s-%s", LOCALIZE_IPP_ATTR_PREFIX,
                            attrKeywordWrk, mediaSizeName.toString()),
                    mediaSizeName.toString());
        } else {
            attrKeywordWrk = attrKeyword;
        }

        final String key = String.format("%s%s-%s", LOCALIZE_IPP_ATTR_PREFIX,
                attrKeywordWrk, choice);
        final String customChoice = localizeCustomIpp(key, locale);

        final String finalChoice;

        if (customChoice == null) {
            finalChoice = localizeWithDefault(locale, key, choiceTextDefault);
        } else {
            finalChoice = customChoice;
        }

        return finalChoice;
    }

    @Override
    public String localizeMnemonic(final MediaSizeName mediaSizeName) {
        return localizeWithDefault(ServiceContext.getLocale(),
                String.format("%s%s-%s", LOCALIZE_IPP_ATTR_PREFIX,
                        IppDictJobTemplateAttr.ATTR_MEDIA,
                        mediaSizeName.toString()),
                mediaSizeName.toString());
    }

    @Override
    public void localize(final Locale locale,
            final JsonPrinterDetail printerDetail) {

        for (final JsonProxyPrinterOptGroup optGroup : printerDetail
                .getGroups()) {

            if (optGroup.getGroupId() == ProxyPrinterOptGroupEnum.ADVANCED) {
                optGroup.setUiText(localize(locale, "ipp-cat-advanced"));
            } else if (optGroup.getGroupId() == ProxyPrinterOptGroupEnum.JOB) {
                optGroup.setUiText(localize(locale, "ipp-cat-job"));
            } else if (optGroup
                    .getGroupId() == ProxyPrinterOptGroupEnum.PAGE_SETUP) {
                optGroup.setUiText(localize(locale, "ipp-cat-page-setup"));
            }

            for (final JsonProxyPrinterOpt option : optGroup.getOptions()) {
                localizePrinterOpt(locale, option);
                attachCssIcons(locale, option);
            }
        }
    }

    /**
     * Attaches CSS icon class to each of the printer option choices.
     *
     * @param locale
     *            The locale.
     * @param option
     *            The printer option.
     */
    private void attachCssIcons(final Locale locale,
            final JsonProxyPrinterOpt option) {

        final String cssDefault = localizeCustomIpp(String.format("%s%s",
                LOCALIZE_IPP_ICON_PREFIX, option.getKeyword()), locale);

        for (final JsonProxyPrinterOptChoice choice : option.getChoices()) {

            final String cssChoice =
                    localizeCustomIpp(
                            String.format("%s%s-%s", LOCALIZE_IPP_ICON_PREFIX,
                                    option.getKeyword(), choice.getChoice()),
                            locale);

            if (cssChoice == null) {
                choice.setUiIconClass(cssDefault);
            } else {
                choice.setUiIconClass(cssChoice);
            }
        }
    }

    @Override
    public List<JsonProxyPrintJob> retrievePrintJobs(final String printerName,
            final Set<Integer> jobIds) throws IppConnectException {

        final List<JsonProxyPrintJob> jobs = new ArrayList<>();

        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(printerName);

        if (proxyPrinter != null) {

            final URI uriPrinter = proxyPrinter.getPrinterUri();

            final URL urlCupsServer;

            try {
                urlCupsServer = this.getCupsServerUrl(uriPrinter);
            } catch (MalformedURLException e) {
                throw new IppConnectException(e);
            }

            for (final Integer jobId : jobIds) {
                final JsonProxyPrintJob job =
                        IPP_CLIENT_SERVICE.retrievePrintJobUri(urlCupsServer,
                                uriPrinter, null, jobId);
                if (job != null) {
                    jobs.add(job);
                }
            }

        } else {
            /*
             * Remote printer might not be present when remote CUPS is down, or
             * when connection is refused.
             */
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Proxy printer [" + printerName
                        + "] not found in cache: possibly due "
                        + "to remote CUPS connection problem.");
            }
        }

        return jobs;
    }

    /**
     * Gets the CUPS job-id from job-uri.
     *
     * @param jobUri
     *            The URI. For example: ipp://192.168.1.200:631/jobs/65
     * @return The job id.
     */
    private static String jobIdFromJobUri(final String jobUri) {
        return jobUri.substring(jobUri.lastIndexOf('/') + 1);
    }

    @Override
    public JsonProxyPrintJob sendPdfToPrinter(
            final AbstractProxyPrintReq request,
            final JsonProxyPrinter jsonPrinter, final String user,
            final PdfCreateInfo createInfo) throws IppConnectException {

        final File filePdf = createInfo.getPdfFile();
        final URL urlCupsServer;

        try {
            urlCupsServer = getCupsServerUrl(jsonPrinter.getPrinterUri());
        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage());
        }

        final String jobNameWork;

        if (StringUtils.isBlank(request.getJobName())) {
            jobNameWork =
                    String.format("%s-%s", CommunityDictEnum.PrintFlowLite.getWord(),
                            DateUtil.formattedDateTime(
                                    ServiceContext.getTransactionDate()));
        } else {
            jobNameWork = request.getJobName();
        }
        /*
         * Client-side collate?
         */
        final boolean clientSideCollate;

        if (request.getNumberOfCopies() > 1) {

            if (request.isCollate()) {
                clientSideCollate =
                        BooleanUtils.isFalse(jsonPrinter.getSheetCollated());
            } else {
                clientSideCollate =
                        BooleanUtils.isFalse(jsonPrinter.getSheetUncollated());
            }

            if (!clientSideCollate) {

                final String collateKeyword;

                if (request.isCollate()) {
                    collateKeyword = IppKeyword.SHEET_COLLATE_COLLATED;
                } else {
                    collateKeyword = IppKeyword.SHEET_COLLATE_UNCOLLATED;
                }
                request.getOptionValues().put(
                        IppDictJobTemplateAttr.ATTR_SHEET_COLLATE,
                        collateKeyword);
            }

        } else {
            clientSideCollate = false;
        }

        // Save number of copies.
        final int numberOfCopiesSaved = request.getNumberOfCopies();

        /*
         * Send the IPP Print Job request.
         */

        // In case a special collection step is performed.
        File pdfFileCollected = null;

        // In case a Raw Print spool file is created.
        File spoolFile = null;

        final List<IppAttrGroup> response;

        try {

            final File pdfFileToPrint;

            if (clientSideCollate) {

                pdfFileCollected = new File(String.format("%s-collected",
                        filePdf.getCanonicalPath()));

                final int nTotCollectedPages =
                        PdfPrintCollector.collect(request, request.isCollate(),
                                filePdf, pdfFileCollected);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "Collected PDF [%s] pages [%d], "
                                    + "copies [%d], collate [%s], "
                                    + "n-up [%d], duplex [%s] -> pages [%d]",
                            request.getJobName(), request.getNumberOfPages(),
                            request.getNumberOfCopies(),
                            Boolean.toString(request.isCollate()),
                            request.getNup(),
                            Boolean.toString(request.isDuplex()),
                            nTotCollectedPages));
                }

                pdfFileToPrint = pdfFileCollected;

                // Trick the request so IPP Print Job request sets the number of
                // copies to one (1).
                request.setNumberOfCopies(1);

            } else {
                pdfFileCollected = null;
                pdfFileToPrint = filePdf;
            }

            final IppReqPrintJob ippJobReq =
                    new IppReqPrintJob(request, pdfFileToPrint, jsonPrinter,
                            user, jobNameWork, jobNameWork, createInfo);

            // Raw printing?
            final File fileToPrint;
            if (jsonPrinter.isRawPrinter()) {
                spoolFile = this.createSpoolFile(pdfFileToPrint, jobNameWork,
                        user, request, jsonPrinter);
                fileToPrint = spoolFile;
                LOGGER.info("[{}] Raw Print [{}]", jsonPrinter.getName(),
                        request.getJobName());
            } else {
                spoolFile = null;
                fileToPrint = pdfFileToPrint;
            }
            // -----------------------
            response = IPP_CLIENT_SERVICE.printJob(urlCupsServer, ippJobReq,
                    fileToPrint);

        } catch (IOException | DocContentToPdfException
                | UnavailableException e) {
            throw new SpException(e.getMessage(), e);
        } finally {
            if (pdfFileCollected != null) {
                pdfFileCollected.delete();
            }
            if (spoolFile != null) {
                spoolFile.delete();
            }
        }
        // Restore number of copies.
        request.setNumberOfCopies(numberOfCopiesSaved);

        /*
         * Collect the PrintJob data.
         */
        final IppAttrGroup group = response.get(1);

        // The job-uri can be null.
        final String jobUri =
                group.getAttrSingleValue(IppDictOperationAttr.ATTR_JOB_URI);

        // The job-id can be null.
        String jobId =
                group.getAttrSingleValue(IppDictOperationAttr.ATTR_JOB_ID);

        if (jobId == null) {
            if (jobUri == null) {
                throw new SpException("job id could not be determined.");
            }
            jobId = jobIdFromJobUri(jobUri);
        }

        /*
         * Retrieve the JOB status from CUPS.
         *
         * NOTE: if the "media-source" is "manual", the printJob is returned
         * with status "processing".
         */
        final JsonProxyPrintJob printJob =
                IPP_CLIENT_SERVICE.retrievePrintJobUri(urlCupsServer, null,
                        jobUri, Integer.valueOf(jobId, 10));

        // Add extra info to print job state.
        printJob.setUser(user);
        printJob.setTitle(request.getJobName());

        return printJob;
    }

    /**
     * Creates a local spool file to send to a raw printer.
     *
     * @param pdf
     *            PDF to be printed.
     * @param jobName
     * @param userName
     * @param request
     * @param jsonPrinter
     * @return spool file
     * @throws DocContentToPdfException
     * @throws UnavailableException
     */
    private File createSpoolFile(final File pdf, final String jobName,
            final String userName, final AbstractProxyPrintReq request,
            final JsonProxyPrinter jsonPrinter)
            throws DocContentToPdfException, UnavailableException {

        final PdfToSpoolFile.FilterParms parms =
                new PdfToSpoolFile.FilterParms();

        parms.setCopies(request.getNumberOfCopies());
        parms.setTitle(jobName);
        parms.setUserName(userName);

        parms.setPPD(this.getRawPrintPPDFile(jsonPrinter.getRawPrintPPD()));
        parms.setDestinationMimetype(jsonPrinter.getRawPrintMimetype());

        // IPP options with (optional) PPD mapping.
        final Map<String, JsonProxyPrinterOpt> ippPpdOptionMap =
                jsonPrinter.getOptionsLookup();

        for (final Entry<String, String> entry : request.getOptionValues()
                .entrySet()) {

            // Skip custom PrintFlowLite attributes
            if (AbstractIppDict.isCustomAttrCommon(entry.getKey())) {
                continue;
            }

            // PPD mappings
            final JsonProxyPrinterOpt ppdOpt =
                    ippPpdOptionMap.get(entry.getKey());

            final String optKey;
            final String optVal;

            if (ppdOpt == null || ppdOpt.getKeywordPpd() == null) {
                // IPP
                optKey = entry.getKey();
                optVal = entry.getValue();
            } else {
                // PPD
                optKey = ppdOpt.getKeywordPpd();
                optVal = ppdOpt.getChoice(entry.getValue()).getChoicePpd();
            }
            parms.addOption(optKey, optVal);
        }

        // Create spool file
        final File spoolFile =
                new PdfToSpoolFile(parms).convert(DocContentTypeEnum.PDF, pdf);

        // Transform?
        if (StringUtils.isNotBlank(jsonPrinter.getRawPrintTransform())) {

            try {
                new SpoolFileTransformer().transform(spoolFile,
                        this.createSpoolFileTransformRules(jsonPrinter, parms));
            } catch (IOException e) {
                throw new SpException(e.getMessage()); // TODO
            }
        }

        return spoolFile;
    }

    /**
     * Creates {@link {@link SpoolFileTransformRules}}.
     *
     * @param jsonPrinter
     *            {@link JsonProxyPrinter}
     * @param parms
     *            {@link PdfToSpoolFile.FilterParms}
     * @return {@link SpoolFileTransformRules}
     * @throws IOException
     */
    private SpoolFileTransformRules createSpoolFileTransformRules(
            final JsonProxyPrinter jsonPrinter,
            final PdfToSpoolFile.FilterParms parms) throws IOException {

        final File jsonFile = this
                .getRawPrintTransformFile(jsonPrinter.getRawPrintTransform());

        final SpoolFileTransformRules rules =
                JsonHelper.read(SpoolFileTransformRules.class, jsonFile);

        if (rules.getPjl() != null) {

            final SpoolFileTransformRules.PJL pjl = rules.getPjl();

            if (pjl.getPjlHeader() != null) {

                final SpoolFileTransformRules.PJLHeader header =
                        pjl.getPjlHeader();

                if (header.getReplace() != null) {

                    for (final SpoolFileTransformRules.Replace replace : header
                            .getReplace()) {
                        switch (replace.getReplacement()) {
                        case IppDictJobTemplateAttr.ATTR_COPIES:
                            replace.setReplacement(
                                    String.valueOf(parms.getCopies()));
                            break;
                        case IppDictJobDescAttr.ATTR_JOB_NAME:
                            replace.setReplacement(parms.getTitle());
                            break;
                        case IppDictOperationAttr.ATTR_REQUESTING_USER_NAME:
                            replace.setReplacement(parms.getUserName());
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
        }
        return rules;
    }

    @Override
    protected void printPdf(final AbstractProxyPrintReq request,
            final JsonProxyPrinter jsonPrinter, final String user,
            final PdfCreateInfo createInfo, final DocLog docLog)
            throws IppConnectException, DocStoreException {

        final JsonProxyPrintJob printJob =
                this.sendPdfToPrinter(request, jsonPrinter, user, createInfo);

        this.collectPrintOutData(request, docLog, jsonPrinter, printJob,
                createInfo);

        final DocStoreTypeEnum docStoreType;

        if (request.isArchive()) {

            docStoreType = DocStoreTypeEnum.ARCHIVE;

        } else if (!request.isDisableJournal()
                && !jsonPrinter.isJournalDisabled()
                && docStoreService().isEnabled(DocStoreTypeEnum.JOURNAL,
                        DocStoreBranchEnum.OUT_PRINT)
                && accessControlService().hasAccess(docLog.getUser(),
                        ACLOidEnum.U_PRINT_JOURNAL)) {
            docStoreType = DocStoreTypeEnum.JOURNAL;

        } else {
            docStoreType = null;
        }

        if (docStoreType != null) {
            docStoreService().store(docStoreType, request, docLog, createInfo);
        }
    }

    @Override
    public boolean cancelPrintJob(final PrintOut printOut)
            throws IppConnectException {

        final String printerName = printOut.getPrinter().getPrinterName();
        final String requestingUserName =
                printOut.getDocOut().getDocLog().getUser().getUserId();

        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(printerName);

        if (proxyPrinter == null) {
            throw new IllegalStateException(
                    String.format("Printer [%s] not found.", printerName));
        }

        return IPP_CLIENT_SERVICE.cancelPrintJob(proxyPrinter.getPrinterUri(),
                requestingUserName, printOut.getCupsJobId());
    }

    @Override
    public String getCupsApiVersion() {
        return null;
    }

    @Override
    public ProxyPrinterDto getProxyPrinterDto(final Printer printer) {

        final ProxyPrinterDto dto = new ProxyPrinterDto();

        dto.setId(printer.getId());
        dto.setPrinterName(printer.getPrinterName());
        dto.setDisplayName(printer.getDisplayName());
        dto.setLocation(printer.getLocation());
        dto.setDisabled(printer.getDisabled());
        dto.setDeleted(printer.getDeleted());

        final JsonProxyPrinter jsonPrinter =
                this.getCachedPrinter(printer.getPrinterName());

        dto.setPresent(jsonPrinter != null);
        dto.setRawPrinter(jsonPrinter != null && jsonPrinter.isRawPrinter());

        dto.setRawPrintingEnabled(
                jsonPrinter != null && jsonPrinter.isRawPrinter());

        dto.setInternal(printerService().isInternalPrinter(printer.getId()));

        if (docStoreService().isEnabled(DocStoreTypeEnum.ARCHIVE,
                DocStoreBranchEnum.OUT_PRINT)) {
            dto.setArchiveDisabled(printerService().isDocStoreDisabled(
                    DocStoreTypeEnum.ARCHIVE, printer.getId()));
        }
        if (docStoreService().isEnabled(DocStoreTypeEnum.JOURNAL,
                DocStoreBranchEnum.OUT_PRINT)) {
            dto.setJournalDisabled(printerService().isDocStoreDisabled(
                    DocStoreTypeEnum.JOURNAL, printer.getId()));
        }

        if (ConfigManager.instance().isConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_FRONTEND_ENABLE)
                && !PAPERCUT_SERVICE
                        .isExtPaperCutPrint(printer.getPrinterName())) {
            dto.setPapercutFrontEnd(
                    printerService().isPaperCutFrontEnd(printer.getId()));
        }

        dto.setPpdExtFile(printerService().getAttributeValue(printer,
                PrinterAttrEnum.CUSTOM_PPD_EXT_FILE));
        dto.setPpdFile(printerService().getAttributeValue(printer,
                PrinterAttrEnum.RAW_PRINT_PPD_FILE));
        dto.setPdlTransformFile(printerService().getAttributeValue(printer,
                PrinterAttrEnum.RAW_PRINT_TRANSFORM_FILE));

        dto.setJobTicket(printerService().isJobTicketPrinter(printer.getId()));

        dto.setJobTicketGroup(printerService().getAttributeValue(printer,
                PrinterAttrEnum.JOBTICKET_PRINTER_GROUP));

        if (jobTicketService().isJobTicketLabelsEnabled()) {
            dto.setJobTicketLabelsEnabled(
                    printerService().isJobTicketLabelsEnabled(printer));
        }

        /*
         * Printer Groups.
         */
        String printerGroups = null;

        final List<PrinterGroupMember> members =
                printer.getPrinterGroupMembers();

        if (members != null) {
            for (PrinterGroupMember member : members) {
                if (printerGroups == null) {
                    printerGroups = "";
                } else {
                    printerGroups += ",";
                }
                printerGroups += member.getGroup().getDisplayName();
            }
        }

        dto.setPrinterGroups(printerGroups);
        return dto;
    }

    /**
     * Creates, updates or removes a printer boolean attribute in the database.
     *
     * @param printer
     *            The printer.
     * @param attribute
     *            The printer attribute.
     * @param attrValue
     *            The attribute value.
     */
    private void setPrinterAttr(final Printer printer,
            final PrinterAttrEnum attribute, final Boolean attrValue) {

        final boolean boolValue = BooleanUtils.isTrue(attrValue);

        final PrinterAttr printerAttr =
                printerService().getAttribute(printer, attribute);

        if (printerAttr == null) {

            if (boolValue) {

                final PrinterAttr attr = new PrinterAttr();

                attr.setPrinter(printer);
                attr.setName(attribute.getDbName());
                attr.setValue(printerAttrDAO().getDbBooleanValue(boolValue));

                printer.getAttributes().add(attr);

                printerAttrDAO().create(attr);
            }

        } else {

            final boolean currentValue =
                    printerAttrDAO().getBooleanValue(printerAttr);

            if (boolValue != currentValue) {

                if (boolValue) {
                    printerAttr.setValue(
                            printerAttrDAO().getDbBooleanValue(boolValue));
                    printerAttrDAO().update(printerAttr);
                } else {
                    printerService().removeAttribute(printer, attribute);
                    printerAttrDAO().delete(printerAttr);
                }
            }
        }
    }

    /**
     * Creates, updates or removes a printer string attribute in the database.
     *
     * @param printer
     *            The printer.
     * @param attribute
     *            The printer attribute.
     * @param attrValue
     *            The attribute value.
     */
    private void setPrinterAttr(final Printer printer,
            final PrinterAttrEnum attribute, final String attrValue) {

        final String strValue = StringUtils.defaultString(attrValue).trim();

        final PrinterAttr printerAttr =
                printerService().getAttribute(printer, attribute);

        if (printerAttr == null) {

            if (StringUtils.isNotBlank(strValue)) {

                final PrinterAttr attr = new PrinterAttr();

                attr.setPrinter(printer);
                attr.setName(attribute.getDbName());
                attr.setValue(strValue);

                printer.getAttributes().add(attr);

                printerAttrDAO().create(attr);
            }

        } else {

            final String currentValue =
                    StringUtils.defaultString(printerAttr.getValue());

            if (!strValue.equals(currentValue)) {

                if (StringUtils.isBlank(strValue)) {
                    printerService().removeAttribute(printer, attribute);
                    printerAttrDAO().delete(printerAttr);
                } else {
                    printerAttr.setValue(strValue);
                    printerAttrDAO().update(printerAttr);
                }
            }
        }
    }

    @Override
    public void setProxyPrinterProps(final Printer jpaPrinter,
            final ProxyPrinterDto dto) {

        final String requestingUser = ServiceContext.getActor();
        final Date now = ServiceContext.getTransactionDate();

        final boolean isJobTicket = BooleanUtils.isTrue(dto.getJobTicket());

        jpaPrinter.setModifiedBy(requestingUser);
        jpaPrinter.setModifiedDate(now);

        // Mantis #1105
        final String displayNameWrk;
        if (StringUtils.isBlank(dto.getDisplayName())) {
            displayNameWrk = dto.getPrinterName();
        } else {
            displayNameWrk = dto.getDisplayName();
        }
        jpaPrinter.setDisplayName(displayNameWrk);

        jpaPrinter.setDisabled(dto.getDisabled());

        // Deleted?
        final boolean isDeleted = dto.getDeleted();

        if (jpaPrinter.getDeleted() != isDeleted) {

            if (isDeleted) {
                printerService().setLogicalDeleted(jpaPrinter);
            } else {
                printerService().undoLogicalDeleted(jpaPrinter);
            }
        }

        // Location.
        jpaPrinter.setLocation(dto.getLocation());

        //
        setPrinterAttr(jpaPrinter, PrinterAttrEnum.ACCESS_INTERNAL,
                dto.getInternal());

        if (docStoreService().isEnabled(DocStoreTypeEnum.ARCHIVE,
                DocStoreBranchEnum.OUT_PRINT)) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.ARCHIVE_DISABLE,
                    dto.getArchiveDisabled());
        }
        if (docStoreService().isEnabled(DocStoreTypeEnum.JOURNAL,
                DocStoreBranchEnum.OUT_PRINT)) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOURNAL_DISABLE,
                    dto.getJournalDisabled());
        }

        if (ConfigManager.instance().isConfigValue(
                Key.PROXY_PRINT_DELEGATE_PAPERCUT_FRONTEND_ENABLE)) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.PAPERCUT_FRONT_END,
                    dto.getPapercutFrontEnd());
        }

        setPrinterAttr(jpaPrinter, PrinterAttrEnum.CUSTOM_PPD_EXT_FILE,
                dto.getPpdExtFile());

        setPrinterAttr(jpaPrinter, PrinterAttrEnum.RAW_PRINT_PPD_FILE,
                dto.getPpdFile());

        setPrinterAttr(jpaPrinter, PrinterAttrEnum.RAW_PRINT_TRANSFORM_FILE,
                dto.getPdlTransformFile());

        //
        setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOBTICKET_ENABLE,
                dto.getJobTicket());
        setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOBTICKET_PRINTER_GROUP,
                dto.getJobTicketGroup());

        if (jobTicketService().isJobTicketLabelsEnabled()) {
            setPrinterAttr(jpaPrinter, PrinterAttrEnum.JOBTICKET_LABELS_ENABLE,
                    Boolean.valueOf(BooleanUtils.isNotTrue(dto.getJobTicket())
                            && BooleanUtils
                                    .isTrue(dto.getJobTicketLabelsEnabled())));
        }

        /*
         * Printer Groups.
         *
         * (1) Put the entered PrinterGroups into a map for easy lookup.
         */
        final String printerGroups = dto.getPrinterGroups();

        final Map<String, String> printerGroupLookup = new HashMap<>();

        for (final String displayName : StringUtils.split(printerGroups,
                " ,;:")) {
            printerGroupLookup.put(displayName.trim().toLowerCase(),
                    displayName.trim());
        }

        /*
         * (1.1) "job sheet media sources configuration is offered for
         * non-job-ticket printers, that belong to at least one job ticket
         * printer group."
         *
         * NOTE: printer groups are not checked for being tied to a job ticket
         * printer, so clean-up might not be performed, even if it logically
         * should.
         */
        if (isJobTicket || printerGroupLookup.isEmpty()) {

            final PrinterAttr removedAttr = printerService().removeAttribute(
                    jpaPrinter, PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES);
            if (removedAttr != null) {
                printerAttrDAO().delete(removedAttr);
            }
        }

        /*
         * (2) Remove PrinterGroupMembers which are not selected now, and remove
         * entries from the Map if member already exists.
         */
        boolean isGroupMemberChange = false;

        List<PrinterGroupMember> printerGroupMembers =
                jpaPrinter.getPrinterGroupMembers();

        if (printerGroupMembers == null) {
            printerGroupMembers = new ArrayList<>();
            jpaPrinter.setPrinterGroupMembers(printerGroupMembers);
        }

        final Iterator<PrinterGroupMember> iterMembers =
                printerGroupMembers.iterator();

        while (iterMembers.hasNext()) {

            final PrinterGroupMember member = iterMembers.next();

            final String groupName = member.getGroup().getGroupName();

            if (printerGroupLookup.containsKey(groupName)) {
                printerGroupLookup.remove(groupName);
            } else {
                printerGroupMemberDAO().delete(member);
                iterMembers.remove();
                isGroupMemberChange = true;
            }
        }

        /*
         * (3) Lazy add new Groups and GroupMember.
         */
        for (final Entry<String, String> entry : printerGroupLookup
                .entrySet()) {

            final PrinterGroup group = printerGroupDAO().readOrAdd(
                    entry.getKey(), entry.getValue(), requestingUser, now);

            final PrinterGroupMember member = new PrinterGroupMember();

            member.setGroup(group);
            member.setPrinter(jpaPrinter);
            member.setCreatedBy(requestingUser);
            member.setCreatedDate(now);

            printerGroupMembers.add(member);

            isGroupMemberChange = true;
        }

        //
        jpaPrinter.setModifiedDate(now);
        jpaPrinter.setModifiedBy(requestingUser);

        printerDAO().update(jpaPrinter);

        //
        this.updateCachedPrinter(jpaPrinter);

        //
        if (isJobTicket && isGroupMemberChange) {
            jobTicketService().updatePrinterGroupIDs(jpaPrinter);
        }
    }

    /**
     * Gets the media-source option choices for a printer from the printer
     * cache.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return The media-source option choices.
     */
    private List<JsonProxyPrinterOptChoice>
            getMediaSourceChoices(final String printerName) {

        final JsonProxyPrinter proxyPrinter = getCachedPrinter(printerName);

        if (proxyPrinter != null) {
            for (JsonProxyPrinterOptGroup group : proxyPrinter.getGroups()) {
                for (JsonProxyPrinterOpt option : group.getOptions()) {
                    if (option.getKeyword()
                            .equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
                        return option.getChoices();
                    }
                }
            }
        }

        return new ArrayList<JsonProxyPrinterOptChoice>();
    }

    /**
     * Gets the media costs of a Printer from the database.
     *
     * @param printer
     *            The Printer.
     * @return the media costs.
     */
    private Map<String, IppMediaCostDto>
            getCostByIppMediaName(final Printer printer) {

        final Map<String, IppMediaCostDto> map = new HashMap<>();

        if (printer.getAttributes() != null) {

            Iterator<PrinterAttr> iterAttr = printer.getAttributes().iterator();

            while (iterAttr.hasNext()) {

                final PrinterAttr attr = iterAttr.next();

                final String key = attr.getName();

                if (!key.startsWith(
                        PrinterAttrEnum.PFX_COST_MEDIA.getDbName())) {
                    continue;
                }

                final PrinterDao.CostMediaAttr costMediaAttr =
                        PrinterDao.CostMediaAttr.createFromDbKey(key);

                /*
                 * Self-correcting action...
                 */
                if (costMediaAttr == null) {

                    // (1)
                    printerAttrDAO().delete(attr);
                    // (2)
                    iterAttr.remove();

                    //
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Auto-correct: removed invalid attribute ["
                                + key + "] " + "from printer ["
                                + printer.getPrinterName() + "]");
                    }
                    continue;
                }

                final String ippMediaName = costMediaAttr.getIppMediaName();

                if (ippMediaName != null) {

                    IppMediaCostDto dto = new IppMediaCostDto();
                    dto.setMedia(ippMediaName);
                    dto.setActive(Boolean.TRUE);
                    try {
                        dto.setPageCost(JsonAbstractBase
                                .create(MediaCostDto.class, attr.getValue()));
                        map.put(ippMediaName, dto);
                    } catch (SpException e) {
                        LOGGER.error(e.getMessage());
                    }

                } else {
                    LOGGER.error("Printer [" + printer.getPrinterName()
                            + "] : no IPP media name found in key [" + key
                            + "]");
                }
            }
        }

        return map;
    }

    /**
     * Gets the media costs of a Printer from the database.
     *
     * @param printer
     *            The Printer.
     * @return the media costs.
     */
    private Map<String, IppMediaSourceCostDto>
            getIppMediaSources(final Printer printer) {

        final Map<String, IppMediaSourceCostDto> map = new HashMap<>();

        if (printer.getAttributes() != null) {

            Iterator<PrinterAttr> iterAttr = printer.getAttributes().iterator();

            while (iterAttr.hasNext()) {

                final PrinterAttr attr = iterAttr.next();

                final String key = attr.getName();

                if (!key.startsWith(
                        PrinterAttrEnum.PFX_MEDIA_SOURCE.getDbName())) {
                    continue;
                }

                final PrinterDao.MediaSourceAttr mediaSourceAttr =
                        PrinterDao.MediaSourceAttr.createFromDbKey(key);

                /*
                 * Self-correcting action...
                 */
                if (mediaSourceAttr == null) {

                    // (1)
                    printerAttrDAO().delete(attr);
                    // (2)
                    iterAttr.remove();

                    //
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Auto-correct: removed invalid attribute ["
                                + key + "] " + "from printer ["
                                + printer.getPrinterName() + "]");
                    }
                    continue;
                }

                final String ippMediaSourceName =
                        mediaSourceAttr.getIppMediaSourceName();

                if (ippMediaSourceName != null) {

                    final IppMediaSourceCostDto dto;

                    try {
                        dto = JsonAbstractBase.create(
                                IppMediaSourceCostDto.class, attr.getValue());

                        map.put(dto.getSource(), dto);

                    } catch (SpException e) {
                        LOGGER.error(e.getMessage());
                    }

                } else {
                    LOGGER.error("Printer [" + printer.getPrinterName()
                            + "] : no IPP media source found in key [" + key
                            + "]");
                }
            }
        }

        return map;
    }

    @Override
    public List<IppMediaCostDto>
            getProxyPrinterCostMedia(final Printer printer) {

        final List<IppMediaCostDto> list = new ArrayList<>();

        /*
         *
         */
        final Map<String, IppMediaCostDto> databaseMediaCost =
                getCostByIppMediaName(printer);

        /*
         * Lazy create "default" media.
         */
        IppMediaCostDto dto =
                databaseMediaCost.get(IppMediaCostDto.DEFAULT_MEDIA);

        if (dto == null) {

            dto = new IppMediaCostDto();

            dto.setMedia(IppMediaCostDto.DEFAULT_MEDIA);
            dto.setActive(Boolean.TRUE);

            MediaCostDto pageCost = new MediaCostDto();
            dto.setPageCost(pageCost);

            MediaPageCostDto cost = new MediaPageCostDto();
            pageCost.setCostOneSided(cost);
            cost.setCostGrayscale("0.0");
            cost.setCostColor("0.0");

            cost = new MediaPageCostDto();
            pageCost.setCostTwoSided(cost);
            cost.setCostGrayscale("0.0");
            cost.setCostColor("0.0");

        }

        list.add(dto);

        /*
         * The "regular" media choices from the printer.
         */
        for (JsonProxyPrinterOptChoice media : getMediaChoices(
                printer.getPrinterName())) {

            dto = databaseMediaCost.get(media.getChoice());

            if (dto == null) {

                dto = new IppMediaCostDto();

                dto.setMedia(media.getChoice());
                dto.setActive(Boolean.FALSE);

                MediaCostDto pageCost = new MediaCostDto();
                dto.setPageCost(pageCost);

                MediaPageCostDto cost = new MediaPageCostDto();
                pageCost.setCostOneSided(cost);
                cost.setCostGrayscale("0.0");
                cost.setCostColor("0.0");

                cost = new MediaPageCostDto();
                pageCost.setCostTwoSided(cost);
                cost.setCostGrayscale("0.0");
                cost.setCostColor("0.0");
            }

            list.add(dto);
        }

        return list;
    }

    @Override
    public List<IppMediaSourceCostDto>
            getProxyPrinterCostMediaSource(final Printer printer) {

        final List<IppMediaSourceCostDto> list = new ArrayList<>();

        /*
         *
         */
        final Map<String, IppMediaSourceCostDto> databaseMediaSources =
                getIppMediaSources(printer);

        /*
         * The media-source choices from the printer.
         */
        for (final JsonProxyPrinterOptChoice mediaSource : getMediaSourceChoices(
                printer.getPrinterName())) {

            final String choice = mediaSource.getChoice();

            IppMediaSourceCostDto dto = databaseMediaSources.get(choice);

            if (dto == null) {

                dto = new IppMediaSourceCostDto();

                dto.setActive(Boolean.FALSE);
                dto.setDisplay(mediaSource.getUiText());
                dto.setSource(mediaSource.getChoice());

                if (!choice.equals(IppKeyword.MEDIA_SOURCE_AUTO)
                        && !choice.equals(IppKeyword.MEDIA_SOURCE_MANUAL)) {

                    IppMediaSourceCostDto dtoMedia =
                            new IppMediaSourceCostDto();
                    dto = dtoMedia;

                    dto.setActive(Boolean.FALSE);
                    dto.setSource(mediaSource.getChoice());

                    final IppMediaCostDto mediaCost = new IppMediaCostDto();
                    dtoMedia.setMedia(mediaCost);

                    mediaCost.setActive(Boolean.FALSE);
                    mediaCost.setMedia(""); // blank

                    final MediaCostDto pageCost = new MediaCostDto();
                    mediaCost.setPageCost(pageCost);

                    MediaPageCostDto cost;

                    cost = new MediaPageCostDto();
                    pageCost.setCostOneSided(cost);
                    cost.setCostGrayscale("0.0");
                    cost.setCostColor("0.0");

                    cost = new MediaPageCostDto();
                    pageCost.setCostTwoSided(cost);
                    cost.setCostGrayscale("0.0");
                    cost.setCostColor("0.0");
                }
            }

            list.add(dto);
        }

        return list;
    }

    /**
     *
     * @param printer
     * @param defaultCost
     * @return
     * @throws ParseException
     */
    private AbstractJsonRpcMethodResponse setPrinterSimpleCost(
            final Printer printer, final String defaultCost,
            final Locale locale) {

        try {
            printer.setDefaultCost(
                    BigDecimalUtil.parse(defaultCost, locale, false, false));

        } catch (ParseException e) {

            return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS, "",
                    localize(ServiceContext.getLocale(),
                            "msg-printer-cost-error", defaultCost));
        }

        printer.setChargeType(Printer.ChargeType.SIMPLE.toString());

        printer.setModifiedDate(ServiceContext.getTransactionDate());
        printer.setModifiedBy(ServiceContext.getActor());

        printerDAO().update(printer);

        updateCachedPrinter(printer);

        return JsonRpcMethodResult.createOkResult();
    }

    /**
     *
     * @param printer
     * @param dtoList
     * @param locale
     *            The Locale of the cost in the dtoList.
     * @return
     */
    private AbstractJsonRpcMethodResponse setPrinterMediaCost(
            final Printer printer, final List<IppMediaCostDto> dtoList,
            final Locale locale) {

        /*
         * Put into map for easy lookup of objects to handle. Validate along the
         * way.
         *
         * NOTE: processed entries are removed from the map later on.
         */
        final Map<String, IppMediaCostDto> mapCost = new HashMap<>();

        for (final IppMediaCostDto dto : dtoList) {

            final String mediaKey = dto.getMedia();

            if (!dto.isDefault() && IppMediaSizeEnum.find(mediaKey) == null) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Media [" + mediaKey + "] is invalid.");
                }
                continue;
            }

            /*
             * Validate active entries only.
             */

            if (dto.getActive()) {

                final MediaPageCostDto dtoCostOne =
                        dto.getPageCost().getCostOneSided();

                final MediaPageCostDto dtoCostTwo =
                        dto.getPageCost().getCostTwoSided();

                for (final String cost : new String[] {
                        dtoCostOne.getCostColor(),
                        dtoCostOne.getCostGrayscale(),
                        dtoCostTwo.getCostColor(),
                        dtoCostTwo.getCostGrayscale() }) {

                    if (!BigDecimalUtil.isValid(cost, locale, false)) {
                        return JsonRpcMethodError
                                .createBasicError(Code.INVALID_PARAMS, "",
                                        localize(ServiceContext.getLocale(),
                                                "msg-printer-cost-error",
                                                cost));
                    }
                }
            }

            mapCost.put(mediaKey, dto);
        }

        /*
         * Lazy create of attribute list.
         */
        if (printer.getAttributes() == null) {
            printer.setAttributes(new ArrayList<PrinterAttr>());
        }

        /*
         * Look for existing PrinterAttr objects to update or delete.
         */
        final Iterator<PrinterAttr> iterAttr =
                printer.getAttributes().iterator();

        while (iterAttr.hasNext()) {

            final PrinterAttr costAttr = iterAttr.next();

            final PrinterDao.CostMediaAttr costMediaAttr =
                    PrinterDao.CostMediaAttr
                            .createFromDbKey(costAttr.getName());

            if (costMediaAttr == null) {
                continue;
            }

            final String mediaKey = costMediaAttr.getIppMediaName();
            final IppMediaCostDto costDto = mapCost.get(mediaKey);

            if (costDto != null && costDto.getActive()) {
                /*
                 * Update active entry.
                 */
                String json;

                try {
                    json = costDto.getPageCost().stringify(locale);
                } catch (ParseException | IOException e) {
                    throw new SpException(e);
                }

                costAttr.setValue(json);

            } else {
                /*
                 * Remove non-active entry.
                 */
                // (1)
                printerAttrDAO().delete(costAttr);
                // (2)
                iterAttr.remove();
            }

            /*
             * Handled, so remove from the map.
             */
            mapCost.remove(mediaKey);
        }

        /*
         * Add the active entries which are left in the map.
         */
        for (final IppMediaCostDto costDto : mapCost.values()) {

            if (costDto.getActive()) {
                /*
                 * Add active entry.
                 */
                final PrinterAttr costAttr = new PrinterAttr();

                costAttr.setPrinter(printer);
                costAttr.setName(
                        new PrinterDao.CostMediaAttr(costDto.getMedia())
                                .getKey());

                String json;

                try {
                    json = costDto.getPageCost().stringify(locale);
                } catch (ParseException | IOException e) {
                    throw new SpException(e);
                }

                costAttr.setValue(json);

                // (1)
                printerAttrDAO().create(costAttr);
                // (2)
                printer.getAttributes().add(costAttr);

            } else {
                /*
                 * Non-active entry + no attribute found earlier on: no code
                 * intended.
                 */
            }

        }

        /*
         * Update the printer.
         */
        printer.setChargeType(Printer.ChargeType.MEDIA.toString());

        printer.setModifiedDate(ServiceContext.getTransactionDate());
        printer.setModifiedBy(ServiceContext.getActor());

        printerDAO().update(printer);

        this.updateCachedPrinter(printer);

        /*
         * We are ok.
         */
        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public AbstractJsonRpcMethodResponse setProxyPrinterCostMedia(
            final Printer printer, final ProxyPrinterCostDto dto) {

        final Locale locale =
                new Locale.Builder().setLanguageTag(dto.getLanguage())
                        .setRegion(dto.getCountry()).build();

        if (dto.getChargeType() == Printer.ChargeType.SIMPLE) {
            return setPrinterSimpleCost(printer, dto.getDefaultCost(), locale);
        } else {
            return setPrinterMediaCost(printer, dto.getMediaCost(), locale);
        }
    }

    @Override
    public AbstractJsonRpcMethodResponse setProxyPrinterCostMediaSources(
            final Printer printer,
            final ProxyPrinterMediaSourcesDto dtoMediaSources) {

        final Locale locale = new Locale.Builder()
                .setLanguageTag(dtoMediaSources.getLanguage())
                .setRegion(dtoMediaSources.getCountry()).build();

        /*
         * Put into map for easy lookup of objects to handle. Validate along the
         * way.
         *
         * NOTE: processed entries are removed from the map later on.
         */
        final Map<String, IppMediaSourceCostDto> mapMediaSources =
                new HashMap<>();

        for (final IppMediaSourceCostDto dto : dtoMediaSources.getSources()) {

            final String mediaSourceKey = dto.getSource();

            /*
             * VALIDATE active entries only.
             */
            if (dto.getActive()) {

                final IppMediaCostDto dtoMediaCost = dto.getMedia();

                if (IppMediaSizeEnum.find(dtoMediaCost.getMedia()) == null) {
                    return JsonRpcMethodError.createBasicError(
                            Code.INVALID_PARAMS, "",
                            localize(ServiceContext.getLocale(),
                                    "msg-printer-media-error",
                                    dtoMediaCost.getMedia(), dto.getDisplay()));
                }

                final MediaPageCostDto dtoCostOne =
                        dtoMediaCost.getPageCost().getCostOneSided();

                final MediaPageCostDto dtoCostTwo =
                        dtoMediaCost.getPageCost().getCostTwoSided();

                for (final String cost : new String[] {
                        dtoCostOne.getCostColor(),
                        dtoCostOne.getCostGrayscale(),
                        dtoCostTwo.getCostColor(),
                        dtoCostTwo.getCostGrayscale() }) {

                    if (!BigDecimalUtil.isValid(cost, locale, false)) {
                        return JsonRpcMethodError
                                .createBasicError(Code.INVALID_PARAMS, "",
                                        localize(ServiceContext.getLocale(),
                                                "msg-printer-cost-error",
                                                cost));
                    }
                }
            }

            mapMediaSources.put(mediaSourceKey, dto);
        }

        /*
         * Add Auto, Manual to the map
         */
        for (final IppMediaSourceCostDto miscSource : new IppMediaSourceCostDto[] {
                dtoMediaSources.getSourceAuto(),
                dtoMediaSources.getSourceManual() }) {

            if (miscSource != null) {
                mapMediaSources.put(miscSource.getSource(), miscSource);
            }
        }

        /*
         * Lazy create of attribute list.
         */
        if (printer.getAttributes() == null) {
            printer.setAttributes(new ArrayList<PrinterAttr>());
        }

        /*
         *
         */
        final Boolean isForceDefaultMonochrome =
                dtoMediaSources.getDefaultMonochrome();

        String attrPrintColorModeDefault = PrinterDao.IppKeywordAttr
                .getKey(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE_DFLT);

        /*
         * Look for existing PrinterAttr objects to update or delete.
         */
        final Iterator<PrinterAttr> iterAttr =
                printer.getAttributes().iterator();

        Boolean clientSideMonochrome =
                dtoMediaSources.getClientSideMonochrome();

        //
        final Set<String> jobSheetsMediaSources =
                dtoMediaSources.getJobSheetsMediaSources();

        String jsonJobSheetsMediaSources = null;

        if (jobSheetsMediaSources != null && !jobSheetsMediaSources.isEmpty()) {
            /*
             * INVARIANT: job-sheet media-sources must match active
             * media-source.
             */
            for (final String mediaSource : jobSheetsMediaSources) {
                final IppMediaSourceCostDto dtoWlk =
                        mapMediaSources.get(mediaSource);
                if (dtoWlk != null && BooleanUtils.isTrue(dtoWlk.getActive())) {
                    continue;
                }
                return JsonRpcMethodError.createBasicError(Code.INVALID_PARAMS,
                        "",
                        localize(ServiceContext.getLocale(),
                                "msg-printer-job-sheet-media-source-disabled",
                                PrintOutNounEnum.JOB_SHEET.uiText(
                                        ServiceContext.getLocale(), true),
                                mediaSource));
            }
            jsonJobSheetsMediaSources =
                    JsonHelper.stringifyStringSet(jobSheetsMediaSources);
        } else {
            jsonJobSheetsMediaSources = null;
        }

        //
        while (iterAttr.hasNext()) {

            final PrinterAttr printerAttr = iterAttr.next();

            /*
             * JobSheetsMediaSource?
             */
            if (printerAttr.getName().equalsIgnoreCase(
                    PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES.getDbName())) {

                if (StringUtils.isNotBlank(jsonJobSheetsMediaSources)) {

                    printerAttr.setValue(jsonJobSheetsMediaSources);
                    jsonJobSheetsMediaSources = null;

                } else {
                    /*
                     * Remove non-active entry.
                     */
                    printerAttrDAO().delete(printerAttr);
                    iterAttr.remove();
                }
                continue;
            }

            /*
             * Client-side grayscale conversion?
             */
            if (printerAttr.getName().equalsIgnoreCase(
                    PrinterAttrEnum.CLIENT_SIDE_MONOCHROME.getDbName())) {

                if (clientSideMonochrome != null
                        && clientSideMonochrome.booleanValue()) {

                    printerAttr.setValue(IAttrDao.V_YES);
                    clientSideMonochrome = null;

                } else {
                    /*
                     * Remove non-active entry.
                     */
                    printerAttrDAO().delete(printerAttr);
                    iterAttr.remove();
                }
                continue;
            }

            /*
             * IppKeywordAttr: force monochrome default (update/delete).
             */
            if (attrPrintColorModeDefault != null && attrPrintColorModeDefault
                    .equalsIgnoreCase(printerAttr.getName())) {

                if (isForceDefaultMonochrome != null
                        && isForceDefaultMonochrome) {

                    printerAttr
                            .setValue(IppKeyword.PRINT_COLOR_MODE_MONOCHROME);

                } else {
                    /*
                     * Remove non-active entry.
                     */
                    printerAttrDAO().delete(printerAttr);
                    iterAttr.remove();
                }

                /*
                 * This is a one-shot setting: prevent handling again by setting
                 * to null.
                 */
                attrPrintColorModeDefault = null;
                continue;
            }

            /*
             * MediaSourceAttr
             */
            final PrinterDao.MediaSourceAttr mediaSourceAttr =
                    PrinterDao.MediaSourceAttr
                            .createFromDbKey(printerAttr.getName());

            if (mediaSourceAttr == null) {
                continue;
            }

            final String mediaSourceKey =
                    mediaSourceAttr.getIppMediaSourceName();

            final IppMediaSourceCostDto mediaSourceDto =
                    mapMediaSources.get(mediaSourceKey);

            if (mediaSourceDto != null && mediaSourceDto.getActive()) {
                /*
                 * Update active entry.
                 */
                String json;

                try {
                    mediaSourceDto.toDatabaseObject(locale);
                    json = mediaSourceDto.stringify();
                } catch (IOException e) {
                    throw new SpException(e);
                }

                printerAttr.setValue(json);

            } else {
                /*
                 * Remove non-active entry.
                 */
                printerAttrDAO().delete(printerAttr);
                iterAttr.remove();
            }

            /*
             * Handled, so remove from the map.
             */
            mapMediaSources.remove(mediaSourceKey);
        }

        /*
         * Add the active entries which are left in the map.
         */
        for (final IppMediaSourceCostDto mediaSourceDto : mapMediaSources
                .values()) {

            if (mediaSourceDto.getActive()) {
                /*
                 * Add active entry.
                 */
                mediaSourceDto.toDatabaseObject(locale);

                try {
                    createAddPrinterAttr(printer,
                            new PrinterDao.MediaSourceAttr(
                                    mediaSourceDto.getSource()).getKey(),
                            mediaSourceDto.stringify());
                } catch (IOException e) {
                    throw new SpException(e);
                }

            } else {
                /*
                 * Non-active entry + no attribute found earlier on: no code
                 * intended.
                 */
            }
        }

        /*
         * IppKeywordAttr: force monochrome default (add).
         */
        if (attrPrintColorModeDefault != null
                && isForceDefaultMonochrome != null
                && isForceDefaultMonochrome) {

            final String nameKey = new PrinterDao.IppKeywordAttr(
                    IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE_DFLT).getKey();

            createAddPrinterAttr(printer, nameKey,
                    IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
        }

        /*
         * Client-side grayscale conversion (add).
         */
        if (clientSideMonochrome != null
                && clientSideMonochrome.booleanValue()) {
            createAddPrinterAttr(printer,
                    PrinterAttrEnum.CLIENT_SIDE_MONOCHROME, IAttrDao.V_YES);
        }

        /*
         * Client-side grayscale conversion (add).
         */
        if (StringUtils.isNotBlank(jsonJobSheetsMediaSources)) {
            createAddPrinterAttr(printer,
                    PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES,
                    jsonJobSheetsMediaSources);
        }

        /*
         * Update the printer.
         */
        printer.setModifiedDate(ServiceContext.getTransactionDate());
        printer.setModifiedBy(ServiceContext.getActor());

        printerDAO().update(printer);

        updateCachedPrinter(printer);

        /*
         * We are OK.
         */
        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Creates a {@link PrinterAttr} in database and adds it to printer
     * attribute list.
     *
     * @param printer
     *            The printer.
     * @param attrEnum
     *            The attribute enum.
     * @param value
     *            The attribute value.
     */
    private static void createAddPrinterAttr(final Printer printer,
            final PrinterAttrEnum attrEnum, final String value) {
        createAddPrinterAttr(printer, attrEnum.getDbName(), value);
    }

    /**
     * Creates a {@link PrinterAttr} in database and adds it to printer
     * attribute list.
     *
     * @param printer
     *            The printer.
     * @param name
     *            The attribute name (key).
     * @param value
     *            The attribute value.
     */
    private static void createAddPrinterAttr(final Printer printer,
            final String name, final String value) {

        final PrinterAttr printerAttr = new PrinterAttr();

        printerAttr.setPrinter(printer);
        printerAttr.setName(name);
        printerAttr.setValue(value);

        // (1)
        printerAttrDAO().create(printerAttr);
        // (2)
        printer.getAttributes().add(printerAttr);
    }

    /**
     * Gets the CUPS URL for a printer.
     *
     * @param path
     *            The path.
     * @return The URL.
     * @throws UnknownHostException
     *             If host unknown.
     */
    private URL getCupsUrl(final String path) throws UnknownHostException {
        return this.getCupsUrl(InetUtils.URL_PROTOCOL_HTTPS,
                InetUtils.getServerHostAddress(), path);
    }

    /**
     * Gets the CUPS URL for a printer.
     *
     * @param protocol
     *            The URL protocol.
     * @param host
     *            Host name or IP address.
     * @param path
     *            The path.
     * @return The URL.
     */
    private URL getCupsUrl(final String protocol, final String host,
            final String path) {

        final URL url;

        try {
            url = new URL(protocol, host,
                    Integer.parseInt(ConfigManager.getCupsPort()), path);

        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage(), e);
        }

        return url;
    }

    @Override
    public URL getCupsPrinterUrl(final String printerName) {
        try {
            return this.getCupsPrinterUrl(InetUtils.URL_PROTOCOL_HTTPS,
                    InetUtils.getServerHostAddress(), printerName);
        } catch (UnknownHostException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    @Override
    public URI getCupsPrinterURI(final String printerName) {
        final JsonProxyPrinter proxyPrinter =
                this.getCachedPrinter(printerName);
        if (proxyPrinter == null) {
            return null;
        }
        return proxyPrinter.getDeviceUri();
    }

    /**
     *
     * @param protocol
     *            The URL protocol.
     * @param host
     *            Host name or IP address.
     * @param printerName
     *            CUPS Printer name.
     * @return The URL.
     * @throws UnknownHostException
     *             If host unknown.
     */
    private URL getCupsPrinterUrl(final String protocol, final String host,
            final String printerName) throws UnknownHostException {
        return getCupsUrl(protocol, host,
                URL_PATH_CUPS_PRINTERS.concat("/").concat(printerName));
    }

    @Override
    public URL getCupsAdminUrl() {
        try {
            return getCupsUrl(URL_PATH_CUPS_ADMIN);
        } catch (UnknownHostException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    public ThirdPartyEnum getExtPrinterManager(final String cupsPrinterName) {

        final JsonProxyPrinter cupsPrinter =
                this.getCachedPrinter(cupsPrinterName);

        if (cupsPrinter != null) {

            final URI deviceUri = cupsPrinter.getDeviceUri();

            if (deviceUri != null
                    && PaperCutHelper.isPaperCutPrinter(deviceUri)) {
                return ThirdPartyEnum.PAPERCUT;
            }
        }
        return null;
    }

    @Override
    public boolean isPrePrintGrayscaleJob(final Printer printer,
            final boolean isGrayscaleJob) {

        return this.isColorPrinter(printer.getPrinterName()) && isGrayscaleJob
                && printerService().isClientSideMonochrome(printer);
    }

    @Override
    public IPdfConverter getPrePrintConverter(final OutboxJobDto dto,
            final Printer printer, final File pdfToPrint) throws IOException {

        final boolean toGrayscalePrinterCfg =
                this.isPrePrintGrayscaleJob(printer, dto.isMonochromeJob());

        return this.getPrePrintConverter(toGrayscalePrinterCfg,
                dto.isMonochromeJob(), pdfToPrint,
                dto.getPdfLength().longValue());
    }

    @Override
    public IPdfConverter getPrePrintConverter(
            final boolean toGrayscalePrinterCfg, final boolean isMonochromeJob,
            final File pdfToPrint) throws IOException {

        return this.getPrePrintConverter(toGrayscalePrinterCfg, isMonochromeJob,
                pdfToPrint, pdfToPrint.length());
    }

    /**
     * Checks if a convert action is needed on the PDF before it is proxy
     * printed. If so, it returns the {@link IPdfConverter}. If not it returns
     * {@code null}.
     *
     * @param toGrayscalePrinterCfg
     *            If {@code true}, PDF is to be converted to grayscale due to
     *            printer configuration.
     * @param isMonochromeJob
     *            If {@code true}, PDF is used in a monochrome print job.
     * @param pdfToPrint
     *            PDF to print.
     * @param pdfLengthBytes
     *            Size of PDF file in bytes.
     * @return {@link IPdfConverter} instance or {@code null} if no convert
     *         action is needed.
     * @throws IOException
     *             If File IO error.
     */
    private IPdfConverter getPrePrintConverter(
            final boolean toGrayscalePrinterCfg, final boolean isMonochromeJob,
            final File pdfToPrint, final long pdfLengthBytes)
            throws IOException {

        final ConfigManager cm = ConfigManager.instance();

        final boolean toGrayscale;
        final boolean toRasterized;

        if (cm.isConfigValue(Key.PROXY_PRINT_CONVERT)) {

            toGrayscale = toGrayscalePrinterCfg
                    || cm.isConfigValue(Key.PROXY_PRINT_CONVERT_GRAYSCALE);

            final boolean rasterizeByGlobalConfig =
                    cm.isConfigValue(Key.PROXY_PRINT_CONVERT_RASTERIZE);

            if (rasterizeByGlobalConfig) {

                final PdfDocumentFonts fontInfo =
                        PdfDocumentFonts.create(pdfToPrint);

                final long maxBytes = cm.getConfigLong(
                        Key.PROXY_PRINT_CONVERT_RASTERIZE_MAX_BYTES);
                final long maxPages = cm.getConfigLong(
                        Key.PROXY_PRINT_CONVERT_RASTERIZE_MAX_PAGES);

                final boolean tooManyBytes =
                        maxBytes > 0 && pdfLengthBytes > maxBytes;
                final boolean tooManyPages =
                        maxPages > 0 && fontInfo.getNumberOfPages() > maxPages;

                final boolean maxExceeded = tooManyBytes || tooManyPages;

                if (maxExceeded) {
                    toRasterized = false;
                } else if (cm.isConfigValue(
                        Key.PROXY_PRINT_CONVERT_RASTERIZE_ZERO_FONTS)) {
                    // Rasterize even if fonts are absent.
                    toRasterized = true;
                } else {
                    // Only rasterize if fonts are present.
                    toRasterized = fontInfo.hasFonts();
                }
            } else {
                toRasterized = false;
            }

        } else {
            toGrayscale = toGrayscalePrinterCfg;
            toRasterized = false;
        }

        final IPdfConverter pdfConverter;

        if (toGrayscale || toRasterized) {

            final File tempDir = new File(ConfigManager.getAppTmpDir());

            if (toRasterized) {
                // Rasterize + Grayscale
                final PdfToRasterPdf.Raster raster;

                if (toGrayscale || isMonochromeJob) {
                    raster = PdfToRasterPdf.Raster.GRAYSCALE;
                } else {
                    raster = PdfToRasterPdf.Raster.CMYK;
                }
                pdfConverter = new PdfToRasterPdf(tempDir, raster,
                        cm.getConfigEnum(PdfResolutionEnum.class,
                                Key.PROXY_PRINT_CONVERT_RASTERIZE_DPI));
            } else {
                // Grayscale
                pdfConverter = new PdfToGrayscale(tempDir);
            }

        } else {
            pdfConverter = null;
        }

        return pdfConverter;
    }

}
