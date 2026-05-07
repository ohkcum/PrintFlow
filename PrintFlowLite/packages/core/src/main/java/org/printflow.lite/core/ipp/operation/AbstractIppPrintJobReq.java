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
package org.printflow.lite.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.printflow.lite.core.dao.enums.DocLogProtocolEnum;
import org.printflow.lite.core.dao.helpers.IppQueueHelper;
import org.printflow.lite.core.ipp.IppAccessDeniedException;
import org.printflow.lite.core.ipp.IppProcessingException;
import org.printflow.lite.core.ipp.attribute.IppAttrGroup;
import org.printflow.lite.core.ipp.attribute.IppAttrValue;
import org.printflow.lite.core.ipp.attribute.IppDictJobDescAttr;
import org.printflow.lite.core.ipp.attribute.IppDictJobTemplateAttr;
import org.printflow.lite.core.ipp.attribute.IppDictOperationAttr;
import org.printflow.lite.core.ipp.attribute.syntax.IppKeyword;
import org.printflow.lite.core.ipp.encoding.IppDelimiterTag;
import org.printflow.lite.core.jpa.IppQueue;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.print.server.DocContentPrintProcessor;
import org.printflow.lite.core.print.server.PostScriptFilter;
import org.printflow.lite.core.system.SystemInfo;
import org.printflow.lite.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractIppPrintJobReq extends AbstractIppRequest {

    /** */
    private static class JobIdCounter {
        /** */
        public static final AtomicInteger INSTANCE =
                new AtomicInteger((int) (SystemInfo.getStarttime()
                        / DateUtil.DURATION_MSEC_SECOND));
    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractIppPrintJobReq.class);

    /*
     * Group 1: Operation Attributes
     *
     * Group 2: Job Template Attributes
     *
     * Group 3: Document Content
     */

    /** */
    private DocContentPrintProcessor printInProcessor = null;

    /**
     * Job-id: generated or requested.
     */
    private int jobId;

    /** */
    public AbstractIppPrintJobReq() {
    }

    /**
     * @return The IPP operation this request belongs to.
     */
    protected abstract IppOperationId getIppOperationId();

    /**
     * @return Job name for PrintIn object.
     */
    protected abstract String getPrintInJobName();

    /**
     * @return {@code true} if a job-id is generated. If {@code false}, the
     *         job-id is taken from the IPP Operation group attributes.
     */
    protected abstract boolean isJobIdGenerated();

    /**
     * @return The status code of the response.
     */
    protected abstract IppStatusCode getResponseStatusCode();

    /**
     * @return Print-in processor.
     */
    protected DocContentPrintProcessor getPrintInProcessor() {
        return this.printInProcessor;
    }

    /**
     * Just the attributes, not the print job data.
     *
     * @param operation
     *            IPP operation.
     * @param istr
     *            Input stream.
     * @throws IOException
     *             If error.
     */
    public void processAttributes(final AbstractIppJobOperation operation,
            final InputStream istr) throws IOException {

        final String authWebAppUser;

        if (operation.isAuthUserIppRequester()) {
            authWebAppUser = operation.getAuthenticatedUser();
        } else {
            authWebAppUser = null;
        }

        /*
         * Step 1: Create generic PrintIn handler.
         *
         * This should be a first action because this handler holds the deferred
         * exception.
         */
        this.printInProcessor =
                new DocContentPrintProcessor(operation.getQueue(),
                        operation.getOriginatorIp(), null, authWebAppUser);

        this.printInProcessor.setDocLogProtocol(DocLogProtocolEnum.IPP);
        this.printInProcessor.setIppOperationId(this.getIppOperationId());

        this.printInProcessor
                .setIppRoutinglistener(operation.getIppRoutingListener());

        /*
         * Step 2: Read the IPP attributes.
         */
        this.readAttributes(operation, istr);

        // Determine job-id.
        if (this.isJobIdGenerated()) {
            this.jobId = JobIdCounter.INSTANCE.incrementAndGet();
        } else {
            this.jobId = this.getJobIdAttr().intValue();
        }

        this.printInProcessor.setJobName(Objects
                .toString(this.getPrintInJobName(), this.getDocumentName()));

        final String requestingUserName = this.getRequestingUserName();
        final String assignedUserId;

        if (operation.isAuthUserIppRequester()) {
            assignedUserId = operation.getAuthenticatedUser();
        } else {
            assignedUserId = requestingUserName;
        }

        this.printInProcessor.processAssignedUser(assignedUserId,
                requestingUserName);

        LOGGER.debug(String.format("\n---------- %s ----------\n%s",
                this.getClass().getSimpleName(),
                this.getAttrValuesForLogging()));
    }

    /**
     * @return {@code true} if trusted user is present.
     */
    public boolean isTrustedUser() {
        return this.printInProcessor.isTrustedUser();
    }

    /**
     * Checks if user exists in database.
     *
     * @throws IppAccessDeniedException
     *             if user does not exist.
     */
    protected final void checkUserDbPresent() throws IppAccessDeniedException {
        if (this.getUserDb() == null) {
            throw new IppAccessDeniedException("user unkown");
        }
    }

    /**
     * Selects IPP attributes for PrintIn data.
     *
     * @return IPP attribute key/value map.
     */
    protected final Map<String, String> selectIppPrintInData() {

        final Map<String, String> attr = new HashMap<>();

        for (final IppAttrGroup group : this.getAttrGroups()) {

            if (group.getDelimiterTag() == IppDelimiterTag.OPERATION_ATTR
                    || group.getDelimiterTag() == IppDelimiterTag.JOB_ATTR) {

                for (final IppAttrValue value : group.getAttributes()) {

                    if (value.getValues().size() == 1) {

                        final String kw = value.getAttribute().getKeyword();

                        switch (kw) {
                        // IANA attributes: no breaks intended
                        case IppDictOperationAttr.ATTR_REQUESTING_USER_NAME:
                        case IppDictOperationAttr.ATTR_PRINTER_URI:
                        case IppDictOperationAttr.ATTR_JOB_ID:
                        case IppDictOperationAttr.ATTR_JOB_NAME:
                        case IppDictOperationAttr.ATTR_DOCUMENT_FORMAT:
                        case IppDictOperationAttr.ATTR_DOCUMENT_NAME:
                        case IppDictJobTemplateAttr.ATTR_ORIENTATION_REQUESTED:
                            attr.put(kw, value.getSingleValue());
                            break;

                        default:
                            this.selectIppPrintInDataExt(attr, kw,
                                    value.getSingleValue());
                            LOGGER.debug("{} : {}", kw, value.getSingleValue());
                            break;
                        }
                    }
                }
            }
        }
        return attr;
    }

    /**
     * Add extra IANA attributes. Some are mapped from "odd" PostScript
     * attributes (as observed in tests). These attributes can be used as
     * defaults in Proxy Printing scenario's (e.g. IPP Routing and Fast Mode
     * Print).
     *
     * @param attr
     * @param key
     * @param value
     */
    private void selectIppPrintInDataExt(final Map<String, String> attr,
            final String key, final String value) {

        switch (key) {

        // IANA IPP
        case IppDictJobTemplateAttr.ATTR_SIDES:
            attr.put(key, value);
            break;

        // Translate PostScript to IPP
        case PostScriptFilter.KEYWORD_DUPLEX:

            switch (key) {
            case PostScriptFilter.KEYWORD_DUPLEX_TWO_SIDED_LONG_EDGE:
                attr.put(IppDictJobDescAttr.ATTR_SIDES,
                        IppKeyword.SIDES_TWO_SIDED_LONG_EDGE);
                break;
            case PostScriptFilter.KEYWORD_DUPLEX_TWO_SIDED_SHORT_EDGE:
                attr.put(IppDictJobDescAttr.ATTR_SIDES,
                        IppKeyword.SIDES_TWO_SIDED_SHORT_EDGE);
                break;
            case PostScriptFilter.KEYWORD_DUPLEX_ONE_SIDED:
                attr.put(IppDictJobDescAttr.ATTR_SIDES,
                        IppKeyword.SIDES_ONE_SIDED);
                break;
            default:
                break;
            }

            break;

        default:
            break;
        }
    }

    /**
     * Add extra IANA attributes. Some are mapped from "odd" PostScript
     * attributes (as observed in tests). These attributes are NOT relevant in
     * in Proxy Printing scenario's because the effect is already captured in
     * the print result.
     *
     * @param attr
     * @param key
     * @param value
     */
    private void selectIppPrintInDataExt2(final Map<String, String> attr,
            final String key, final String value) {

        switch (key) {

        // IANA IPP
        case IppDictJobTemplateAttr.ATTR_COPIES:
        case IppDictJobTemplateAttr.ATTR_SHEET_COLLATE:
            attr.put(key, value);
            break;

        // Translate PostScript to IPP
        case "Collate": // TODO
            switch (key) {
            case "0":
                attr.put(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE,
                        IppKeyword.SHEET_COLLATE_UNCOLLATED);
                break;
            case "1":
                attr.put(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE,
                        IppKeyword.SHEET_COLLATE_COLLATED);
            default:
                break;
            }

            break;

        default:
            break;
        }
    }

    /**
     *
     * @return The job-id attribute.
     */
    private Integer getJobIdAttr() {

        final IppAttrValue ippValue =
                this.getAttrValue(IppDictJobDescAttr.ATTR_JOB_ID);

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return null;
        }
        return Integer.valueOf(ippValue.getValues().get(0));
    }

    /**
     * @return The job name.
     */
    public String getJobName() {

        final IppAttrValue ippValue =
                this.getAttrValue(IppDictJobDescAttr.ATTR_JOB_NAME);

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return "";
        }
        return ippValue.getValues().get(0);
    }

    /**
     * @return The document name.
     */
    public String getDocumentName() {

        final IppAttrValue ippValue =
                this.getAttrValue(IppDictOperationAttr.ATTR_DOCUMENT_NAME);

        if (ippValue == null || ippValue.getValues().isEmpty()) {
            return "";
        }
        return ippValue.getValues().get(0);
    }

    /**
     * @param theJobId
     *            Job id.
     * @return Job URI.
     */
    public String getJobUri(final int theJobId) {
        return IppDictJobDescAttr.createJobUri(
                this.getAttrValue(IppDictOperationAttr.ATTR_PRINTER_URI)
                        .getValues().get(0),
                String.valueOf(theJobId));
    }

    /**
     * The user object from the database representing the user who printed this
     * job.
     *
     * @return {@code null} when unknown.
     */
    public User getUserDb() {
        return this.printInProcessor.getUserDb();
    }

    /**
     * @return The job-id (either generated, or passed as IPP operation
     *         attribute).
     */
    public int getJobId() {
        return this.jobId;
    }

    /**
     * @return {@code true} if deferred exception.
     */
    public boolean hasDeferredException() {
        return this.printInProcessor.getDeferredException() != null;
    }

    /**
     *
     * @return Exception.
     */
    public IppProcessingException getDeferredException() {
        final Exception ex = this.printInProcessor.getDeferredException();
        if (ex == null) {
            return null;
        }
        if (ex instanceof IppProcessingException) {
            return (IppProcessingException) ex;
        }
        return new IppProcessingException(
                IppProcessingException.StateEnum.INTERNAL_ERROR,
                ex.getMessage(), ex);
    }

    /**
     * @param e
     *            Exception.
     */
    public void setDeferredException(final IppProcessingException e) {
        this.printInProcessor.setDeferredException(e);
    }

    /**
     * @param e
     *            Exception.
     */
    public void setDeferredException(final IppAccessDeniedException e) {
        this.printInProcessor.setDeferredException(e);
    }

    /**
     * @return {@code true} if DRM error.
     */
    public boolean isDrmViolationDetected() {
        return this.printInProcessor.isDrmViolationDetected();
    }

    /**
     * Wraps the
     * {@link DocContentPrintProcessor#evaluateErrorState(boolean, String)}
     * method.
     *
     * @param operation
     *            The {@link AbstractIppJobOperation}.
     * @throws IppProcessingException
     *             If IPP error.
     */
    public void evaluateErrorState(final AbstractIppJobOperation operation)
            throws IppProcessingException {

        final boolean isAuthorized = operation.isAuthorized();
        final String requestingUserName = this.getRequestingUserName();

        this.printInProcessor.evaluateErrorState(isAuthorized,
                requestingUserName);

        if (hasDeferredException()) {
            throw getDeferredException();

        } else if (!isAuthorized) {

            final IppQueue queue = operation.getQueue();

            final StringBuilder msg = new StringBuilder();

            msg.append("User \"" + requestingUserName
                    + "\" denied access to queue");
            msg.append(" \"").append(IppQueueHelper.uiPath(queue)).append("\"");

            if (queue.getDeleted().booleanValue()) {
                msg.append(" (deleted)");
            } else if (queue.getDisabled().booleanValue()) {
                msg.append(" (disabled)");
            }
            msg.append(".");

            throw new IppProcessingException(
                    IppProcessingException.StateEnum.UNAUTHORIZED,
                    msg.toString());
        }
    }

}
