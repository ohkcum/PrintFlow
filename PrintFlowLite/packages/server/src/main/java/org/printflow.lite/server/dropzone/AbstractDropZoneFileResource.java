/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.server.dropzone;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.resource.AbstractResource;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.UnavailableException.State;
import org.printflow.lite.core.util.JsonHelper;
import org.printflow.lite.server.api.request.ApiRequestMixin;
import org.printflow.lite.server.api.request.ApiResultCodeEnum;
import org.printflow.lite.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractDropZoneFileResource extends AbstractResource {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractDropZoneFileResource.class);

    /**
     * As in: {@code <input type="file">} .
     */
    private static final String UPLOAD_PARAM_NAME_FILE = "file";

    /**
     * @return Parameter name as in {@code <input type="file">} ..
     */
    public static String getUploadFileParm() {
        return UPLOAD_PARAM_NAME_FILE;
    }

    /**
     * @param helper
     * @param attributes
     * @return ({@link ResourceResponse}.
     */
    protected ResourceResponse newResourceResponse(
            final IFileUploadHelperEx helper, final Attributes attributes) {
        final ResourceResponse resourceResponse = new ResourceResponse();

        final ServletWebRequest webRequest =
                (ServletWebRequest) attributes.getRequest();

        final String originatorIp =
                WebAppHelper.getClientIP(attributes.getRequest());

        ApiResultCodeEnum resultCode = ApiResultCodeEnum.OK;
        String resultText = "";

        final Map<String, Boolean> filesStatus = new HashMap<>();
        final Map<String, FileItem> fileItemsToHandle = new HashMap<>();

        try {

            if (!helper.isUploadEnabled()) {
                final String msg = "Service is not available.";
                LOGGER.error(msg);
                throw new UnavailableException(State.PERMANENT, msg);
            }

            final MultipartServletWebRequest multiPartRequest =
                    webRequest.newMultipartWebRequest(helper.getMaxUploadSize(),
                            "ignored");

            /*
             * CRUCIAL: parse the file parts first, before getting the files :-)
             */
            multiPartRequest.parseFileParts();

            final Map<String, List<FileItem>> files =
                    multiPartRequest.getFiles();

            final List<FileItem> fileItems = files.get(UPLOAD_PARAM_NAME_FILE);

            for (final FileItem fileItem : fileItems) {
                fileItemsToHandle.put(fileItem.getName(), fileItem);
            }

            final StringBuilder feedbackMsg = new StringBuilder();

            for (final FileItem fileItem : fileItems) {

                final String fileKey = fileItem.getName();
                filesStatus.put(fileKey, Boolean.FALSE);

                helper.handleFileUpload(originatorIp, new FileUpload(fileItem),
                        feedbackMsg);

                filesStatus.put(fileKey, Boolean.TRUE);
                fileItemsToHandle.remove(fileKey);
            }

            resultText = feedbackMsg.toString();

        } catch (FileUploadException | UnavailableException e) {

            resultCode = ApiResultCodeEnum.INFO;
            resultText = e.getMessage();

        } catch (Exception e) {

            resultCode = ApiResultCodeEnum.ERROR;
            resultText = e.getMessage();

            LOGGER.error("An error occurred while uploading a file.", e);

        } finally {
            // Clean up any file items not handled.
            for (final FileItem fileItem : fileItemsToHandle.values()) {
                fileItem.delete();
            }
        }

        this.writeResponse(resourceResponse, resultCode, resultText,
                filesStatus);

        return resourceResponse;
    }

    /**
     * Sets the response's content type and body.
     *
     * @param response
     *            The {@link ResourceResponse}.
     * @param code
     *            The result code.
     * @param text
     *            The result text.
     * @param fileStatus
     *            The status of each file uploaded.
     */
    private void writeResponse(final ResourceResponse response,
            final ApiResultCodeEnum code, final String text,
            final Map<String, Boolean> fileStatus) {

        response.setContentType("application/json");

        final String responseContent;

        try {
            final Map<String, Object> result =
                    ApiRequestMixin.createApiResultText(code, text);

            result.put("filesStatus", fileStatus);
            responseContent = JsonHelper.objectMapAsString(result);

        } catch (IOException e) {
            throw new SpException(e);
        }

        response.setWriteCallback(new WriteCallback() {
            @Override
            public void writeData(final Attributes attributes)
                    throws IOException {
                attributes.getResponse().write(responseContent);
            }
        });
    }
}
