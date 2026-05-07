/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.helpers.ProxyPrinterName;
import org.printflow.lite.core.ipp.IppMediaSizeEnum;
import org.printflow.lite.core.ipp.IppPrinterType;
import org.printflow.lite.core.ipp.IppSyntaxException;
import org.printflow.lite.core.ipp.attribute.AbstractIppDict;
import org.printflow.lite.core.ipp.attribute.IppAttr;
import org.printflow.lite.core.ipp.attribute.IppAttrGroup;
import org.printflow.lite.core.ipp.attribute.IppAttrValue;
import org.printflow.lite.core.ipp.attribute.IppDictJobDescAttr;
import org.printflow.lite.core.ipp.attribute.IppDictJobTemplateAttr;
import org.printflow.lite.core.ipp.attribute.IppDictJobTemplateAttr.ApplEnum;
import org.printflow.lite.core.ipp.attribute.IppDictOperationAttr;
import org.printflow.lite.core.ipp.attribute.IppDictPrinterDescAttr;
import org.printflow.lite.core.ipp.attribute.IppDictSubscriptionAttr;
import org.printflow.lite.core.ipp.attribute.syntax.IppBoolean;
import org.printflow.lite.core.ipp.attribute.syntax.IppKeyword;
import org.printflow.lite.core.ipp.attribute.syntax.IppName;
import org.printflow.lite.core.ipp.attribute.syntax.IppUriScheme;
import org.printflow.lite.core.ipp.client.CupsAttrPPD;
import org.printflow.lite.core.ipp.client.CupsAttrPrinter;
import org.printflow.lite.core.ipp.client.IppClient;
import org.printflow.lite.core.ipp.client.IppConnectException;
import org.printflow.lite.core.ipp.client.IppReqCupsGetPpd;
import org.printflow.lite.core.ipp.client.IppReqPrintJob;
import org.printflow.lite.core.ipp.encoding.IppDelimiterTag;
import org.printflow.lite.core.ipp.operation.IppGetPrinterAttrOperation;
import org.printflow.lite.core.ipp.operation.IppOperationId;
import org.printflow.lite.core.ipp.operation.IppStatusCode;
import org.printflow.lite.core.print.cups.CupsCommandLine;
import org.printflow.lite.core.print.proxy.JsonProxyPrintJob;
import org.printflow.lite.core.print.proxy.JsonProxyPrinter;
import org.printflow.lite.core.print.proxy.JsonProxyPrinterOpt;
import org.printflow.lite.core.print.proxy.JsonProxyPrinterOptChoice;
import org.printflow.lite.core.print.proxy.JsonProxyPrinterOptGroup;
import org.printflow.lite.core.print.proxy.ProxyPrintLogger;
import org.printflow.lite.core.print.proxy.ProxyPrinterOptGroupEnum;
import org.printflow.lite.core.services.IppClientService;
import org.printflow.lite.core.services.helpers.CupsPrinterClass;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppClientServiceImpl extends AbstractService
        implements IppClientService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppClientServiceImpl.class);

    /** */
    private static final String URL_PATH_CUPS_PRINTERS = "/printers";

    /** */
    private static final String SUBSCRIPTION_PRINTER_URI =
            IppUriScheme.SCHEME_IPP + "://" + InetUtils.LOCAL_HOST + "/";

    /**
     * A unique ID to distinguish our subscription from other system
     * subscriptions.
     */
    private static final String NOTIFY_USER_DATA =
            "PrintFlowLite:" + ConfigManager.getServerPort();

    /**
     * The {@link URL} of the default (local) CUPS server.
     */
    private URL urlDefaultServer = null;

    /** */
    private final IppClient ippClient = IppClient.instance();

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

    /**
     * @param uriPrinter
     *            printer URI
     * @return {@code true} if local printer.
     */
    private boolean isLocalPrinter(final URI uriPrinter) {
        return uriPrinter.getHost().equals(this.urlDefaultServer.getHost());
    }

    /**
     * Initializes a CUPS printer class from a class member.
     *
     * @param printerClass
     *            The CUPS printer class to initialize.
     * @param printerMember
     *            The CUPS printer class member to initialize from.
     */
    private static void initPrinterClassFromMember(
            final JsonProxyPrinter printerClass,
            final JsonProxyPrinter printerMember) {

        printerClass.setGroups(
                JsonProxyPrinter.createGroupsCopy(printerMember.getGroups()));

        printerClass.setAutoMediaSource(printerMember.getAutoMediaSource());
        printerClass.setColorDevice(printerMember.getColorDevice());
        printerClass.setDuplexDevice(printerMember.getDuplexDevice());
        printerClass.setManualMediaSource(printerMember.getManualMediaSource());
        printerClass.setPpd(printerMember.getPpd());
        printerClass
                .setPpdPresentCupsClassMembers(printerMember.isPpdPresent());
        printerClass.setPpdVersion(printerMember.getPpdVersion());
        printerClass.setSheetCollated(printerMember.getSheetCollated());
        printerClass.setSheetUncollated(printerMember.getSheetUncollated());
    }

    /**
     *
     * @param uriPrinter
     * @return IPP attribute groups.
     */
    private List<IppAttrGroup> reqGetPrinterAttr(final URI uriPrinter) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        IppAttrValue value = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter.toString());

        // ---------
        value = new IppAttrValue(
                dict.getAttr(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES));

        /*
         * Commented code below shows how to retrieve attribute subsets.
         */
        // value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_JOB_TPL);
        // value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_PRINTER_DESC);
        // value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_MEDIA_COL_DATABASE);

        /*
         * We want them all.
         */
        value.addValue(IppGetPrinterAttrOperation.ATTR_GRP_ALL);

        group.addAttribute(value);

        // ---------
        return attrGroups;
    }

    /**
     * UNDER CONSTRUCTION.
     *
     * @param uriPrinter
     * @param attributes
     *            key/value pairs.
     * @return IPP attribute groups.
     */
    private List<IppAttrGroup> reqSetPrinterAttr(final URI uriPrinter,
            final Map<IppAttr, String> attributes) {

        final String requestingUser = ConfigManager.getProcessUserName();

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = this.createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter.toString());

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);

        /*
         * Group 2: Printer Object Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.PRINTER_ATTR);
        attrGroups.add(group);

        for (final Entry<IppAttr, String> attr : attributes.entrySet()) {
            group.add(attr.getKey(), attr.getValue());
        }
        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @param requestingUser
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqGetPrinterSubscriptions(
            final String uriPrinter, final String requestingUser) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();
        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        // ---------
        AbstractIppDict dict = IppDictOperationAttr.instance();
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_MY_SUBSCRIPTIONS),
                IppBoolean.TRUE);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);

        // ---------
        return attrGroups;
    }

    /**
     * Creates an IPP request to renews the printer subscription.
     *
     * @param requestingUser
     *            The requesting user.
     * @param subscriptionId
     *            The subscription id.
     * @param leaseSeconds
     *            The lease seconds.
     *
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqRenewPrinterSubscription(
            final String requestingUser, final String subscriptionId,
            final String leaseSeconds) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                SUBSCRIPTION_PRINTER_URI);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_NOTIFY_SUBSCRIPTION_ID),
                subscriptionId);

        /*
         * Group 2: Subscription Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.SUBSCRIPTION_ATTR);
        attrGroups.add(group);

        dict = IppDictSubscriptionAttr.instance();

        group.add(
                dict.getAttr(
                        IppDictSubscriptionAttr.ATTR_NOTIFY_LEASE_DURATION),
                leaseSeconds);

        // ---------
        return attrGroups;
    }

    /**
     * Creates an IPP request to create the printer subscription.
     * <p>
     * <b>Note</b>: either recipientUri or notifyPullMethod must be specified.
     * </p>
     *
     * @param requestingUser
     *            The requesting user.
     * @param recipientUri
     *            The recipient as as value of
     *            {@link IppDictSubscriptionAttr#ATTR_NOTIFY_RECIPIENT_URI}
     * @param leaseSeconds
     *            The lease seconds.
     *
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqCreatePrinterPushSubscriptions(
            final String requestingUser, final String recipientUri,
            final String leaseSeconds) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                SUBSCRIPTION_PRINTER_URI);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);

        /*
         * Group 2: Subscription Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.SUBSCRIPTION_ATTR);
        attrGroups.add(group);

        dict = IppDictSubscriptionAttr.instance();

        if (recipientUri != null) {
            group.add(
                    dict.getAttr(
                            IppDictSubscriptionAttr.ATTR_NOTIFY_RECIPIENT_URI),
                    recipientUri);
        }

        group.add(dict.getAttr(IppDictSubscriptionAttr.ATTR_NOTIFY_USER_DATA),
                NOTIFY_USER_DATA);

        group.add(
                dict.getAttr(
                        IppDictSubscriptionAttr.ATTR_NOTIFY_LEASE_DURATION),
                leaseSeconds);

        /*
         * group.add(dict.getAttr(IppDictSubscriptionAttr.
         * ATTR_NOTIFY_TIME_INTERVAL ), "5");
         */

        /*
         * Printer and Jobs events to subscribe on ...
         */
        final String[] events = {
                /* */
                "printer-config-changed",
                /* */
                "printer-media-changed",
                /* */
                "printer-queue-order-changed",
                /* */
                "printer-restarted",
                /* */
                "printer-shutdown",
                /* */
                "printer-state-changed",
                /* */
                "printer-stopped",
                /* */
                "job-state-changed",
                /* */
                "job-created",
                /* */
                "job-completed",
                /* */
                "job-stopped",

                /* */
                /* "job-progress" */

                /* CUPS event: printer or class is added */
                "printer-added",
                /* CUPS event: printer or class is deleted */
                "printer-deleted",
                /* CUPS event: printer or class is modified */
                "printer-modified",
                /* CUPS event: security condition occurs */
                "server-audit",
                /* CUPS event: server is restarted */
                "server-restarted",
                /* CUPS event: server is started */
                "server-started",
                /* CUPS event: server is stopped */
                "server-stopped"
                //
        };

        IppAttrValue attrEvents = new IppAttrValue(
                dict.getAttr(IppDictSubscriptionAttr.ATTR_NOTIFY_EVENTS));

        for (String event : events) {
            attrEvents.addValue(event);
        }

        group.addAttribute(attrEvents);

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param requestingUser
     *            The requesting user.
     * @param subscriptionId
     *            The subscription id.
     *
     * @return The IPP request.
     */
    private List<IppAttrGroup> reqCancelPrinterSubscription(
            final String requestingUser, final String subscriptionId) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                SUBSCRIPTION_PRINTER_URI);
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUser);
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_NOTIFY_SUBSCRIPTION_ID),
                subscriptionId);
        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriJob
     * @param reqUser
     * @return
     */
    private List<IppAttrGroup> reqGetJobAttr(final String uriJob) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_URI), uriJob);

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @param jobId
     * @return
     */
    private List<IppAttrGroup> reqGetJobAttr(final URI uriPrinter,
            final Integer jobId) {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter.toString());
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_ID),
                jobId.toString());

        // ---------
        return attrGroups;
    }

    /**
     *
     * @param uriPrinter
     * @param jobId
     * @param requestingUserName
     * @return
     */
    private List<IppAttrGroup> reqCancelJobAttr(final URI uriPrinter,
            final Integer jobId, final String requestingUserName) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;
        AbstractIppDict dict = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        dict = IppDictOperationAttr.instance();

        // ---------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter.toString());
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_JOB_ID),
                jobId.toString());
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTING_USER_NAME),
                requestingUserName);

        // ---------
        return attrGroups;
    }

    /**
     * Creates a {@link JsonProxyPrinter}, with a subset of IPP option
     * (attributes).
     * <p>
     * NOTE: Irrelevant (raw) IPP options from the {@link IppAttrGroup} input
     * parameter are not copied to the output {@link JsonProxyPrinter}.
     * </p>
     *
     * @param group
     *            The (raw) IPP printer options.
     * @return the {@link JsonProxyPrinter} or {@code null} when printer
     *         definition is not valid somehow.
     */
    private JsonProxyPrinter createUserPrinter(final IppAttrGroup group) {

        final JsonProxyPrinter printer = new JsonProxyPrinter();

        /*
         * Mantis #403: Cache CUPS printer-name internally as upper-case.
         */
        printer.setName(ProxyPrinterName.getDaoName(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_PRINTER_NAME)));

        printer.setManufacturer(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO_MANUFACTURER));
        printer.setModelName(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL));

        // Device URI
        final URI deviceUri;

        final String deviceUriValue;

        try {
            deviceUriValue = group
                    .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_DEVICE_URI);

            if (deviceUriValue == null) {
                deviceUri = null;
            } else {
                deviceUri = new URI(deviceUriValue);
            }
        } catch (URISyntaxException e) {
            throw new SpException(e.getMessage());
        }

        printer.setDeviceUri(deviceUri);

        // Printer URI
        final URI printerUri;

        try {
            final String uriValue = group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED);
            if (uriValue == null) {
                // Mantis #585
                LOGGER.warn(String.format(
                        "Skipping printer [%s] model [%s] device URI [%s]"
                                + ": no printer URI",
                        printer.getName(),
                        Objects.toString(printer.getModelName(), ""),
                        Objects.toString(deviceUriValue, "")));
                return null;
            } else {
                printerUri = new URI(uriValue);
            }
        } catch (URISyntaxException e) {
            throw new SpException(e.getMessage());
        }

        printer.setPrinterUri(printerUri);

        //
        printer.setInfo(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_PRINTER_INFO));

        printer.setAcceptingJobs(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS,
                IppBoolean.TRUE).equals(IppBoolean.TRUE));

        printer.setLocation(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION));
        printer.setState(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_PRINTER_STATE));
        printer.setStateChangeTime(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME));
        printer.setStateReasons(group.getAttrSingleValue(
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS));

        printer.setColorDevice(group
                .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED,
                        IppBoolean.FALSE)
                .equals(IppBoolean.TRUE));

        printer.setDuplexDevice(Boolean.FALSE);

        // ----------------
        ArrayList<JsonProxyPrinterOptGroup> printerOptGroups =
                new ArrayList<>();
        printer.setGroups(printerOptGroups);

        // ---------------------
        // Options
        // ---------------------
        this.addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.PAGE_SETUP,
                IppDictJobTemplateAttr.ATTR_SET_UI_PAGE_SETUP);

        this.addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.JOB,
                IppDictJobTemplateAttr.ATTR_SET_UI_JOB);

        this.addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.ADVANCED,
                IppDictJobTemplateAttr.ATTR_SET_UI_ADVANCED);

        this.addUserPrinterOptGroup(printer, group,
                ProxyPrinterOptGroupEnum.REFERENCE_ONLY,
                IppDictJobTemplateAttr.ATTR_SET_REFERENCE_ONLY);

        /*
         * TODO : The PPD values are used to see if a printer "changed", but
         * this becomes obsolete as soon as we utilize event subscription fully.
         *
         * For now make sure PPD values are NOT NULL !!
         *
         * "pcfilename":"HL1250.PPD", "FileVersion":"1.1"
         */
        printer.setPpd("");
        printer.setPpdVersion("");

        return printer;
    }

    /**
     * Creates the first Group with Operation Attributes.
     *
     * @return IPP attribute group.
     */
    private IppAttrGroup createOperationGroup() {
        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = new IppAttrGroup(IppDelimiterTag.OPERATION_ATTR);

        AbstractIppDict dict = IppDictOperationAttr.instance();

        // ------------------------------------------------------------------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_CHARSET),
                "utf-8");
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_NATURAL_LANG),
                "en-us");

        return group;
    }

    /**
     * Adds a {@link JsonProxyPrinterOptGroup} to {@link JsonProxyPrinter}.
     *
     * @param printer
     *            The printer to add the option group to.
     * @param ippAttrGroup
     *            All the (raw) IPP options of the printer.
     * @param groupId
     *            The ID of the option group.
     * @param attrKeywords
     *            The IPP keywords to add to the option group.
     */
    private void addUserPrinterOptGroup(final JsonProxyPrinter printer,
            final IppAttrGroup ippAttrGroup,
            final ProxyPrinterOptGroupEnum groupId,
            final String[] attrKeywords) {

        final ArrayList<JsonProxyPrinterOpt> printerOptions = new ArrayList<>();

        for (final String keyword : attrKeywords) {
            this.addOption(printer, ippAttrGroup, printerOptions, keyword);
        }

        if (!printerOptions.isEmpty()) {

            final JsonProxyPrinterOptGroup optGroup =
                    new JsonProxyPrinterOptGroup();

            optGroup.setOptions(printerOptions);
            optGroup.setGroupId(groupId);
            optGroup.setUiText(groupId.toString());

            printer.getGroups().add(optGroup);
        }
    }

    /**
     * Adds an {@code IppBoolean} option to the printerOptions parameter.
     *
     * @param printerOptions
     *            The list of {@link JsonProxyPrinterOpt} to add the option to.
     * @param attrKeyword
     *            The IPP attribute keyword.
     * @param defaultChoice
     *            The default choice.
     */
    private void addOptionBoolean(
            final ArrayList<JsonProxyPrinterOpt> printerOptions,
            final String attrKeyword, final boolean defaultChoice) {

        final JsonProxyPrinterOpt option = new JsonProxyPrinterOpt();

        option.setKeyword(attrKeyword);
        option.setUiText(attrKeyword);
        final ArrayList<JsonProxyPrinterOptChoice> choices = new ArrayList<>();

        JsonProxyPrinterOptChoice choice = new JsonProxyPrinterOptChoice();
        choice.setChoice(IppBoolean.FALSE);
        choice.setUiText(IppBoolean.FALSE);
        choices.add(choice);

        choice = new JsonProxyPrinterOptChoice();
        choice.setChoice(IppBoolean.TRUE);
        choice.setUiText(IppBoolean.TRUE);
        choices.add(choice);

        option.setChoices(choices);

        if (defaultChoice) {
            option.setDefchoice(IppBoolean.TRUE);
        } else {
            option.setDefchoice(IppBoolean.FALSE);
        }

        printerOptions.add(option);
    }

    /**
     * Adds an option to the printerOptions parameter.
     * <p>
     * See Mantis #185.
     * </p>
     *
     * @param printer
     *            The {@link JsonProxyPrinter}.
     * @param group
     *            The {@link IppAttrGroup} with all the (raw) IPP options of the
     *            printer.
     * @param printerOptions
     *            The list of {@link JsonProxyPrinterOpt} to add the option to.
     * @param attrKeyword
     *            The IPP attribute keyword.
     */
    private void addOption(final JsonProxyPrinter printer,
            final IppAttrGroup group,
            final ArrayList<JsonProxyPrinterOpt> printerOptions,
            final String attrKeyword) {

        /*
         * Skip exclusive PPDE options.
         */
        if (attrKeyword.equals(
                IppDictJobTemplateAttr.ORG_PRINTFLOWLITE_ATTR_FINISHINGS_JOG_OFFSET)) {
            return;
        }

        /*
         * Handle internal attributes first.
         */
        if (attrKeyword.equals(
                IppDictJobTemplateAttr.ORG_PRINTFLOWLITE_ATTR_INT_PAGE_ROTATE180)) {
            this.addOptionBoolean(printerOptions, attrKeyword, false);
            return;
        }

        /*
         * INVARIANT: More than one (1) choice.
         */
        final IppAttrValue attrChoice =
                group.getAttrValue(IppDictJobTemplateAttr.attrName(attrKeyword,
                        ApplEnum.SUPPORTED));

        if (attrChoice == null || attrChoice.getValues().size() < 2) {
            return;
        }

        /*
         * If no default found, use the first from the list of choices.
         */
        String defChoice = group.getAttrSingleValue(
                IppDictJobTemplateAttr.attrName(attrKeyword, ApplEnum.DEFAULT));

        if (defChoice == null) {
            defChoice = attrChoice.getValues().get(0);
        }

        final String txtKeyword = attrKeyword;

        final JsonProxyPrinterOpt option = new JsonProxyPrinterOpt();

        option.setKeyword(attrKeyword);
        option.setUiText(txtKeyword);

        String defChoiceFound = attrChoice.getValues().get(0);

        final boolean isMedia = IppDictJobTemplateAttr.isMediaAttr(attrKeyword);

        final boolean isMediaSource =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE);

        final boolean isNup =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_NUMBER_UP);

        final boolean isSides =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_SIDES);

        final boolean isSheetCollate =
                attrKeyword.equals(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE);

        boolean sheetCollated = false;
        boolean sheetUncollated = false;
        boolean isDuplexPrinter = false;
        boolean hasManualMediaSource = false;
        boolean hasAutoMediaSource = false;

        for (final String choice : attrChoice.getValues()) {

            // Do not add "auto" media source, but set an indication (see
            // below).
            if (isMediaSource
                    && choice.equalsIgnoreCase(IppKeyword.MEDIA_SOURCE_AUTO)) {
                hasAutoMediaSource = true;
                continue;
            }

            /*
             * Mantis #185: Limit IPP n-up to max 9 (1 character).
             */
            if (isNup && choice.length() > 1) {
                continue;
            }

            // Skip media unknown in IppMediaSizeEnum.
            if (isMedia && IppMediaSizeEnum.find(choice) == null) {
                continue;
            }

            if (isSides
                    && !choice.equalsIgnoreCase(IppKeyword.SIDES_ONE_SIDED)) {
                isDuplexPrinter = true;
            } else if (isMediaSource && choice
                    .equalsIgnoreCase(IppKeyword.MEDIA_SOURCE_MANUAL)) {
                hasManualMediaSource = true;
            } else if (isSheetCollate) {
                if (choice
                        .equalsIgnoreCase(IppKeyword.SHEET_COLLATE_COLLATED)) {
                    sheetCollated = true;
                } else if (choice.equalsIgnoreCase(
                        IppKeyword.SHEET_COLLATE_UNCOLLATED)) {
                    sheetUncollated = true;
                }
            }

            if (choice.equals(defChoice)) {
                defChoiceFound = defChoice;
            }

            option.addChoice(choice, choice);
        }

        option.setDefchoice(defChoiceFound);
        option.setDefchoiceIpp(defChoiceFound);

        if (!option.getChoices().isEmpty()) {
            // A single media-source choice is added, but single choices of
            // other IPP attributes are not. See Mantis #1171.
            if (isMediaSource || option.getChoices().size() > 1) {
                printerOptions.add(option);
            }
        }

        if (isSides) {
            printer.setDuplexDevice(Boolean.valueOf(isDuplexPrinter));
        } else if (isMediaSource) {
            printer.setAutoMediaSource(Boolean.valueOf(hasAutoMediaSource));
            printer.setManualMediaSource(Boolean.valueOf(hasManualMediaSource));
        } else if (isSheetCollate) {
            printer.setSheetCollated(sheetCollated);
            printer.setSheetUncollated(sheetUncollated);
        }

    }

    /**
     * @return IPP request for {@link IppOperationId#CUPS_GET_PRINTERS}.
     */
    private List<IppAttrGroup> reqCupsGetPrinters() {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        /* */
        final IppAttrValue reqAttr =
                new IppAttrValue(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES,
                        IppKeyword.instance());

        for (final String value : new String[] {
                IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED,
                IppDictPrinterDescAttr.ATTR_PRINTER_NAME,
                IppDictPrinterDescAttr.ATTR_PRINTER_TYPE,
                IppDictPrinterDescAttr.ATTR_MEMBER_NAMES,
                IppDictPrinterDescAttr.ATTR_PRINTER_INFO,
                IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS,
                IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION,
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE,
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_CHANGE_TIME,
                IppDictPrinterDescAttr.ATTR_PRINTER_STATE_REASONS,
                IppDictPrinterDescAttr.ATTR_COLOR_SUPPORTED,
                IppDictPrinterDescAttr.ATTR_PRINTER_MORE_INFO_MANUFACTURER,
                IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL, //
                IppDictPrinterDescAttr.ATTR_CUPS_PPD_NAME //
        }) {
            reqAttr.addValue(value);
        }

        group.addAttribute(reqAttr);

        // ---------
        return attrGroups;
    }

    /**
     * @param uriPrinter
     *            Printer URI
     * @return IPP request for {@link IppOperationId#CUPS_DELETE_PRINTER}.
     */
    private List<IppAttrGroup> reqCupsDeletePrinter(final URI uriPrinter) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        /*
         * Group 1: Operation Attributes
         */
        final IppAttrGroup group = this.createOperationGroup();
        attrGroups.add(group);

        final AbstractIppDict dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter.toString());

        return attrGroups;
    }

    /**
     * @param uriPrinter
     *            Printer URI.
     * @param printerObj
     *            Printer object attributes.
     * @return IPP request for {@link IppOperationId#CUPS_ADD_MODIFY_PRINTER}.
     */
    private List<IppAttrGroup> reqCupsAddModifyPrinter(final URI uriPrinter,
            final CupsAttrPrinter printerObj) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup groupWlk;
        AbstractIppDict dictWlk;

        /*
         * Group 1: Operation Attributes
         */
        groupWlk = this.createOperationGroup();
        attrGroups.add(groupWlk);

        dictWlk = IppDictOperationAttr.instance();

        groupWlk.add(dictWlk.getAttr(IppDictOperationAttr.ATTR_PRINTER_URI),
                uriPrinter.toString());

        /*
         * Group 2: Printer Object Attributes
         */
        groupWlk = new IppAttrGroup(IppDelimiterTag.PRINTER_ATTR);
        attrGroups.add(groupWlk);

        dictWlk = IppDictPrinterDescAttr.instance();

        // Honor any non-null value.

        if (printerObj.getDeviceUri() != null) {
            groupWlk.add(
                    dictWlk.getAttr(IppDictPrinterDescAttr.ATTR_DEVICE_URI),
                    printerObj.getDeviceUri().toString());
        }
        if (printerObj.getPpdName() != null) {
            groupWlk.add(
                    dictWlk.getAttr(IppDictPrinterDescAttr.ATTR_CUPS_PPD_NAME),
                    printerObj.getPpdName());
        }
        if (printerObj.getLocation() != null) {
            groupWlk.add(
                    dictWlk.getAttr(
                            IppDictPrinterDescAttr.ATTR_PRINTER_LOCATION),
                    printerObj.getLocation());
        }
        if (printerObj.getInfo() != null) {
            groupWlk.add(
                    dictWlk.getAttr(IppDictPrinterDescAttr.ATTR_PRINTER_INFO),
                    printerObj.getInfo());
        }
        if (printerObj.getIsAcceptingJobs() != null) {
            groupWlk.add(dictWlk.getAttr(
                    IppDictPrinterDescAttr.ATTR_PRINTER_IS_ACCEPTING_JOBS),
                    printerObj.getIsAcceptingJobs().booleanValue()
                            ? IppBoolean.TRUE
                            : IppBoolean.FALSE);
        }
        if (printerObj.getState() != null) {
            groupWlk.add(
                    dictWlk.getAttr(IppDictPrinterDescAttr.ATTR_PRINTER_STATE),
                    printerObj.getState().ippValue());
        }

        // ----------------
        return attrGroups;
    }

    /**
     * Request a list of PPD manufacturers, or PPD's of a single manufacturer.
     * See {@link IppOperationId#CUPS_GET_PPDS}.
     *
     * @param manufacturer
     *            Value of {@link IppDictPrinterDescAttr#ATTR_CUPS_PPD_MAKE} or
     *            {@code null} for a list of manufacturers,
     * @return IPP request.
     */
    private List<IppAttrGroup> reqCupsGetPPDs(final String manufacturer) {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        attrGroups.add(group);

        if (manufacturer == null) {
            // Get a list of manufacturers.
            final IppAttrValue reqAttr = new IppAttrValue(
                    IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES,
                    IppKeyword.instance());
            reqAttr.addValue(IppDictPrinterDescAttr.ATTR_CUPS_PPD_MAKE);
            group.addAttribute(reqAttr);

        } else {
            // Get a list of PPD's for this one manufacturer.
            final AbstractIppDict dict = IppDictPrinterDescAttr.instance();
            group.add(dict.getAttr(IppDictPrinterDescAttr.ATTR_CUPS_PPD_MAKE),
                    manufacturer);
        }

        /*
         * For documentation purposes only.
         *
         * (1) URI's with these schema's are excluded by this filter.
         */

        // final IppAttrValue reqSchemesExcl =
        // new IppAttrValue(ATTR_CUPS_EXCLUDE_SCHEMES, new IppName());
        // reqSchemesExcl.addValue("foomatic-db-compressed-ppds"); // TEST
        // reqSchemesExcl.addValue("openprinting-ppds"); // TEST
        // group.addAttribute(reqSchemesExcl);

        /*
         * (2) limit to 8 instances.
         */
        // group.add(ATTR_CUPS_LIMIT, IppInteger.instance(), "8");

        return attrGroups;
    }

    /**
     * @return IPP request for {@link IppOperationId#CUPS_GET_DEVICES}.
     */
    private List<IppAttrGroup> reqCupsGetDevices() {

        final List<IppAttrGroup> attrGroups = new ArrayList<>();

        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = this.createOperationGroup();
        attrGroups.add(group);

        // Request URI
        final IppAttrValue reqAttr =
                new IppAttrValue(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES,
                        IppKeyword.instance());
        reqAttr.addValue(IppDictPrinterDescAttr.ATTR_DEVICE_URI);
        group.addAttribute(reqAttr);

        /*
         * Exclude common single word scheme (and filters) names. As tests
         * show... URI's with these names are included if the single word name
         * is excluded. Also, if a single word name to exclude is not found,
         * URI's containing the schema name are excluded.
         */
        final IppAttrValue reqSchemesExcl =
                new IppAttrValue(ATTR_CUPS_EXCLUDE_SCHEMES, new IppName());

        for (final String value : new String[] {
                //
                IppUriScheme.SCHEME_HTTP, //
                IppUriScheme.SCHEME_HTTPS, //
                IppUriScheme.SCHEME_IPP, //
                IppUriScheme.SCHEME_IPPS, //
                IppUriScheme.SCHEME_LPD, //
                IppUriScheme.SCHEME_SERIAL, //
                IppUriScheme.SCHEME_SOCKET //
        }) {
            reqSchemesExcl.addValue(value);
        }
        group.addAttribute(reqSchemesExcl);

        // Do not set a limit
        // group.add(ATTR_CUPS_LIMIT, IppInteger.instance(), "8");

        return attrGroups;
    }

    /**
     * @return CUPS server URL.
     */
    private URL getUrlDefaultServer() {
        return this.urlDefaultServer;
    }

    @Override
    public String getDefaultCupsUrl() {
        return InetUtils.URL_PROTOCOL_HTTP.concat("://")
                .concat(ConfigManager.getDefaultCupsHost()).concat(":")
                .concat(ConfigManager.getCupsPort());
    }

    @Override
    public JsonProxyPrinter retrieveCupsPrinterDetails(final String printerName,
            final URI printerUri) throws IppConnectException {

        JsonProxyPrinter printer = null;

        final List<IppAttrGroup> response =
                this.getIppPrinterAttr(printerName, printerUri);

        if (response.size() > 1) {
            printer = this.createUserPrinter(response.get(1));
        }

        return printer;
    }

    @Override
    public JsonProxyPrintJob retrievePrintJobUri(final URL urlCupsServer,
            final URI uriPrinter, final String uriJob, final Integer jobId)
            throws IppConnectException {

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode;

        if (uriJob == null) {
            statusCode = this.ippClient.send(urlCupsServer,
                    IppOperationId.GET_JOB_ATTR,
                    this.reqGetJobAttr(uriPrinter, jobId), response);
        } else {
            statusCode = this.ippClient.send(urlCupsServer,
                    IppOperationId.GET_JOB_ATTR, reqGetJobAttr(uriJob),
                    response);
        }

        final JsonProxyPrintJob job;

        if (statusCode == IppStatusCode.OK && response.size() > 1) {

            job = new JsonProxyPrintJob();

            job.setJobId(jobId);

            final IppAttrGroup group = response.get(1);

            job.setDest(group.getAttrSingleValue(
                    IppDictJobDescAttr.ATTR_JOB_PRINTER_URI));
            job.setTitle(
                    group.getAttrSingleValue(IppDictJobDescAttr.ATTR_JOB_NAME));

            job.setJobState(Integer.parseInt(
                    group.getAttrSingleValue(IppDictJobDescAttr.ATTR_JOB_STATE),
                    NumberUtil.RADIX_10));
            job.setJobStateMessage(group.getAttrSingleValue(
                    IppDictJobDescAttr.ATTR_JOB_STATE_MESSAGE));
            job.setJobStateReasons(group
                    .getAttrValues(IppDictJobDescAttr.ATTR_JOB_STATE_REASONS));

            job.setCreationTime(Integer.valueOf(
                    group.getAttrSingleValue(
                            IppDictJobDescAttr.ATTR_TIME_AT_CREATION),
                    NumberUtil.RADIX_10));

            final String value = group.getAttrSingleValue(
                    IppDictJobDescAttr.ATTR_TIME_AT_COMPLETED, "");

            if (StringUtils.isNotBlank(value)) {
                job.setCompletedTime(
                        Integer.parseInt(value, NumberUtil.RADIX_10));
            }

        } else {
            job = null;
        }
        // ---------------
        return job;
    }

    @Override
    public JsonProxyPrinter cupsGetDefault() throws IppConnectException {

        final List<IppAttrGroup> request = new ArrayList<>();

        request.add(createOperationGroup());

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode =
                this.ippClient.send(this.getUrlDefaultServer(),
                        IppOperationId.CUPS_GET_DEFAULT, request, response);

        if (statusCode == IppStatusCode.CLI_NOTFND) {
            return null;
        }

        return this.createUserPrinter(response.get(1));
    }

    @Override
    public List<JsonProxyPrinter> cupsGetPrinters() throws IppConnectException,
            URISyntaxException, MalformedURLException {
        // All printers, including CUPS printer classes.
        final List<JsonProxyPrinter> printers = new ArrayList<>();

        // A map of ALL printer by uppercase name.
        final Map<String, JsonProxyPrinter> printerMap = new HashMap<>();

        // The printers that are a CUPS printer class.
        final List<CupsPrinterClass> printerClasses = new ArrayList<>();

        //
        final boolean remoteCupsEnabled = ConfigManager.instance()
                .isConfigValue(Key.CUPS_IPP_REMOTE_ENABLED);

        /*
         * Get the list of CUPS printers.
         */
        final List<IppAttrGroup> response = this.ippClient.send(
                this.getUrlDefaultServer(), IppOperationId.CUPS_GET_PRINTERS,
                this.reqCupsGetPrinters());

        /*
         * Traverse the response groups.
         */
        for (IppAttrGroup group : response) {

            /*
             * Handle PRINTER_ATTR groups only.
             */
            if (group.getDelimiterTag() != IppDelimiterTag.PRINTER_ATTR) {
                continue;
            }

            /*
             * Skip any PrintFlowLite printer.
             */
            final String makeModel = group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_PRINTER_MAKE_MODEL, "");

            if (makeModel.toLowerCase().startsWith("PrintFlowLite")) {
                continue;
            }

            /*
             * Get the printer URI.
             */
            final String printerUriSupp = group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_PRINTER_URI_SUPPORTED);

            final URI uriPrinter = new URI(printerUriSupp);

            /*
             * Skip remote printer when remoteCups is disabled.
             */
            if (!remoteCupsEnabled && !this.isLocalPrinter(uriPrinter)) {
                continue;
            }

            /*
             * Get the printer-type: printer class?
             */
            final Integer printerType =
                    Integer.parseInt(group.getAttrSingleValue(
                            IppDictPrinterDescAttr.ATTR_PRINTER_TYPE));

            if (IppPrinterType.hasProperty(printerType,
                    IppPrinterType.BitEnum.IMPLICIT_CLASS)) {
                continue;
            }

            final boolean isPpdPresent;

            if (IppPrinterType.hasProperty(printerType,
                    IppPrinterType.BitEnum.PRINTER_CLASS)) {

                isPpdPresent = false;

                final String printerName = group
                        .getAttrSingleValue(
                                IppDictPrinterDescAttr.ATTR_PRINTER_NAME)
                        .toUpperCase();

                final CupsPrinterClass printerClass = new CupsPrinterClass();

                printerClass.setPrinterUri(uriPrinter);
                printerClass.setName(printerName);

                for (final String member : group
                        .getAttrValue(IppDictPrinterDescAttr.ATTR_MEMBER_NAMES)
                        .getValues()) {
                    printerClass.addMemberName(member.toUpperCase());
                }

                printerClasses.add(printerClass);
            } else {
                isPpdPresent = this.isCupsPpdPresent(uriPrinter);
            }

            /*
             * Create JsonProxyPrinter object from PRINTER_ATTR group.
             */
            final JsonProxyPrinter proxyPrinterFromGroup =
                    this.createUserPrinter(group);

            if (proxyPrinterFromGroup == null) {
                continue;
            }

            /*
             * Retrieve printer details.
             */
            try {
                final JsonProxyPrinter proxyPrinterDetails =
                        this.retrieveCupsPrinterDetails(
                                proxyPrinterFromGroup.getName(),
                                proxyPrinterFromGroup.getPrinterUri());

                if (proxyPrinterDetails != null) {

                    proxyPrinterDetails.setPpdPresent(isPpdPresent);

                    printers.add(proxyPrinterDetails);
                    printerMap.put(proxyPrinterDetails.getName(),
                            proxyPrinterDetails);
                }

            } catch (IppConnectException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format("%s [%s]: %s",
                            e.getClass().getSimpleName(),
                            proxyPrinterFromGroup.getName(), e.getMessage()));
                }
            }
        }

        //
        for (final CupsPrinterClass printerClass : printerClasses) {

            /*
             * INVARIANT: Printer Class MUST have members.
             */
            if (printerClass.getMemberNames().size() == 0) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "No Proxy Printer Members of Printer "
                                    + "class [%s] found.",
                            printerClass.getName()));
                }
                continue;
            }

            /*
             * INVARIANT: Printer Class members MUST same Make and Model.
             */
            String commonMakeModel = null;

            for (final String member : printerClass.getMemberNames()) {

                final JsonProxyPrinter proxyPrinterMember =
                        printerMap.get(member);

                proxyPrinterMember.addPrinterClass(printerClass.getName());

                if (commonMakeModel == null) {
                    commonMakeModel = proxyPrinterMember.getModelName();
                } else if (!proxyPrinterMember.getModelName()
                        .equals(commonMakeModel)) {
                    commonMakeModel = null;
                    break;
                }
            }

            if (commonMakeModel == null) {
                LOGGER.error(String.format(
                        "Proxy Printer Members of Printer class [%s] "
                                + "do NOT have same Make/Model.",
                        printerClass.getName()));
                continue;
            }

            /*
             * Use the IPP options of the first member.
             */
            final String memberName = printerClass.getMemberNames().get(0);

            final JsonProxyPrinter proxyPrinterMember =
                    printerMap.get(memberName);

            /*
             * INVARIANT: Printer Class member MUST be present as Proxy Printer.
             */
            if (proxyPrinterMember == null) {
                LOGGER.error(String.format(
                        "Proxy Printer Member [%s] of Printer class [%s] "
                                + "not found.",
                        memberName, printerClass.getName()));
                continue;
            }

            final JsonProxyPrinter proxyPrinterClass =
                    printerMap.get(printerClass.getName());

            proxyPrinterClass
                    .setCupsClassMembers(printerClass.getMemberNames().size());

            initPrinterClassFromMember(proxyPrinterClass, proxyPrinterMember);

            if (LOGGER.isTraceEnabled()) {
                final String msg =
                        String.format("Printer Class [%s] URI [%s] [%s]",
                                printerClass.getName(),
                                printerClass.getPrinterUri(), commonMakeModel);
                LOGGER.trace(msg);
            }
        }
        return printers;
    }

    @Override
    public List<URI> cupsGetDevices()
            throws IppConnectException, URISyntaxException {

        final List<URI> devices = new ArrayList<>();

        final List<IppAttrGroup> response = this.ippClient.send(
                this.getUrlDefaultServer(), true,
                IppOperationId.CUPS_GET_DEVICES, this.reqCupsGetDevices());
        /*
         * Traverse the response groups.
         */
        for (IppAttrGroup group : response) {

            // Handle PRINTER_ATTR groups only.
            if (group.getDelimiterTag() != IppDelimiterTag.PRINTER_ATTR) {
                continue;
            }
            // Get the printer URI.
            final String printerUri = group
                    .getAttrSingleValue(IppDictPrinterDescAttr.ATTR_DEVICE_URI);
            devices.add(new URI(printerUri));
        }

        return devices;
    }

    @Override
    public List<String> cupsGetPPDManufacturers()
            throws IppConnectException, URISyntaxException {

        final List<String> ppds = new ArrayList<>();

        final List<IppAttrGroup> response = this.ippClient.send(
                this.getUrlDefaultServer(), true, IppOperationId.CUPS_GET_PPDS,
                this.reqCupsGetPPDs(null));
        /*
         * Traverse the response groups.
         */
        for (IppAttrGroup group : response) {
            // Handle PRINTER_ATTR groups only.
            if (group.getDelimiterTag() != IppDelimiterTag.PRINTER_ATTR) {
                continue;
            }
            ppds.add(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_MAKE));
        }
        return ppds;
    }

    @Override
    public List<CupsAttrPPD> cupsGetPPDs(final String manufacturer)
            throws IppConnectException, URISyntaxException {

        final List<CupsAttrPPD> ppds = new ArrayList<>();

        final List<IppAttrGroup> response = this.ippClient.send(
                this.getUrlDefaultServer(), true, IppOperationId.CUPS_GET_PPDS,
                this.reqCupsGetPPDs(manufacturer));
        /*
         * Traverse the response groups.
         */
        for (IppAttrGroup group : response) {

            // Handle PRINTER_ATTR groups only.
            if (group.getDelimiterTag() != IppDelimiterTag.PRINTER_ATTR) {
                continue;
            }

            final CupsAttrPPD dto = new CupsAttrPPD();
            ppds.add(dto);

            dto.setDeviceId(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_DEVICE_ID));
            dto.setName(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_NAME));
            dto.setMake(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_MAKE));
            dto.setMakeAndModel(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_MAKE_AND_MODEL));

            dto.setModelNumber(Integer.valueOf(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_MODEL_NUMBER)));

            dto.setProduct(group.getAttrValues(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_PRODUCT));
            dto.setPsVersion(group.getAttrValues(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_PSVERSION));

            dto.setType(group.getAttrSingleValue(
                    IppDictPrinterDescAttr.ATTR_CUPS_PPD_TYPE));
        }
        return ppds;
    }

    @Override
    public boolean cupsGetPPD(final URI printerURI, final OutputStream ppdOut)
            throws IppConnectException {

        final URL urlCupsServer;

        try {
            urlCupsServer = this.getCupsServerUrl(printerURI);
        } catch (MalformedURLException e) {
            throw new SpException(e.getMessage());
        }

        final List<IppAttrGroup> ippRequest =
                new IppReqCupsGetPpd(printerURI).build();

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = this.ippClient.send(urlCupsServer,
                IppOperationId.CUPS_GET_PPD, ippRequest, response, ppdOut);

        if (statusCode == IppStatusCode.OK) {
            // The PPD file followed the end of the IPP response and was
            // captured in the OutputStream ppdOut parameter.
            return true;
        } else if (statusCode == IppStatusCode.CUPS_SEE_OTHER) {
            // Get the printer URI that provides the actual PPD file.
            final String printerUriRedirect = response.get(0)
                    .getAttrSingleValue(IppDictOperationAttr.ATTR_PRINTER_URI);
            LOGGER.warn("PPD for [{}] NOT found: redirect [{}] NOT followed.",
                    printerURI, Objects.toString(printerUriRedirect, "?"));
            return false;
        } else if (statusCode == IppStatusCode.CLI_NOTFND) {
            // PPD file does not exist: e.g. Raw Printer
            return false;
        }

        return false;
    }

    @Override
    public boolean cupsDeletePrinter(final URI uriPrinter)
            throws IppConnectException, URISyntaxException {

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = this.ippClient.send(
                this.getUrlDefaultServer(), IppOperationId.CUPS_DELETE_PRINTER,
                this.reqCupsDeletePrinter(uriPrinter), response);

        return statusCode == IppStatusCode.OK;
    }

    @Override
    public boolean cupsAddModifyPrinter(final URI uriPrinter,
            final CupsAttrPrinter printerObj)
            throws IppConnectException, URISyntaxException {

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = this.ippClient.send(
                this.getUrlDefaultServer(),
                IppOperationId.CUPS_ADD_MODIFY_PRINTER,
                this.reqCupsAddModifyPrinter(uriPrinter, printerObj), response);

        return statusCode == IppStatusCode.OK;
    }

    @Override
    public int getCupsSystemTime() {
        return (int) (System.currentTimeMillis() / NumberUtil.INT_THOUSAND);
    }

    @Override
    public Date getCupsDate(final Integer cupsTime) {
        return new Date(cupsTime.longValue() * NumberUtil.INT_THOUSAND);
    }

    @Override
    public String getCupsVersion() {
        List<IppAttrGroup> reqGroups = new ArrayList<>();

        IppAttrGroup group = null;

        /*
         * Group 1: Operation Attributes
         */
        group = createOperationGroup();
        reqGroups.add(group);

        AbstractIppDict dict = IppDictOperationAttr.instance();

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_REQUESTED_ATTRIBUTES),
                IppDictPrinterDescAttr.ATTR_CUPS_VERSION);

        group.add(dict.getAttr(IppDictOperationAttr.ATTR_LIMIT), "1");

        // -----------
        List<IppAttrGroup> response = new ArrayList<>();

        String version = null;

        try {
            final IppStatusCode statusCode = this.ippClient.send(
                    this.getUrlDefaultServer(),
                    IppOperationId.CUPS_GET_PRINTERS, reqGroups, response);

            if (statusCode == IppStatusCode.OK) {
                version = response.get(1).getAttrSingleValue(
                        IppDictPrinterDescAttr.ATTR_CUPS_VERSION);
            }

        } catch (IppConnectException e) {
            // noop
        }

        return version;
    }

    @Override
    public boolean isCupsPpdPresent(final URI printerURI)
            throws IppConnectException {
        return this.cupsGetPPD(printerURI, null);
    }

    @Override
    public List<IppAttrGroup> getIppPrinterAttr(final String printerName,
            final URI printerUri) throws IppConnectException {

        final boolean isLocalCups = this.isLocalPrinter(printerUri);

        try {
            /*
             * If this is a REMOTE printer (e.g. printerUri:
             * ipp://192.168.1.36:631/printers/HL-2030-series) ...
             *
             * ... then we MUST use that URI to get the details (groups).
             */
            final URL urlCupsServer;

            if (isLocalCups) {
                urlCupsServer = this.getUrlDefaultServer();
            } else {
                urlCupsServer = this.getCupsServerUrl(printerUri);
            }
            return this.ippClient.send(urlCupsServer, isLocalCups,
                    IppOperationId.GET_PRINTER_ATTR,
                    this.reqGetPrinterAttr(printerUri));

        } catch (MalformedURLException e) {
            throw new SpException(e);
        }
    }

    /**
     * @param uri
     *            CUPS printer URI
     * @return CUPS printer name.
     */
    private static String getCupsPrinterName(final URI uri) {
        final String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     */
    @Override
    public boolean setIppPrinterAttr(final URI printerUri,
            final Map<IppAttr, String> attributes) throws IppConnectException {

        boolean execIPP = false;

        if (execIPP) {
            // TODO : make this method work.
            return this.setIppPrinterAttrEx(printerUri, attributes);
        }

        // For now, use the CLI interface.
        final Map<String, String> options = new HashMap<>();
        for (final Entry<IppAttr, String> attr : attributes.entrySet()) {
            options.put(attr.getKey().getKeyword(), attr.getValue());
        }
        try {
            return CupsCommandLine
                    .setPrinterOptions(getCupsPrinterName(printerUri), options);
        } catch (IOException | InterruptedException e) {
            throw new IppConnectException(e);
        }
    }

    /**
     * Sets attributes for a printer. See <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3380#page-10">RCC3380</a>.
     * <p>
     * <b>Warning</b>: This method does NOT work. Why?
     * </p>
     *
     * @param printerUri
     *            The {@link URI} of the IPP printer.
     * @param attributes
     *            IPP key/value pairs.
     * @return {@code true} if successful.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    private boolean setIppPrinterAttrEx(final URI printerUri,
            final Map<IppAttr, String> attributes) throws IppConnectException {

        final boolean isLocalCups = this.isLocalPrinter(printerUri);

        try {
            /*
             * If this is a REMOTE printer (e.g. printerUri:
             * ipp://192.168.1.36:631/printers/HL-2030-series) ...
             *
             * ... then we MUST use that URI to get the details (groups).
             */
            final URL urlCupsServer;

            if (isLocalCups) {
                urlCupsServer = this.getUrlDefaultServer();
            } else {
                urlCupsServer = this.getCupsServerUrl(printerUri);
            }

            final List<IppAttrGroup> response = new ArrayList<>();

            final IppStatusCode statusCode = this.ippClient.send(urlCupsServer,
                    IppOperationId.SET_PRINTER_ATTRIBUTES,
                    this.reqSetPrinterAttr(printerUri, attributes), response);

            return statusCode == IppStatusCode.OK;

        } catch (MalformedURLException e) {
            throw new SpException(e);
        }

    }

    @Override
    public List<IppAttrGroup> printJob(final URL urlCupsServer,
            final IppReqPrintJob ippJobReq, final File fileToPrint)
            throws IppConnectException {

        final IppOperationId ippOperation = IppOperationId.PRINT_JOB;

        final List<IppAttrGroup> ippRequest = ippJobReq.build();

        final List<IppAttrGroup> response = this.ippClient.send(urlCupsServer,
                ippOperation, ippRequest, fileToPrint);

        ProxyPrintLogger.log(ippOperation, ippRequest, response);

        return response;
    }

    @Override
    public boolean cancelPrintJob(final URI uriPrinter,
            final String requestingUserName, final Integer jobId)
            throws IppConnectException {

        final URL urlCupsServer;

        try {
            urlCupsServer = this.getCupsServerUrl(uriPrinter);
        } catch (MalformedURLException e) {
            throw new SpException(e);
        }

        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = this.ippClient.send(urlCupsServer,
                IppOperationId.CANCEL_JOB,
                this.reqCancelJobAttr(uriPrinter, jobId, requestingUserName),
                response);

        return statusCode == IppStatusCode.OK;
    }

    @Override
    public IppClient init() {
        try {
            this.urlDefaultServer = new URL(this.getDefaultCupsUrl());
        } catch (MalformedURLException e) {
            throw new SpException(e);
        }
        this.ippClient.init();
        return this.ippClient;
    }

    @Override
    public void exit() throws IppConnectException, IppSyntaxException {
        this.ippClient.shutdown();
    }

    @Override
    public URL getCupsPpdUrl(final String printerName) {
        try {
            return new URL(this.getUrlDefaultServer().toString()
                    .concat(URL_PATH_CUPS_PRINTERS).concat("/")
                    .concat(printerName).concat(".ppd"));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    /**
     * @return Value of IPP "requesting-user-name" keyword for CUPS event
     *         subscription.
     */
    private static String getCUPSEventSubscrRequestingUserName() {
        return ConfigManager.getProcessUserName();
    }

    /**
     * Gets the value of IPP "notify-recipient-uri" keyword for CUPS even push
     * notification.
     * <p>
     * Example: {@code PrintFlowLite:localhost:8631}
     * </p>
     *
     * @return IPP keyword value.
     */
    private static String getCUPSPushSubscrNotifyRecipientUri() {
        return String.format("%s:" + InetUtils.LOCAL_HOST + ":%s",
                ConfigManager.getCupsNotifier(), ConfigManager.getServerPort());
    }

    @Override
    public boolean startCUPSPushEventSubscription()
            throws IppConnectException, IppSyntaxException {

        if (!ConfigManager.isCupsPushNotification()) {
            return false;
        }

        final String requestingUser = getCUPSEventSubscrRequestingUserName();
        final String recipientUri = getCUPSPushSubscrNotifyRecipientUri();
        final String leaseSeconds = ConfigManager.instance().getConfigValue(
                Key.CUPS_IPP_NOTIFICATION_PUSH_NOTIFY_LEASE_DURATION);

        /*
         * Step 1: Get the existing printer subscriptions for requestingUser.
         */
        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode =
                this.ippClient.send(this.getUrlDefaultServer(),
                        IppOperationId.GET_SUBSCRIPTIONS,
                        this.reqGetPrinterSubscriptions(
                                SUBSCRIPTION_PRINTER_URI, requestingUser),
                        response);

        /*
         * NOTE: When this is a first-time subscription it is possible that
         * there are NO subscriptions for the user, this will result in status
         * IppStatusCode.CLI_NOTFND or IppStatusCode.CLI_NOTPOS.
         */
        if (statusCode != IppStatusCode.OK
                && statusCode != IppStatusCode.CLI_NOTFND
                && statusCode != IppStatusCode.CLI_NOTPOS) {
            throw new IppSyntaxException(statusCode.toString());
        }

        /*
         * Step 2: Renew only our OWN printer subscription.
         */
        boolean isRenewed = false;

        for (final IppAttrGroup group : response) {

            if (group.getDelimiterTag() != IppDelimiterTag.SUBSCRIPTION_ATTR) {
                continue;
            }

            /*
             * There might be other subscription that are NOT ours (like native
             * CUPS descriptions).
             */
            final String recipientUriFound = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_RECIPIENT_URI);

            if (recipientUriFound == null
                    || !recipientUri.equals(recipientUriFound)) {
                continue;
            }

            final String subscriptionId = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_SUBSCRIPTION_ID);

            this.ippClient.send(this.getUrlDefaultServer(),
                    IppOperationId.RENEW_SUBSCRIPTION,
                    this.reqRenewPrinterSubscription(requestingUser,
                            subscriptionId, leaseSeconds));

            isRenewed = true;
        }

        /*
         * ... or create when not renewed.
         */
        if (!isRenewed) {
            this.ippClient.send(this.getUrlDefaultServer(),
                    IppOperationId.CREATE_PRINTER_SUBSCRIPTIONS,
                    this.reqCreatePrinterPushSubscriptions(requestingUser,
                            recipientUri, leaseSeconds));
        }

        return true;
    }

    @Override
    public void stopCUPSEventSubscription()
            throws IppConnectException, IppSyntaxException {

        final String requestingUser = getCUPSEventSubscrRequestingUserName();
        final String recipientUri = getCUPSPushSubscrNotifyRecipientUri();
        /*
         * Step 1: Get the existing printer subscriptions for requestingUser.
         */
        final List<IppAttrGroup> response = new ArrayList<>();

        final IppStatusCode statusCode = this.ippClient.send(
                this.getUrlDefaultServer(), IppOperationId.GET_SUBSCRIPTIONS,
                reqGetPrinterSubscriptions(SUBSCRIPTION_PRINTER_URI,
                        requestingUser),
                response);
        /*
         * NOTE: it is possible that there are NO subscriptions for the user,
         * this will given an IPP_NOT_FOUND.
         */
        if (statusCode != IppStatusCode.OK
                && statusCode != IppStatusCode.CLI_NOTFND) {
            throw new IppSyntaxException(statusCode.toString());
        }

        /*
         * Step 2: Cancel only our OWN printer subscription.
         */
        for (final IppAttrGroup group : response) {

            if (group.getDelimiterTag() != IppDelimiterTag.SUBSCRIPTION_ATTR) {
                continue;
            }

            /*
             * There might be other subscription that are NOT ours (like native
             * CUPS descriptions).
             */
            final String recipientUriFound = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_RECIPIENT_URI);

            if (recipientUriFound == null
                    || !recipientUri.equals(recipientUriFound)) {
                continue;
            }

            final String subscriptionId = group.getAttrSingleValue(
                    IppDictSubscriptionAttr.ATTR_NOTIFY_SUBSCRIPTION_ID);

            this.ippClient.send(this.getUrlDefaultServer(),
                    IppOperationId.CANCEL_SUBSCRIPTION,
                    this.reqCancelPrinterSubscription(requestingUser,
                            subscriptionId));
        }
    }

}
