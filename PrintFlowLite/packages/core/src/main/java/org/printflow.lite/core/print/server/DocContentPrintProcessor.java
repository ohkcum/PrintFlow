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
package org.printflow.lite.core.print.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.printflow.lite.core.PostScriptDrmException;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpExceptionWarn;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.cometd.AdminPublisher;
import org.printflow.lite.core.cometd.PubLevelEnum;
import org.printflow.lite.core.cometd.PubTopicEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.config.OnOffEnum;
import org.printflow.lite.core.dao.PrinterDao;
import org.printflow.lite.core.dao.UserDao;
import org.printflow.lite.core.dao.enums.ACLOidEnum;
import org.printflow.lite.core.dao.enums.DocLogProtocolEnum;
import org.printflow.lite.core.dao.enums.IppQueueAttrEnum;
import org.printflow.lite.core.dao.enums.IppRoutingEnum;
import org.printflow.lite.core.dao.enums.ReservedIppQueueEnum;
import org.printflow.lite.core.dao.helpers.IppQueueHelper;
import org.printflow.lite.core.doc.DocContent;
import org.printflow.lite.core.doc.DocContentToPdfException;
import org.printflow.lite.core.doc.DocContentTypeEnum;
import org.printflow.lite.core.doc.DocInputStream;
import org.printflow.lite.core.doc.IDocFileConverter;
import org.printflow.lite.core.doc.IPostScriptConverter;
import org.printflow.lite.core.doc.IStreamConverter;
import org.printflow.lite.core.doc.PdfOptimize;
import org.printflow.lite.core.doc.PdfRepair;
import org.printflow.lite.core.doc.PdfToDecrypted;
import org.printflow.lite.core.doc.PdfToPrePress;
import org.printflow.lite.core.doc.PsToImagePdf;
import org.printflow.lite.core.doc.store.DocStoreBranchEnum;
import org.printflow.lite.core.doc.store.DocStoreException;
import org.printflow.lite.core.doc.store.DocStoreTypeEnum;
import org.printflow.lite.core.fonts.InternalFontFamilyEnum;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.ipp.IppAccessDeniedException;
import org.printflow.lite.core.ipp.operation.IppOperationId;
import org.printflow.lite.core.ipp.routing.IppRoutingListener;
import org.printflow.lite.core.jpa.Device;
import org.printflow.lite.core.jpa.DocLog;
import org.printflow.lite.core.jpa.IppQueue;
import org.printflow.lite.core.jpa.PrintIn;
import org.printflow.lite.core.jpa.Printer;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.pdf.AbstractPdfCreator;
import org.printflow.lite.core.pdf.IPdfPageProps;
import org.printflow.lite.core.pdf.PdfAbstractException;
import org.printflow.lite.core.pdf.PdfDocumentFonts;
import org.printflow.lite.core.pdf.PdfPasswordException;
import org.printflow.lite.core.pdf.PdfSecurityException;
import org.printflow.lite.core.pdf.PdfUnsupportedException;
import org.printflow.lite.core.pdf.PdfValidityException;
import org.printflow.lite.core.pdf.SpPdfPageProps;
import org.printflow.lite.core.print.proxy.ProxyPrintException;
import org.printflow.lite.core.services.AccessControlService;
import org.printflow.lite.core.services.DeviceService;
import org.printflow.lite.core.services.DocLogService;
import org.printflow.lite.core.services.DocStoreService;
import org.printflow.lite.core.services.InboxService;
import org.printflow.lite.core.services.ProxyPrintService;
import org.printflow.lite.core.services.QueueService;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.services.RateLimiterService.EndlessWaitException;
import org.printflow.lite.core.services.RateLimiterService.IEvent;
import org.printflow.lite.core.services.RateLimiterService.LimitEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.UserService;
import org.printflow.lite.core.services.helpers.DocContentPrintInInfo;
import org.printflow.lite.core.services.helpers.DocLogHelper;
import org.printflow.lite.core.services.helpers.ExternalSupplierInfo;
import org.printflow.lite.core.services.helpers.IRateLimiterListener;
import org.printflow.lite.core.services.helpers.PdfPrintInData;
import org.printflow.lite.core.services.helpers.PdfRepairEnum;
import org.printflow.lite.core.system.PdfFontsErrorValidator;
import org.printflow.lite.core.users.conf.UserAliasList;
import org.printflow.lite.core.util.FileSystemHelper;
import org.printflow.lite.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.ExceptionConverter;

/**
 * Processes a document print request. The document can be PDF, PostScripts,
 * {@link CupsCommandFile#FIRST_LINE_SIGNATURE}, or any other supported format.
 *
 * @author Rijk Ravestein
 *
 */
public final class DocContentPrintProcessor implements IRateLimiterListener {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocContentPrintProcessor.class);

    /** */
    public static final String SHORT_NAME = "PRINT-IN";

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();
    /** */
    private static final DeviceService DEVICE_SERVICE =
            ServiceContext.getServiceFactory().getDeviceService();
    /** */
    private static final DocLogService DOC_LOG_SERVICE =
            ServiceContext.getServiceFactory().getDocLogService();
    /** */
    private static final DocStoreService DOC_STORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();
    /** */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();
    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();
    /** */
    private static final PrinterDao PRINTER_DAO =
            ServiceContext.getDaoContext().getPrinterDao();
    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();
    /** */
    private static final RateLimiterService RATE_LIMITER_SERVICE =
            ServiceContext.getServiceFactory().getRateLimiterService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final int BUFFER_SIZE = 4096;

    /** */
    private java.util.UUID uuidJob = java.util.UUID.randomUUID();

    /**
     * The number of content bytes read from the input stream.
     */
    private long inputByteCount = 0;

    /** */
    private byte[] readAheadInputBytes = null;

    /** */
    private SpPdfPageProps pageProps = null;

    /**
     * Information about supplied PDF.
     */
    private PdfPrintInData suppliedPdfInfo = null;

    /** */
    private boolean drmViolationDetected = false;

    /** */
    private boolean drmRestricted = false;

    /**
     * {@code null} if no PDF document.
     */
    private PdfRepairEnum pdfRepair;

    /**
     * If {@code true} the provided {@link DocContentTypeEnum#PDF} is clean.
     */
    private boolean pdfProvidedIsClean = false;

    /** */
    private boolean pdfToCairo = false;

    /** */
    private User userDb = null;

    /** */
    private String uidTrusted = null;

    /** */
    private String mimetype;

    /** */
    private String signatureString = null;

    /** */
    private DocLogProtocolEnum docLogProtocol = null;

    /**
     * IPP operation of {@link DocLogProtocolEnum#IPP}.
     */
    private IppOperationId ippOperationId;

    /** */
    private final IppQueue queue;

    /** */
    private final ReservedIppQueueEnum reservedQueue;

    /**
     * The authenticated WebApp user: {@code null} if not present.
     */
    private final String authWebAppUser;

    /**
     * The exception which occurred during processing the request.
     */
    private Exception deferredException = null;

    /** */
    private String assignedUserId = null;

    /** */
    private String jobName;

    /** */
    private final String originatorIp;

    /** */
    private IppRoutingListener ippRoutinglistener;

    /** */
    private String originatorEmail;

    /**
     * {@link DocLog} to attach {@link PrintIn} to. Can be {@code null}.
     */
    private DocLog printInParent;

    /**
     * Creates a print server request.
     *
     * @param queue
     *            The queue to print to.
     * @param originatorIp
     *            The IP address of the requesting client.
     * @param jobName
     *            The name of the print job.
     * @param authWebAppUser
     *            The authenticated WebApp user: {@code null} if not present.
     */
    public DocContentPrintProcessor(final IppQueue queue,
            final String originatorIp, final String jobName,
            final String authWebAppUser) {

        this.jobName = jobName;
        this.queue = queue;
        this.originatorIp = originatorIp;
        this.authWebAppUser = authWebAppUser;

        if (this.queue == null) {
            this.reservedQueue = null;
        } else {
            this.reservedQueue = QUEUE_SERVICE
                    .getReservedQueue(this.getQueue().getUrlPath());
        }
    }

    /**
     * @return Listener.
     */
    public IppRoutingListener getIppRoutinglistener() {
        return ippRoutinglistener;
    }

    /**
     * @param listener
     *            Listener.
     */
    public void setIppRoutinglistener(final IppRoutingListener listener) {
        this.ippRoutinglistener = listener;
    }

    /**
     * Gets the IP address of the requesting client.
     *
     * @return {@code null} when unknown or irrelevant.
     */
    public String getOriginatorIp() {
        return originatorIp;
    }

    /**
     *
     * @return
     */
    public String getOriginatorEmail() {
        return originatorEmail;
    }

    /**
     *
     * @param bytes
     *            The bytes read.
     */
    public final void setReadAheadInputBytes(final byte[] bytes) {
        readAheadInputBytes = bytes;
    }

    /**
     * Is the queue trusted?
     *
     * @return
     */
    public boolean isTrustedQueue() {
        return this.queue != null && this.queue.getTrusted();
    }

    /**
     * Is this an authorized print job? This takes the user and the queue into
     * account, but not the IP address of the requesting user.
     *
     * @return
     */
    public boolean isAuthorized() {

        final boolean authorized =
                isTrustedUser() && (isTrustedQueue() || isAuthWebAppUser());

        if (!authorized && LOGGER.isWarnEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append("Authorized [").append(authorized).append("] :");

            msg.append(" Assigned User [").append(this.assignedUserId)
                    .append("]");

            msg.append(" Trusted User [").append(this.uidTrusted).append("]");

            //
            msg.append(" Trusted Queue [").append(this.isTrustedQueue())
                    .append("]");
            if (this.queue == null) {
                msg.append(" Reason [queue is null]");
            }

            msg.append(" Authenticatied Web App User [")
                    .append(this.authWebAppUser).append("]");

            LOGGER.warn(msg.toString());
        }
        return authorized;
    }

    /**
     * Get the uid of the Person who is currently authenticated in User WebApp
     * at same IP-address as the job was issued from.
     *
     * @return {@code null} if no user is authenticated.
     */
    public String getAuthWebAppUser() {
        return this.authWebAppUser;
    }

    /**
     * Is the authenticated WebApp User present?
     *
     * @return {@code true} if present.
     */
    public boolean isAuthWebAppUser() {
        return StringUtils.isNotBlank(this.authWebAppUser);
    }

    /**
     * Processes the assigned user, i.e. checks whether he is trusted to print a
     * job. Trust can either be direct, by alias, or by authenticated WebApp
     * User. The user (alias) must be a Person.
     * <p>
     * <b>Note</b>: On a trusted Queue (and lazy print enabled) a user is lazy
     * inserted. <i>This method has its own database transaction scope.</i>
     * </p>
     *
     * @param assignedUser
     *            Assigned user id. {@code null} if not available.
     * @param requestingUser
     *            Requesting user id (for logging purposes only).
     * @return {@code true} if we have a trusted assigned uid.
     */
    public boolean processAssignedUser(final String assignedUser,
            final String requestingUser) {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        this.assignedUserId = assignedUser;

        String uid = null;

        if (assignedUser == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        String.format("Requesting user [%s] is not assigned.",
                                requestingUser));
            }
            this.userDb = null;
            this.uidTrusted = null;

        } else {
            // Get the alias (if present).
            uid = UserAliasList.instance().getUserName(this.assignedUserId);

            if (!uid.equals(this.assignedUserId)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "Replaced assigned user [%s] alias by user [%s].",
                            this.assignedUserId, uid));
                }
            }

            this.userDb = userDao.findActiveUserByUserId(uid);

            /*
             * On a trusted queue (and lazy print enabled) we can lazy insert a
             * user...
             */
            final ConfigManager cm = ConfigManager.instance();

            if (this.userDb == null && this.isTrustedQueue()
                    && cm.isUserInsertLazyPrint()) {

                final String group =
                        cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

                ServiceContext.getDaoContext().beginTransaction();

                this.userDb = USER_SERVICE
                        .lazyInsertExternalUser(cm.getUserSource(), uid, group);

                if (this.userDb == null) {
                    ServiceContext.getDaoContext().rollback();
                } else {
                    ServiceContext.getDaoContext().commit();
                }
            }
        }

        // Do we have a (lazy inserted) database user?
        if (this.userDb == null) {
            /*
             * The user is not found in the database (no lazy insert). Try the
             * authenticated WebApp user (if present)...
             */
            this.uidTrusted = this.getAuthWebAppUser();

            if (this.uidTrusted != null) {
                this.userDb = userDao.findActiveUserByUserId(this.uidTrusted);
            }

        } else {
            this.uidTrusted = uid;
        }

        /*
         * Check authorization.
         */
        boolean isAuthorized = false;
        final String reason;

        if (this.userDb == null) {
            /*
             * No (trusted WebApp) database user.
             */
            reason = "is unknown or untrusted";

        } else {

            if (this.userDb.getPerson()) {

                final Date dateNow = new Date();

                if (USER_SERVICE.isUserPrintInDisabled(this.userDb, dateNow)) {
                    reason = "is disabled for printing";
                } else {
                    isAuthorized = true;
                    reason = null;
                }

            } else {
                reason = "is not a Person";
            }
        }

        if (isAuthorized) {

            if (!this.uidTrusted.equals(uid)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Requesting user [" + uid + "] is unknown:"
                            + " using WebApp user [" + uidTrusted + "]");
                }
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("job-name: [" + getJobName() + "]");
            }

        } else {

            if (reason != null) {

                final String protocol;
                if (this.docLogProtocol == null) {
                    protocol = "?";
                } else {
                    protocol = this.docLogProtocol.toString();
                }

                final String protocolCtx;
                if (this.ippOperationId == null) {
                    protocolCtx = "-";
                } else {
                    protocolCtx = this.ippOperationId.toString();

                }

                if (this.uidTrusted != null && !this.uidTrusted.equals(uid)) {

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "[{}] [{}] Requesting user [{}] is "
                                        + "unknown -> WebApp user [{}] {} : "
                                        + "print denied",
                                protocol, protocolCtx, uid, this.uidTrusted,
                                reason);
                    }

                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "[{}] [{}] Requesting user [{}] {}"
                                        + " : access denied to queue [{}]",
                                protocol, protocolCtx, requestingUser, reason,
                                IppQueueHelper.uiPath(this.queue));
                    }
                }
            }
            this.uidTrusted = null;
        }

        return this.uidTrusted != null;
    }

    /**
     * Writes the content to an output stream.
     * <p>
     * Note: {@link #inputByteCount} is incremented while reading.
     * </p>
     *
     * @param istr
     *            The content input.
     * @param ostr
     *            The content ouput.
     * @throws IOException
     *             When reading or writing goes wrong.
     */
    private void saveBinary(final InputStream istr, final OutputStream ostr)
            throws IOException {

        final byte[] buffer = new byte[BUFFER_SIZE];

        int noOfBytesWlk = 0;

        /*
         * Read bytes from source file and write to destination file...
         */
        while ((noOfBytesWlk = istr.read(buffer)) != -1) {
            this.inputByteCount += noOfBytesWlk;
            ostr.write(buffer, 0, noOfBytesWlk);
        }
    }

    /**
     * Streams and processes all PostScript input to the PostScript output
     * stream. An exception is throw when PostScript has copyright restrictions.
     *
     * @param istr
     *            The PostScript input stream.
     * @param ostr
     *            The PostScript ouput stream.
     * @return Result of postscript processing.
     * @throws IOException
     *             When a read/write error occurs.
     * @throws PostScriptDrmException
     *             When PostScript could not be re-distilled due to copyright
     *             restrictions. The obvious reason is when the PostScript file
     *             was created as a result of printing an encrypted PDF file. In
     *             this case the {@code ps2pdf} program fails and reports that
     *             <i>Redistilling encrypted PDF is not permitted</i>.
     */
    private PostScriptFilter.Result savePostScript(final InputStream istr,
            final OutputStream ostr)
            throws IOException, PostScriptDrmException {

        final boolean respectDRM = !ConfigManager.instance()
                .isConfigValue(IConfigProp.Key.PRINT_IN_PDF_ENCRYPTED_ALLOW);

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(istr));
                BufferedWriter writer =
                        new BufferedWriter(new OutputStreamWriter(ostr))) {

            final PostScriptFilter.Result result =
                    PostScriptFilter.process(reader, writer, respectDRM);

            switch (result.getCode()) {
            case DRM_NEGLECTED:
                this.setDrmRestricted(true);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("DRM protected PostScript from user ["
                            + uidTrusted + "] accepted");
                }
                break;
            case DRM_NO:
                break;
            case DRM_YES:
                throw new PostScriptDrmException(
                        "PostScript not accepted due to "
                                + "copyright restrictions");
            default:
                break;
            }
            return result;
        }
    }

    /**
     * @param signature
     *            Signature bytes.
     * @param reference
     *            Reference bytes.
     * @return {@code true} if signature matches reference.
     */
    private boolean isSignatureMatch(final byte[] signature,
            final byte[] reference) {
        final int max;
        if (signature.length < reference.length) {
            max = signature.length;
        } else {
            max = reference.length;
        }
        for (int i = 0; i < max; i++) {
            if (signature[i] != reference[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks or assigns the content type of the job depending on the delivery
     * protocol.
     * <p>
     * Optionally a read-ahead of the content input stream (to read the
     * signature) is performed.
     * </p>
     *
     * @param delivery
     *            The delivery protocol.
     * @param typeProvided
     *            The content type as claimed by the provider.
     * @param content
     *            The content input stream.
     * @return The assigned content type.
     * @throws IOException
     *             If IO error.
     */
    private DocContentTypeEnum checkJobContent(
            final DocLogProtocolEnum delivery,
            final DocContentTypeEnum typeProvided, final InputStream content)
            throws IOException {

        DocContentTypeEnum contentType = DocContentTypeEnum.UNKNOWN;

        this.signatureString = null;

        if (delivery.equals(DocLogProtocolEnum.IPP)) {
            /*
             * What kind of file are we getting in?
             */
            byte[] signature = new byte[4];
            content.read(signature);

            this.setReadAheadInputBytes(signature);

            this.signatureString = new String(signature);

            if (this.signatureString.startsWith(DocContent.HEADER_PDF)) {
                contentType = DocContentTypeEnum.PDF;
            } else if (this.signatureString.startsWith(DocContent.HEADER_PS)) {
                contentType = DocContentTypeEnum.PS;
            } else if (DocContent.HEADER_PJL.startsWith(this.signatureString)) {
                // Note: HEADER_PJL string length is GT signatureString.
                contentType = DocContentTypeEnum.PS;
            } else if (this.signatureString
                    .startsWith(DocContent.HEADER_UNIRAST)) {
                contentType = DocContentTypeEnum.URF;
            } else if (this.signatureString
                    .startsWith(DocContent.HEADER_PWGRAST)) {
                contentType = DocContentTypeEnum.PWG;
            } else if (this.isSignatureMatch(signature,
                    DocContent.HEADER_JPEG)) {
                contentType = DocContentTypeEnum.JPEG;
            } else if (this.signatureString
                    .startsWith(DocContent.HEADER_PDF_BANNER)) {
                contentType = DocContentTypeEnum.CUPS_PDF_BANNER;
            } else if (CupsCommandFile.isSignatureStart(this.signatureString)) {
                contentType = DocContentTypeEnum.CUPS_COMMAND;
            }

        } else if (delivery.equals(DocLogProtocolEnum.IMAP)) {
            /*
             * Do not check: accept the provided type.
             */
            contentType = typeProvided;

        } else {
            /*
             * Do not check: accept the provided type.
             */
            contentType = typeProvided;
        }

        return contentType;
    }

    /**
     * Save UnsupportedPrintJobContent for debugging purposes?
     * <p>
     * TODO: make this a {@link IConfigProp.Key}.
     * </p>
     */
    private final static boolean SAVE_UNSUPPORTED_CONTENT = false;

    /**
     * @return Hex representation of captured signature.
     */
    private String hexSignature() {
        final StringBuilder strHex = new StringBuilder();
        for (final byte ch : this.readAheadInputBytes) {
            strHex.append(String.format("%02X ", ch));
        }
        return strHex.toString().trim();
    }

    /**
     * Evaluates assigned the DocContentType and throws an exception when
     * content is NOT supported.
     * <p>
     * </p>
     *
     * @param delivery
     *            The delivery protocol.
     * @param assignedContentType
     * @param content
     * @throws IOException
     * @throws UnsupportedPrintJobContent
     */
    private void evaluateJobContent(DocLogProtocolEnum delivery,
            final DocContentTypeEnum assignedContentType,
            final InputStream content)
            throws IOException, UnsupportedPrintJobContent {

        UnsupportedPrintJobContent formatException = null;
        FileOutputStream fostr = null;

        try {
            if (assignedContentType == DocContentTypeEnum.UNKNOWN) {

                formatException = new UnsupportedPrintJobContent(
                        "header [" + this.hexSignature() + "] unknown");

                if (SAVE_UNSUPPORTED_CONTENT) {
                    fostr = new FileOutputStream(
                            ConfigManager.getAppTmpDir() + "/" + delivery + "_"
                                    + System.currentTimeMillis() + ".unknown");
                    fostr.write(this.readAheadInputBytes);
                    saveBinary(content, fostr);
                }

            } else if (!DocContent.isSupported(assignedContentType)) {

                formatException =
                        new UnsupportedPrintJobContent("Content type ["
                                + assignedContentType + "] NOT supported.");

            }
        } finally {
            if (fostr != null) {
                fostr.close();
            }
        }

        if (formatException != null) {
            throw formatException;
        }

    }

    /**
     * @param cm
     *            {@link ConfigManager}.
     * @param tempDirApp
     *            Directory for temporary files.
     * @param isDriverPrint
     *            {@code true} if Driver Print.
     * @return {@link IPostScriptConverter}.
     */
    private IPostScriptConverter createPostScriptToImageConverter(
            final ConfigManager cm, final String tempDirApp,
            final boolean isDriverPrint) {

        final int dpi;

        if (isDriverPrint) {
            dpi = cm.getConfigInt(Key.PRINT_IN_PS_DRIVER_IMAGES_DPI);
        } else {
            dpi = cm.getConfigInt(Key.PRINT_IN_PS_DRIVERLESS_IMAGES_DPI);
        }

        return new PsToImagePdf(new File(tempDirApp), dpi, this.jobName, Objects
                .toString(this.getUserDb().getFullName(), this.assignedUserId));
    }

    /**
     * Processes content to be printed as offered on the input stream, writes a
     * {@link DocLog}, and places the resulting PDF in the user's inbox.
     * <p>
     * When this is a {@link DocLogProtocolEnum#RAW} print it is assumed that
     * the input stream header is already validated.
     * </p>
     *
     * @param istrContent
     *            The input stream containing the content to be printed.
     * @param supplierInfo
     *            {@link ExternalSupplierInfo} (can be {@code null}).
     * @param protocol
     *            The originating printing protocol.
     * @param originatorEmailAddr
     *            MUST be present for {@link DocLogProtocolEnum#IMAP}. For all
     *            other protocols {@code null}.
     * @param contentTypeProvided
     *            The content type as claimed by the provider. This parameter is
     *            {@code null} and ignored when protocol is
     *            {@link DocLogProtocolEnum#IPP}.
     * @param preferredOutputFont
     *            The preferred font for the PDF output. This parameter is
     *            {@code null} when (user) preference is unknown or irrelevant.
     * @throws IOException
     *             If IO errors.
     */
    public void process(final InputStream istrContent,
            final ExternalSupplierInfo supplierInfo,
            final DocLogProtocolEnum protocol, final String originatorEmailAddr,
            final DocContentTypeEnum contentTypeProvided,
            final InternalFontFamilyEnum preferredOutputFont)
            throws IOException {

        if (!isTrustedUser()) {
            return;
        }

        this.originatorEmail = originatorEmailAddr;

        final DocContentTypeEnum inputType =
                checkJobContent(protocol, contentTypeProvided, istrContent);

        // Skip CUPS_COMMAND for now.
        if (inputType == DocContentTypeEnum.CUPS_COMMAND) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(CupsCommandFile.FIRST_LINE_SIGNATURE + " ignored");
            }
            return;
        }

        //
        final String homeDir = ConfigManager.getUserHomeDir(this.uidTrusted);
        final String tempDirApp = ConfigManager.getAppTmpDir();

        //
        USER_SERVICE.lazyUserHomeDir(userDb);

        FileOutputStream fostrContent = null;

        this.inputByteCount = 0;

        final List<File> filesCreated = new ArrayList<>();
        final List<File> files2Delete = new ArrayList<>();

        final ConfigManager cm = ConfigManager.instance();

        try {
            /*
             * Keep the job content evaluation WITHIN the try block, so the
             * exception is handled appropriately.
             */
            this.evaluateJobContent(protocol, inputType, istrContent);

            this.setMimetype(DocContent.getMimeType(inputType));

            /*
             * The basename of the resulting file WITHOUT extension.
             */
            final String jobFileBase =
                    String.format("%s%c", this.uuidJob.toString(), '.');

            /*
             * The basename of the resulting PDF file.
             */
            final String jobFileBasePdf = String.format("%s%s", jobFileBase,
                    DocContent.FILENAME_EXT_PDF);

            /*
             * File to receive the input stream content.
             */
            final File contentFile = new File(String.format("%s%c%s%s",
                    tempDirApp, File.separatorChar, jobFileBase,
                    DocContent.getFileExtension(inputType)));

            /*
             * Create the file.
             */
            fostrContent = new FileOutputStream(contentFile);

            final OnOffEnum detainPostScript;

            if (inputType == DocContentTypeEnum.PS
                    && protocol.isDriverPrint()) {
                detainPostScript = cm.getConfigEnum(OnOffEnum.class,
                        Key.PRINT_IN_PS_DRIVER_DETAIN);
            } else {
                detainPostScript = OnOffEnum.OFF;
            }

            /*
             * Administer the just created file as created or 2delete.
             */
            if (inputType == DocContentTypeEnum.PDF) {
                filesCreated.add(contentFile);
            } else {
                // Wait for PDF conversion outcome:
                // do not delete PostScript file yet.
                if (detainPostScript == OnOffEnum.OFF) {
                    files2Delete.add(contentFile);
                }
            }

            /*
             * Write the saved pre-processed input bytes to the output stream.
             */
            if (this.readAheadInputBytes != null) {
                this.inputByteCount += this.readAheadInputBytes.length;
                fostrContent.write(this.readAheadInputBytes);
            }

            /*
             * Be optimistic about PostScript and PDF content.
             */
            this.setDrmViolationDetected(false);
            this.setDrmRestricted(false);

            this.pdfToCairo = false;
            this.pdfRepair = null;

            /*
             * Document content converters are needed for non-PDF content.
             */
            IStreamConverter streamConverter = null;
            IDocFileConverter fileConverter = null;
            IPostScriptConverter postScriptConverter = null;

            /*
             * Directly write (rest of) offered content to PostScript or PDF, or
             * convert to PDF.
             */
            if (inputType == DocContentTypeEnum.PDF) {

                this.pdfRepair = PdfRepairEnum.NONE;
                this.saveBinary(istrContent, fostrContent);

            } else if (inputType == DocContentTypeEnum.PS) {
                /*
                 * An exception is throw upon a DRM violation.
                 */
                final PostScriptFilter.Result result =
                        this.savePostScript(istrContent, fostrContent);

                // Integrate IPP options from result with supplierInfo.
                DocLogHelper.updateExternalSupplierInfo(supplierInfo, result);

                if (protocol.isDriverPrint()) {
                    if (OnOffEnum.ON == cm.getConfigEnum(OnOffEnum.class,
                            Key.PRINT_IN_PS_DRIVER_IMAGES_TRIGGER)) {
                        postScriptConverter =
                                this.createPostScriptToImageConverter(cm,
                                        tempDirApp, true);
                    }
                } else {
                    if (OnOffEnum.ON == cm.getConfigEnum(OnOffEnum.class,
                            Key.PRINT_IN_PS_DRIVERLESS_IMAGES_TRIGGER)) {
                        postScriptConverter =
                                this.createPostScriptToImageConverter(cm,
                                        tempDirApp, false);
                    }
                }

                if (postScriptConverter == null) {
                    /*
                     * Always use a file converter,
                     */
                    fileConverter =
                            DocContent.createPdfFileConverter(inputType);
                }

            } else if (inputType == DocContentTypeEnum.CUPS_PDF_BANNER) {

                streamConverter = DocContent.createPdfStreamConverter(inputType,
                        preferredOutputFont);

            } else if (inputType == DocContentTypeEnum.HTML) {

                // If available, file converter is preferred.
                fileConverter = DocContent.createPdfFileConverter(inputType);

                if (fileConverter == null) {
                    streamConverter = DocContent.createPdfStreamConverter(
                            inputType, preferredOutputFont);
                } else {
                    this.saveBinary(istrContent, fostrContent);
                }

            } else {

                if (!protocol.isDriverPrint()) {
                    streamConverter = DocContent.createPdfStreamConverter(
                            inputType, preferredOutputFont);
                }

                if (streamConverter == null) {

                    fileConverter =
                            DocContent.createPdfFileConverter(inputType);

                    if (fileConverter != null) {
                        this.saveBinary(istrContent, fostrContent);
                    }
                }
            }

            /*
             * Convert to PDF with a stream converter?
             */
            if (streamConverter != null) {
                /*
                 * INVARIANT: no read-ahead on the input content stream.
                 */
                final DocInputStream istrDoc = new DocInputStream(istrContent);
                this.inputByteCount = streamConverter.convert(inputType,
                        istrDoc, fostrContent);
            }

            /*
             * Path of the PDF file BEFORE it is moved to its final destination.
             * As a DEFAULT we use the path of the streamed content.
             */
            String tempPathPdf = contentFile.getAbsolutePath();

            /*
             * We're done with capturing the content input stream, so close the
             * file output stream.
             */
            fostrContent.close();
            fostrContent = null;

            /*
             * Convert to PDF with a FILE converter?
             */
            if (fileConverter != null) {
                this.inputByteCount = contentFile.length();

                final File pdfOutputFile =
                        fileConverter.convert(inputType, contentFile);

                /*
                 * Retry with PostScript converter?
                 */
                if (inputType == DocContentTypeEnum.PS
                        && fileConverter.hasStdErrMsg()) {

                    if (protocol.isDriverPrint()) {
                        if (OnOffEnum.AUTO == cm.getConfigEnum(OnOffEnum.class,
                                Key.PRINT_IN_PS_DRIVER_IMAGES_TRIGGER)) {
                            postScriptConverter =
                                    this.createPostScriptToImageConverter(cm,
                                            tempDirApp, true);
                        }
                    } else {
                        if (OnOffEnum.AUTO == cm.getConfigEnum(OnOffEnum.class,
                                Key.PRINT_IN_PS_DRIVERLESS_IMAGES_TRIGGER)) {
                            postScriptConverter =
                                    this.createPostScriptToImageConverter(cm,
                                            tempDirApp, false);
                        }
                    }

                    if (postScriptConverter == null) {
                        filesCreated.add(pdfOutputFile);
                    } else {
                        pdfOutputFile.delete();
                    }
                }
                tempPathPdf = pdfOutputFile.getAbsolutePath();
            }

            /*
             * Convert to PDF with PostScript converter?
             */
            if (postScriptConverter != null) {
                final File pdfOutputFile =
                        postScriptConverter.convert(contentFile);
                filesCreated.add(pdfOutputFile);
                tempPathPdf = pdfOutputFile.getAbsolutePath();
            }

            /*
             * Calculate number of pages, etc. and repair along the way.
             */
            final SpPdfPageProps pdfPageProps =
                    this.createPdfPageProps(tempPathPdf);

            this.setPageProps(pdfPageProps);

            //
            if (inputType == DocContentTypeEnum.PDF) {

                this.setSuppliedPdfInfo(PdfPrintInData
                        .create(AbstractPdfCreator.createPdfInfo(tempPathPdf)));

                final File fileWrk = new File(tempPathPdf);

                // Copy the incoming PDF before optimize/repair.
                final File fileWrkAtTheStart =
                        new File(fileWrk.getAbsolutePath().concat(".pdf"));
                FileUtils.copyFile(fileWrk, fileWrkAtTheStart);
                files2Delete.add(fileWrkAtTheStart);

                // Here we go ...
                if (cm.isConfigValue(Key.PRINT_IN_PDF_OPTIMIZE)) {
                    this.optimizePdf(fileWrk, fileWrk);
                }
                if (cm.isConfigValue(Key.PRINT_IN_PDF_FONTS_VERIFY)) {
                    this.verifyPdfFonts(fileWrk, cm.isConfigValue(
                            Key.PRINT_IN_PDF_FONTS_VERIFY_REJECT));
                }
                if (!this.pdfToCairo
                        && cm.isConfigValue(Key.PRINT_IN_PDF_FONTS_EMBED)) {
                    this.embedPdfFonts(fileWrk);
                }
                if (!this.pdfToCairo && !this.pdfProvidedIsClean
                        && cm.isConfigValue(Key.PRINT_IN_PDF_CLEAN)) {
                    this.cleanPdf(fileWrk);
                }
                if (cm.isConfigValue(Key.PRINT_IN_PDF_PREPRESS)) {
                    this.cleanPdfPrepress(fileWrk);
                }
                this.checkPdfEndResult(fileWrk, fileWrkAtTheStart);

            } else if (fileConverter != null && (fileConverter.hasStdErrMsg()
                    || detainPostScript == OnOffEnum.ON)) {

                final StringBuilder msg = new StringBuilder();
                msg.append("User \"").append(this.uidTrusted).append("\" ")
                        .append(fileConverter.getClass().getSimpleName());

                if (fileConverter.hasStdErrMsg()) {
                    msg.append(" errors.");
                } else {
                    msg.append(".");
                }

                final PubLevelEnum pubLevel;

                if (postScriptConverter == null) {
                    if (detainPostScript == OnOffEnum.OFF) {
                        pubLevel = PubLevelEnum.ERROR;
                        msg.append(" Rendering invalid.");
                    } else {
                        pubLevel = PubLevelEnum.WARN;
                    }
                } else {
                    pubLevel = PubLevelEnum.WARN;
                    msg.append(" Pages rendered as images.");
                }

                if (detainPostScript == OnOffEnum.ON
                        || (detainPostScript == OnOffEnum.AUTO
                                && fileConverter.hasStdErrMsg())) {
                    msg.append(" (PostScript file is detained).");
                }
                AdminPublisher.instance().publish(PubTopicEnum.USER, pubLevel,
                        msg.toString());
            }

            // Check again...
            if (detainPostScript == OnOffEnum.AUTO && (fileConverter == null
                    || !fileConverter.hasStdErrMsg())) {
                // No stderr: delete PostScript file after all.
                files2Delete.add(contentFile);
            }

            /*
             * STEP 1: Log in Database: BEFORE the file MOVE.
             */
            final DocContentPrintInInfo printInInfo =
                    this.logPrintIn(protocol, supplierInfo);

            /*
             * STEP 2: PDF to Doc Store?
             */
            final File pdfTempFile = new File(tempPathPdf);

            final boolean isDocJournal =
                    this.processDocJournal(printInInfo, pdfTempFile);

            /*
             * STEP 3: IPP Routing?
             */
            final boolean isIppRouting =
                    this.processIppRouting(printInInfo, pdfTempFile);

            /*
             * STEP 4: Move to user safepages home?
             */
            final boolean isMailPrintTicket =
                    isDocJournal && this.isMailPrintTicket();

            if (isIppRouting || isMailPrintTicket) {
                files2Delete.add(pdfTempFile);
            } else {
                // See Mantis #1167 : "touch" PDF file.
                pdfTempFile.setLastModified(System.currentTimeMillis());

                final Path pathTarget = FileSystems.getDefault()
                        .getPath(homeDir, jobFileBasePdf);

                FileSystemHelper.doAtomicFileMove(//
                        FileSystems.getDefault().getPath(tempPathPdf),
                        pathTarget);
                /*
                 * Start task to create the shadow EcoPrint PDF file?
                 */
                if (ConfigManager.isEcoPrintEnabled() && this.getPageProps()
                        .getNumberOfPages() <= cm.getConfigInt(
                                Key.ECO_PRINT_AUTO_THRESHOLD_SHADOW_PAGE_COUNT)) {
                    INBOX_SERVICE.startEcoPrintPdfTask(homeDir,
                            pathTarget.toFile(), this.uuidJob);
                }
            }

        } catch (Exception e) {

            if (e instanceof PostScriptDrmException) {

                setDrmRestricted(true);
                setDrmViolationDetected(true);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("DRM protected PostScript from user ["
                            + uidTrusted + "] REJECTED");
                }

                /*
                 * We also need to log the rejected print-in, since we want to
                 * notify the result in the User WebApp.
                 */
                this.logPrintIn(protocol, supplierInfo);

            } else {

                if (e instanceof PdfValidityException) {
                    if (this.pdfRepair == null
                            || !this.pdfRepair.isRepairFail()) {
                        this.pdfRepair = PdfRepairEnum.DOC_FAIL;
                    }
                    this.logPrintIn(protocol, supplierInfo);
                }
                /*
                 * Save the exception, so it can be thrown at the end of the
                 * parent operation.
                 */
                setDeferredException(e);
            }

            /*
             * Clean up any created files.
             */
            for (final File file : filesCreated) {
                if (file.exists()) {
                    file.delete();
                }
            }

        } finally {

            if (fostrContent != null) {
                fostrContent.close();
            }

            for (final File file : files2Delete) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Validates and optionally repairs PDF file for font errors.
     *
     * @param pdf
     *            PDF file.
     * @param throwException
     *            If true, throw exception if font errors/warnings.
     * @throws PdfValidityException
     *             If font errors/warnings in PDF document.
     * @throws IOException
     *             When file IO error.
     */
    private void verifyPdfFonts(final File pdf, final boolean throwException)
            throws PdfValidityException, IOException {

        final PdfFontsErrorValidator validator =
                new PdfFontsErrorValidator(pdf);

        if (!validator.execute()) {

            if (!this.pdfToCairo) {

                final PdfRepair converter = new PdfRepair();
                FileSystemHelper.replaceWithNewVersion(pdf,
                        converter.convert(pdf));
                this.pdfToCairo = true;

                // Try again.
                if (validator.execute()) {
                    this.pdfRepair = PdfRepairEnum.FONT;
                    return;
                }
                this.pdfRepair = PdfRepairEnum.FONT_FAIL;
            }
            if (throwException) {
                throw new PdfValidityException("Font errors.",
                        PhraseEnum.PDF_INVALID
                                .uiText(ServiceContext.getLocale()),
                        PhraseEnum.PDF_INVALID);
            }
        }
    }

    /**
     * Embeds non-standard fonts in PDF file.
     *
     * @param pdf
     *            PDF file.
     * @throws PdfValidityException
     *             When embed font error(s).
     */
    private void embedPdfFonts(final File pdf) throws PdfValidityException {

        final PdfDocumentFonts fonts;

        try {
            fonts = PdfDocumentFonts.create(pdf);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }

        if (fonts.isAllEmbeddedOrStandard()) {
            return;
        }

        final PdfRepair converter = new PdfRepair();

        try {
            FileSystemHelper.replaceWithNewVersion(pdf, converter.convert(pdf));
            this.pdfToCairo = true;
            if (converter.hasStdout()) {
                this.pdfRepair = PdfRepairEnum.DOC;
            }
        } catch (IOException e) {
            this.pdfRepair = PdfRepairEnum.DOC_FAIL;
            throw new PdfValidityException("Embed Font errors.",
                    PhraseEnum.PDF_INVALID.uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_INVALID);
        }
    }

    /**
     * Cleans a PDF file.
     *
     * @param pdf
     *            PDF file.
     * @throws PdfValidityException
     *             When error(s).
     */
    private void cleanPdf(final File pdf) throws PdfValidityException {

        final PdfRepair converter = new PdfRepair();

        try {
            FileSystemHelper.replaceWithNewVersion(pdf, converter.convert(pdf));
            this.pdfToCairo = true;
            if (converter.hasStdout()) {
                this.pdfRepair = PdfRepairEnum.DOC;
            }
        } catch (IOException e) {
            this.pdfRepair = PdfRepairEnum.DOC_FAIL;
            throw new PdfValidityException("PDF cleaning errors.",
                    PhraseEnum.PDF_INVALID.uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_INVALID);
        }
    }

    /**
     * Cleans a PDF file by executing Ghostscript prepress.
     *
     * @param pdf
     *            PDF file.
     * @throws PdfValidityException
     *             When error(s).
     */
    private void cleanPdfPrepress(final File pdf) throws PdfValidityException {

        final PdfToPrePress converter = new PdfToPrePress();

        try {
            FileSystemHelper.replaceWithNewVersion(pdf, converter.convert(pdf));
            if (converter.hasStdout()) {
                this.pdfRepair = PdfRepairEnum.DOC;
            }
        } catch (IOException e) {
            this.pdfRepair = PdfRepairEnum.DOC_FAIL;
            throw new PdfValidityException("PDF prepress errors.",
                    PhraseEnum.PDF_INVALID.uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_INVALID);
        }
    }

    /**
     * Optimizes a PDF file by executing {@link PdfOptimize} and moves the
     * result to the target file.
     *
     * @param targetFile
     *            The target file.
     * @param fileToOptimize
     *            The source file to optimize: the optimized result is
     *            <i>moved</i> to the target file.
     * @throws PdfValidityException
     *             If error(s).
     */
    private void optimizePdf(final File targetFile, final File fileToOptimize)
            throws PdfValidityException {

        final PdfOptimize converter = new PdfOptimize();
        try {
            FileSystemHelper.replaceWithNewVersion(targetFile,
                    converter.convert(DocContentTypeEnum.PDF, fileToOptimize));
            if (converter.hasStdout()) {
                this.pdfRepair = PdfRepairEnum.DOC;
            }
        } catch (IOException | DocContentToPdfException
                | UnavailableException e) {

            this.pdfRepair = PdfRepairEnum.DOC_FAIL;

            throw new PdfValidityException("PDF optimization errors.",
                    PhraseEnum.PDF_INVALID.uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_INVALID);
        }
    }

    /**
     * Checks a PDF by reading the page properties. If the check fails, a
     * {@link PdfOptimize} repair is tried.
     *
     * @param fileToCheck
     *            The PDF to check.
     * @param fileToOptimize
     *            The initial PDF as optimization candidate.
     * @throws PdfValidityException
     *             If error(s).
     */
    private void checkPdfEndResult(final File fileToCheck,
            final File fileToOptimize) throws PdfValidityException {
        try {
            SpPdfPageProps.create(fileToCheck.getAbsolutePath());
        } catch (PdfValidityException | PdfSecurityException
                | PdfPasswordException | PdfUnsupportedException e) {
            this.optimizePdf(fileToCheck, fileToOptimize);
        }
    }

    /**
     * Creates PDF page properties, and optionally repairs or decrypts PDF.
     *
     * @param tempPathPdf
     *            The PDF file path.
     * @return {@link SpPdfPageProps}.
     * @throws PdfValidityException
     *             When invalid PDF document.
     * @throws PdfSecurityException
     *             When encrypted PDF document.
     * @throws IOException
     *             When file IO error.
     * @throws PdfPasswordException
     *             When password protected PDF document.
     * @throws PdfUnsupportedException
     *             When unsupported PDF document.
     */
    private SpPdfPageProps createPdfPageProps(final String tempPathPdf)
            throws PdfValidityException, PdfSecurityException, IOException,
            PdfPasswordException, PdfUnsupportedException {

        SpPdfPageProps pdfPageProps;

        try {
            pdfPageProps = SpPdfPageProps.create(tempPathPdf);
        } catch (PdfValidityException e) {

            if (ConfigManager.instance().isConfigValue(
                    IConfigProp.Key.PRINT_IN_PDF_INVALID_REPAIR)) {

                this.pdfRepair = PdfRepairEnum.DOC_FAIL;

                final File pdfFile = new File(tempPathPdf);

                // Convert ...
                try {
                    final PdfRepair converter = new PdfRepair();
                    FileSystemHelper.replaceWithNewVersion(pdfFile,
                            converter.convert(pdfFile));
                } catch (IOException ignore) {
                    throw new PdfValidityException(e.getMessage(),
                            PhraseEnum.PDF_REPAIR_FAILED
                                    .uiText(ServiceContext.getLocale()),
                            PhraseEnum.PDF_REPAIR_FAILED);
                }
                // and try again.
                pdfPageProps = SpPdfPageProps.create(tempPathPdf);

                this.pdfRepair = PdfRepairEnum.DOC;
                this.pdfToCairo = true;
            } else {
                throw e;
            }

        } catch (PdfSecurityException e) {

            if (e.isPrintingAllowed()
                    && ConfigManager.instance().isConfigValue(
                            IConfigProp.Key.PRINT_IN_PDF_ENCRYPTED_ALLOW)
                    && PdfToDecrypted.isAvailable()) {

                final File pdfFile = new File(tempPathPdf);

                // Convert ...
                FileSystemHelper.replaceWithNewVersion(pdfFile,
                        new PdfToDecrypted().convert(pdfFile));

                // and try again.
                pdfPageProps = SpPdfPageProps.create(tempPathPdf);

                this.setDrmRestricted(true);

            } else {
                throw e;
            }
        }

        return pdfPageProps;
    }

    /**
     * Logs the PrintIn job.
     * <p>
     * <b>IMPORTANT</b>: This method has it <u>own transaction scope</u>.
     * </p>
     *
     * @param protocol
     *            The {@link DocLogProtocolEnum}.
     * @param supplierInfo
     *            {@link {@link ExternalSupplierInfo}} (can be {@code null}).
     * @return {@link DocContentPrintInInfo}.
     */
    private DocContentPrintInInfo logPrintIn(final DocLogProtocolEnum protocol,
            final ExternalSupplierInfo supplierInfo) {

        final DocContentPrintInInfo printInInfo = new DocContentPrintInInfo();

        printInInfo.setPrintInDate(ServiceContext.getTransactionDate());

        printInInfo.setDrmRestricted(this.isDrmRestricted());
        printInInfo.setPdfRepair(this.pdfRepair);
        printInInfo.setJobBytes(this.getJobBytes());
        printInInfo.setJobName(this.getJobName());
        printInInfo.setMimetype(this.getMimetype());
        printInInfo.setOriginatorEmail(this.getOriginatorEmail());
        printInInfo.setOriginatorIp(this.getOriginatorIp());
        printInInfo.setPageProps(this.getPageProps());
        printInInfo.setSuppliedPdfInfo(this.getSuppliedPdfInfo());
        printInInfo.setUuidJob(this.getUuidJob());
        printInInfo.setSupplierInfo(supplierInfo);

        if (this.printInParent == null) {
            DOC_LOG_SERVICE.logPrintIn(this.getUserDb(), this.getQueue(),
                    protocol, printInInfo);
        } else {
            DOC_LOG_SERVICE.attachPrintIn(this.printInParent, this.getUserDb(),
                    this.getQueue(), protocol, printInInfo);
        }

        return printInInfo;
    }

    /**
     * Stores PDF file into the Journal branch of the Document Store, if this
     * branch is enabled and user has permission to journal their SavePages.
     *
     * @param printInInfo
     *            {@link PrintIn} information.
     * @param pdfFile
     *            The PDF document to route.
     * @return {@code true} if PDF was stored in Document Store, {@code false}
     *         if store/branch is disabled..
     * @throws DocStoreException
     */
    private boolean processDocJournal(final DocContentPrintInInfo printInInfo,
            final File pdfFile) throws DocStoreException {

        final DocStoreTypeEnum store = DocStoreTypeEnum.JOURNAL;

        if (DOC_STORE_SERVICE.isEnabled(store, DocStoreBranchEnum.IN_PRINT)
                && !QUEUE_SERVICE.isDocStoreJournalDisabled(this.getQueue())
                && this.userDb != null && ACCESS_CONTROL_SERVICE
                        .hasAccess(userDb, ACLOidEnum.U_QUEUE_JOURNAL)) {
            DOC_STORE_SERVICE.store(store, printInInfo, pdfFile);
            return true;
        }
        return false;
    }

    /**
     * Processes IPP Routing, if applicable.
     *
     * @param printInInfo
     *            {@link PrintIn} information.
     * @param pdfFile
     *            The PDF document to route.
     * @return {@code true} if IPP routing was applied, {@code false} if not.
     */
    private boolean processIppRouting(final DocContentPrintInInfo printInInfo,
            final File pdfFile) {

        if (!ConfigManager.instance().isConfigValue(Key.IPP_ROUTING_ENABLE)) {
            return false;
        }

        if (StringUtils.isBlank(this.originatorIp)
                || QUEUE_SERVICE.isReservedQueue(this.queue.getUrlPath())) {
            return false;
        }

        /*
         * Use a new queue instance to prevent
         * org.hibernate.LazyInitializationException.
         */
        final IppQueue queueWrk = ServiceContext.getDaoContext()
                .getIppQueueDao().findById(this.queue.getId());

        final IppRoutingEnum routing = QUEUE_SERVICE.getIppRouting(queueWrk);

        if (routing == null || routing == IppRoutingEnum.NONE) {
            return false;
        }

        Printer targetPrinter = null;
        String warnMsg = null;

        if (routing == IppRoutingEnum.PRINTER) {

            final String printerName = QUEUE_SERVICE.getAttrValue(queue,
                    IppQueueAttrEnum.IPP_ROUTING_PRINTER_NAME);
            targetPrinter = PRINTER_DAO.findByName(printerName);

            if (targetPrinter == null) {
                warnMsg = String.format(": printer %s not found.", printerName);
            }

        } else if (routing == IppRoutingEnum.TERMINAL) {

            final Device terminal =
                    DEVICE_SERVICE.getHostTerminal(this.originatorIp);

            if (terminal == null) {
                warnMsg = ": terminal not found.";
            } else if (BooleanUtils.isTrue(terminal.getDisabled())) {
                warnMsg = ": terminal disabled.";
            } else {
                targetPrinter = terminal.getPrinter();
                if (targetPrinter == null) {
                    warnMsg = ": no printer on terminal.";
                }
            }
        } else {
            throw new SpException(String.format(
                    "IPP Routing [%s] is not supported", routing.toString()));
        }

        if (warnMsg == null) {
            if (BooleanUtils.isTrue(targetPrinter.getDisabled())) {
                warnMsg = String.format(": printer %s disabled.",
                        targetPrinter.getPrinterName());
            }
        }

        if (warnMsg != null) {
            final String msg =
                    String.format("IPP Routing [%s] of Queue /%s from %s %s",
                            routing.toString(), queueWrk.getUrlPath(),
                            this.originatorIp, warnMsg);
            AdminPublisher.instance().publish(PubTopicEnum.PROXY_PRINT,
                    PubLevelEnum.WARN, msg);
            LOGGER.warn(msg);
            return false;
        }

        try {
            PROXYPRINT_SERVICE.proxyPrintIppRouting(this.userDb, queueWrk,
                    targetPrinter, printInInfo, pdfFile,
                    this.ippRoutinglistener);
        } catch (ProxyPrintException e) {
            throw new SpExceptionWarn(e.getMessage(), e);
        }

        return true;
    }

    /**
     * @param docLog
     *            {@link DocLog} to attach {@link PrintIn} to. Can be
     *            {@code null}.
     */
    public void setPrintInParent(final DocLog docLog) {
        this.printInParent = docLog;
    }

    /**
     * @return
     */
    public java.util.UUID getUuidJob() {
        return uuidJob;
    }

    public void setUuidJob(java.util.UUID uuidJob) {
        this.uuidJob = uuidJob;
    }

    public long getJobBytes() {
        return inputByteCount;
    }

    public IPdfPageProps getPageProps() {
        return pageProps;
    }

    public int getNumberOfPages() {
        return pageProps.getNumberOfPages();
    }

    private void setPageProps(final SpPdfPageProps pageProps) {
        this.pageProps = pageProps;
    }

    /**
     * @return Information about supplied PDF.
     */
    public PdfPrintInData getSuppliedPdfInfo() {
        return suppliedPdfInfo;
    }

    /**
     * @param pdfInfo
     *            Information about supplied PDF.
     */
    private void setSuppliedPdfInfo(final PdfPrintInData pdfInfo) {
        this.suppliedPdfInfo = pdfInfo;
    }

    public String getMimetype() {
        return mimetype;
    }

    private void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * The user object from the database representing the user who printed this
     * job.
     *
     * @return {@code null} when unknown.
     */
    public User getUserDb() {
        return userDb;
    }

    /**
     * @param id
     *            IPP operation of {@link DocLogProtocolEnum#IPP}.
     */
    public void setIppOperationId(final IppOperationId id) {
        this.ippOperationId = id;
    }

    /**
     * @param protocol
     */
    public void setDocLogProtocol(final DocLogProtocolEnum protocol) {
        this.docLogProtocol = protocol;
    }

    public boolean isDrmViolationDetected() {
        return drmViolationDetected;
    }

    public void setDrmViolationDetected(boolean drmViolationDetected) {
        this.drmViolationDetected = drmViolationDetected;
    }

    public boolean isDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(boolean restricted) {
        drmRestricted = restricted;
    }

    public boolean isPdfRepaired() {
        return this.pdfRepair != null && this.pdfRepair.isRepaired();
    }

    public boolean isPdfFontFail() {
        return this.pdfRepair != null && this.pdfRepair.isFontFail();
    }

    /**
     * Checks if we have a trusted userid who is allowed to print.
     *
     * @return {@code true} if we have a trusted user.
     */
    public boolean isTrustedUser() {
        return this.uidTrusted != null;
    }

    /**
     * @return {@code true} if content is a processed as MailPrint Ticket.
     */
    private boolean isMailPrintTicket() {
        return this.reservedQueue != null
                && this.reservedQueue == ReservedIppQueueEnum.MAILPRINT
                && ConfigManager.isMailPrintTicketingEnabled(this.getUserDb());
    }

    public Exception getDeferredException() {
        return deferredException;
    }

    public void setDeferredException(Exception deferredException) {
        this.deferredException = deferredException;
    }

    public boolean hasDeferredException() {
        return deferredException != null;
    }

    /**
     *
     * @return
     */
    public String getJobName() {
        return jobName;
    }

    /**
     *
     */
    public void setJobName(final String name) {
        this.jobName = name;
    }

    /**
     *
     * @return
     */
    public IppQueue getQueue() {
        return queue;
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * application is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), key, args);
    }

    /**
     * Evaluates (and adapts) the error state of the print job processed by this
     * request.
     * <p>
     * When a deferred exception is present or the user is not authorized to
     * print this job, messages are logged and send to the
     * {@link AdminPublisher}.
     * </p>
     * <p>
     * NOTE: Some "legal" (and other) deferred exceptions like
     * {@link PostScriptDrmException}, {@link PdfSecurityException} and
     * {@link UnsupportedPrintJobContent} are nullified after message handling,
     * i.e. {@link #getDeferredException()} will return {@code null} after this
     * method is performed.
     * </p>
     *
     * @param isAuthorized
     *            {@code true} when requesting user is authorized.
     * @param requestingUserId
     *            Requesting user id (for logging purposes only).
     */
    public void evaluateErrorState(final boolean isAuthorized,
            final String requestingUserId) {

        final Exception exception = getDeferredException();

        if (exception == null && isAuthorized) {
            return;
        }

        this.onRateLimiting();

        final String urlQueue = IppQueueHelper.uiPath(this.queue);
        final String userid;

        if (getUserDb() == null) {
            userid = StringUtils.defaultString(getAuthWebAppUser());
        } else {
            userid = getUserDb().getUserId();
        }

        final String pubMessage;
        PubLevelEnum pubLevel = PubLevelEnum.WARN;
        boolean logWarnPrintDenied = false;

        if (exception == null) {

            pubLevel = PubLevelEnum.INFO;
            pubMessage = localize("print-in-denied-authentication");

        } else {

            if (exception instanceof PostScriptDrmException) {

                pubMessage = exception.getMessage();

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                            "User [%s] Distilling PostScript to PDF : %s",
                            userid, exception.getMessage()));
                }
                setDeferredException(null);

            } else if ((exception instanceof PdfAbstractException)) {

                pubMessage = ((PdfAbstractException) exception).getLogMessage();

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            String.format("User [%s]: %s", userid, pubMessage));
                }
                setDeferredException(null);

            } else if (exception instanceof UnsupportedPrintJobContent) {

                pubMessage = exception.getMessage();

                if (LOGGER.isWarnEnabled()) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format(
                                "Unsupported Print Content from user [%s]: %s",
                                userid, exception.getMessage()));
                    }
                }
                setDeferredException(null);

            } else if (exception instanceof IppAccessDeniedException) {

                pubMessage = exception.getMessage();

                if (LOGGER.isWarnEnabled()) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("IPP access denied for user [{}]: {}",
                                this.assignedUserId, exception.getMessage());
                    }
                }
                setDeferredException(null);

            } else {

                pubMessage = exception.getMessage();

                if ((exception instanceof org.xhtmlrenderer.util.XRRuntimeException)
                        || (exception instanceof ExceptionConverter)
                        || (exception instanceof DocContentToPdfException)) {
                    LOGGER.warn("[{}] PDF error: {}", this.getJobName(),
                            pubMessage);
                    pubLevel = PubLevelEnum.WARN;
                } else {
                    if (exception instanceof SpExceptionWarn) {
                        logWarnPrintDenied = true;
                        pubLevel = PubLevelEnum.WARN;
                    } else {
                        LOGGER.error(pubMessage, exception);
                        pubLevel = PubLevelEnum.ERROR;
                    }
                }

                if (this.docLogProtocol == DocLogProtocolEnum.IPP) {
                    // Nullify deferred exception: see Mantis #1306!
                    setDeferredException(null);
                }
            }
        }

        if (pubMessage != null) {
            final String msgPrintDenied = localize("pub-user-print-in-denied",
                    requestingUserId, urlQueue, originatorIp, pubMessage);

            if (logWarnPrintDenied) {
                LOGGER.warn(msgPrintDenied);
            }

            AdminPublisher.instance().publish(PubTopicEnum.USER, pubLevel,
                    msgPrintDenied);
        }
    }

    /**
     * @param isClean
     *            If {@code true} the provided {@link DocContentTypeEnum#PDF} is
     *            clean.
     */
    public void setPdfProvidedIsClean(final boolean isClean) {
        this.pdfProvidedIsClean = isClean;
    }

    /**
     */
    private void onRateLimiting() {

        if (ConfigManager.isPrintInRateLimitingEnabled()) {

            String addr = this.getOriginatorIp();

            if (StringUtils.isBlank(addr)) {
                addr = this.getOriginatorEmail();
            }
            if (StringUtils.isNotBlank(addr)) {
                try {
                    RATE_LIMITER_SERVICE.consumeOrWaitForEvent(
                            LimitEnum.PRINT_IN_FAILURE_BY_ADDR,
                            new RateLimiterService.IPEvent(addr, SHORT_NAME),
                            this);
                } catch (EndlessWaitException e) {
                    LOGGER.warn("PrintIn Rate Limiting on {} : {}", addr,
                            e.getClass().getSimpleName());
                }
            }
        }
    }

    @Override
    public void onRateLimited(final IEvent event, final long waitMsec) {
        final PubTopicEnum topic;

        if (this.docLogProtocol == null) {
            topic = PubTopicEnum.PRINT_IN;
        } else {
            switch (this.docLogProtocol) {
            case IPP:
                topic = PubTopicEnum.IPP;
                break;
            case IMAP:
                topic = PubTopicEnum.MAILPRINT;
                break;
            case RAW:
                topic = PubTopicEnum.RAW_PRINT;
                break;
            default:
                topic = PubTopicEnum.PRINT_IN;
                break;
            }
        }
        AdminPublisher.publish(event, topic, waitMsec);
    }

}
