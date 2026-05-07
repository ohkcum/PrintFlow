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
package org.printflow.lite.core.services;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.printflow.lite.core.ipp.IppSyntaxException;
import org.printflow.lite.core.ipp.attribute.IppAttr;
import org.printflow.lite.core.ipp.attribute.IppAttrGroup;
import org.printflow.lite.core.ipp.attribute.IppDictPrinterDescAttr;
import org.printflow.lite.core.ipp.client.CupsAttrPPD;
import org.printflow.lite.core.ipp.client.CupsAttrPrinter;
import org.printflow.lite.core.ipp.client.IppClient;
import org.printflow.lite.core.ipp.client.IppConnectException;
import org.printflow.lite.core.ipp.client.IppReqPrintJob;
import org.printflow.lite.core.ipp.operation.IppOperationId;
import org.printflow.lite.core.print.proxy.JsonProxyPrintJob;
import org.printflow.lite.core.print.proxy.JsonProxyPrinter;

/**
 * IPP client services for communication with the IPP server (CUPS).
 *
 * @author Rijk Ravestein
 *
 */
public interface IppClientService {

    /** */
    String ATTR_CUPS_EXCLUDE_SCHEMES = "exclude-schemes";
    /** */
    String ATTR_CUPS_INCLUDE_SCHEMES = "include-schemes";
    /** */
    String ATTR_CUPS_LIMIT = "limit";

    /**
     * Initializes the service.
     *
     * @return the IPP client.
     */
    IppClient init();

    /**
     * Closes the service.
     *
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws IppSyntaxException
     *             If syntax error.
     */
    void exit() throws IppConnectException, IppSyntaxException;

    /**
     * Gets the IPP attributes of a printer.
     *
     * @param printerName
     *            The printer name.
     * @param printerUri
     *            The {@link URI} of the IPP printer.
     * @return A list of {@link IppAttrGroup} instances.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    List<IppAttrGroup> getIppPrinterAttr(String printerName, URI printerUri)
            throws IppConnectException;

    /**
     * Sets attributes for a printer. See <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3380#page-10">RCC3380</a>.
     *
     * @param printerUri
     *            The {@link URI} of the IPP printer.
     * @param attributes
     *            IPP key/value pairs.
     * @return {@code true} if successful.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    boolean setIppPrinterAttr(URI printerUri, Map<IppAttr, String> attributes)
            throws IppConnectException;

    /**
     * Retrieves the printer details. Note that the details are a subset of all
     * the IPP printer options.
     *
     * @param printerName
     *            The CUPS printer name.
     * @param printerUri
     *            The URI of the printer.
     * @return The {@link JsonProxyPrinter} or {@code null} when not found.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    JsonProxyPrinter retrieveCupsPrinterDetails(String printerName,
            URI printerUri) throws IppConnectException;

    /**
     * Prints a PDF file.
     *
     * @param urlCupsServer
     * @param ippJobRequest
     * @param fileToPrint
     * @return IPP response.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    List<IppAttrGroup> printJob(URL urlCupsServer, IppReqPrintJob ippJobRequest,
            File fileToPrint) throws IppConnectException;

    /**
     * Retrieves the print job data using the URI of the printer or the job.
     *
     * @param urlCupsServer
     *            The {@link URL} of the CUPS server.
     * @param uriPrinter
     *            If {@code null} uriJob is used.
     * @param uriJob
     *            If {@code null} uriPrinter and jobId is used.
     * @param jobId
     *            CUPS job id.
     * @return {@code null} when print job is not found.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    JsonProxyPrintJob retrievePrintJobUri(URL urlCupsServer, URI uriPrinter,
            String uriJob, Integer jobId) throws IppConnectException;

    /**
     * Cancels a print job.
     *
     * @param uriPrinter
     * @param requestingUserName
     * @param jobId
     * @return {@code true} when successfully cancelled.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    boolean cancelPrintJob(URI uriPrinter, String requestingUserName,
            Integer jobId) throws IppConnectException;

    /**
     * Starts a CUPS push event subscription, if CUPS push notification is
     * configured/enabled in PrintFlowLite. This is an idempotent operation: when the
     * subscription already exists it is renewed.
     *
     * @return {@code false} if CUPS Push notification is not enabled.
     *
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws IppSyntaxException
     *             If syntax error.
     */
    boolean startCUPSPushEventSubscription()
            throws IppConnectException, IppSyntaxException;

    /**
     * Stops any active CUPS event subscription that was started with
     * {@link #startCUPSPushEventSubscription()}. This is an idempotent
     * operation: when the subscription does not exists, it is not stopped.
     *
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws IppSyntaxException
     *             If syntax error.
     */
    void stopCUPSEventSubscription()
            throws IppConnectException, IppSyntaxException;

    /**
     * URL of the CUPS host, like {@code http://localhost:631} .
     *
     * @return The URL.
     */
    String getDefaultCupsUrl();

    /**
     * Gets the default {@link JsonProxyPrinter}. See
     * {@link IppOperationId#CUPS_GET_DEFAULT}.
     *
     * @return The default {@link JsonProxyPrinter} or {@code null} when not
     *         found.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    JsonProxyPrinter cupsGetDefault() throws IppConnectException;

    /**
     * Gets printer details from CUPS. See
     * {@link IppOperationId#CUPS_GET_PRINTERS}.
     *
     * @return A list of {@link JsonProxyPrinter} objects.
     * @throws IppConnectException
     *             When IPP connection failed.
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    List<JsonProxyPrinter> cupsGetPrinters() throws IppConnectException,
            URISyntaxException, MalformedURLException;

    /**
     * All of the supported device-uri's from CUPS. See
     * {@link IppOperationId#CUPS_GET_DEVICES}.
     *
     * @return List of URI's.
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    List<URI> cupsGetDevices() throws IppConnectException, URISyntaxException;

    /**
     * All of the locally available PPD manufacturers from CUPS. See
     * {@link IppOperationId#CUPS_GET_PPDS}.
     *
     * @return List of manufacturers i.e. unique
     *         {@link IppDictPrinterDescAttr#ATTR_CUPS_PPD_MAKE} values.
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws URISyntaxException
     */
    List<String> cupsGetPPDManufacturers()
            throws IppConnectException, URISyntaxException;

    /**
     * All of the locally available PPD names of a manufacturer. See
     * {@link IppOperationId#CUPS_GET_PPDS}.
     *
     * @param manufacturer
     *            ppd-make
     *
     * @return List of PPD names of a manufacturer.
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws URISyntaxException
     */
    List<CupsAttrPPD> cupsGetPPDs(String manufacturer)
            throws IppConnectException, URISyntaxException;

    /**
     * Gets PPD file from CUPS and writes to {@link OutputStream}. See
     * {@link IppOperationId#CUPS_GET_PPD}.
     * <p>
     * Note: the PPD file follows after the end of the IPP response.
     * </p>
     *
     * @param uriPrinter
     *            Printer URI.
     * @param ppdOut
     *            PPD output stream. If {@code null} the PPD response is not
     *            processed.
     * @return {@code true} when PPD is present.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    boolean cupsGetPPD(URI uriPrinter, OutputStream ppdOut)
            throws IppConnectException;

    /**
     * Deletes a printer. See {@link IppOperationId#CUPS_DELETE_PRINTER}.
     *
     * @param uriPrinter
     *            Printer URI.
     * @return {@code true} if deleted successfully.
     * @throws IppConnectException
     *             If IPP connection fails.
     * @throws URISyntaxException
     */
    boolean cupsDeletePrinter(URI uriPrinter)
            throws IppConnectException, URISyntaxException;

    /**
     * Adds a new printer or modifies an existing printer on CUPS.
     * {@link IppOperationId#CUPS_ADD_MODIFY_PRINTER}.
     *
     * @param uriPrinter
     *            Printer URI.
     * @param printerObj
     *            Printer object attributes.
     * @throws IppConnectException
     *             If IPP connection fails.
     * @return {@code true} if added/modified successfully.
     * @throws URISyntaxException
     */
    boolean cupsAddModifyPrinter(URI uriPrinter, CupsAttrPrinter printerObj)
            throws IppConnectException, URISyntaxException;

    /**
     * @return The CUPS time (seconds from epoch) of "now" (this very moment).
     */
    int getCupsSystemTime();

    /**
     *
     * @param cupsTime
     *            The CUPS time (seconds from epoch).
     * @return The CUPS date.
     */
    Date getCupsDate(Integer cupsTime);

    /**
     * Gets the CUPS runtime version.
     *
     * @return {@code null} when not found (unknown).
     */
    String getCupsVersion();

    /**
     * Checks if PPD is present in CUPS for printer. See
     * {@link IppOperationId#CUPS_GET_PPD}.
     * <p>
     * Note: the PPD follows at the end of the IPP response but is not
     * processed.
     * </p>
     *
     * @param printerURI
     *            Printer URI.
     * @return {@code true} when PPD is present.
     * @throws IppConnectException
     *             If IPP connection fails.
     */
    boolean isCupsPpdPresent(URI printerURI) throws IppConnectException;

    /**
     * Gets the CUPS printer PPD URL for download.
     *
     * @param printerName
     *            CUPS printer name.
     * @return The URL.
     */
    URL getCupsPpdUrl(String printerName);

    // /** CUPS 1.0 : Get all of the available printer classes. */
    // CUPS_GET_CLASSES(0x4005),

    // /** CUPS 1.0 : Add or modify a printer class. */
    // CUPS_ADD_MODIFY_CLASS(0x4006),

    // /** CUPS 1.0 : Delete a printer class. */
    // CUPS_DELETE_CLASS(0x4007),

    // /** CUPS 1.0 : Accept jobs on a printer or printer class. */
    // CUPS_ACCEPT_JOBS(0x4008),

    // /** CUPS 1.0 : Reject jobs on a printer or printer class. */
    // CUPS_REJECT_JOBS(0x4009),

    // /** CUPS 1.0 : Set the default destination. */
    // CUPS_SET_DEFAULT(0x400A),

    // /** CUPS 1.1 : Move a job to a different printer. */
    // CUPS_MOVE_JOB(0x400D),

    // /** CUPS 1.2 : Authenticate a job for printing. */
    // CUPS_AUTHENTICATE_JOB(0x400E),

    // /** CUPS 1.4 : Get a document file from a job. */
    // CUPS_GET_DOCUMENT(0x4027),

    // /**
    // * CUPS 2.2 : Creates a local (temporary) print queue pointing to a remote
    // * IPP Everywhere printer.
    // */
    // CUPS_CREATE_LOCAL_PRINTER(0x4028);

}
