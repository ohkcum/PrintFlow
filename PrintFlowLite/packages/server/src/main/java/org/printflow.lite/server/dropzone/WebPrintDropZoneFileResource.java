/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
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
package org.printflow.lite.server.dropzone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.UnavailableException.State;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.fonts.InternalFontFamilyEnum;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.print.server.DocContentPrintException;
import org.printflow.lite.core.print.server.DocContentPrintRsp;
import org.printflow.lite.core.print.server.PrintInResultEnum;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.services.ServiceEntryPoint;
import org.printflow.lite.core.util.DateUtil;
import org.printflow.lite.core.util.JsonHelper;
import org.printflow.lite.core.util.NumberUtil;
import org.printflow.lite.server.api.request.ApiRequestMixin;
import org.printflow.lite.server.api.request.ApiResultCodeEnum;
import org.printflow.lite.server.session.SpSession;
import org.printflow.lite.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resource that handles DropZone file uploads. It reads the file items from
 * the request parameters and prints them to the user's inbox.
 * <p>
 * Additionally it writes the response's content type and body.
 * </p>
 * <p>
 * Checks for max upload size and supported file type are also done at the
 * client (JavaScript) side, before sending the file(s).
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class WebPrintDropZoneFileResource
        extends AbstractDropZoneFileResource implements ServiceEntryPoint {

    /**
     * As in: {@code ?font=CJK} .
     */
    private static final String UPLOAD_PARAM_NAME_FONT = "font";

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebPrintDropZoneFileResource.class);

    /**
     * .
     */
    public WebPrintDropZoneFileResource() {
        ServiceContext.setLocale(SpSession.get().getLocale());
    }

    /**
     * @return font parameter.
     */
    public static String getUploadFontParm() {
        return UPLOAD_PARAM_NAME_FONT;
    }

    @Override
    protected ResourceResponse
            newResourceResponse(final Attributes attributes) {

        final ResourceResponse resourceResponse = new ResourceResponse();

        final ServletWebRequest webRequest =
                (ServletWebRequest) attributes.getRequest();

        final SpSession session = SpSession.get();
        final String userId;

        if (session == null) {
            userId = null;
        } else {
            userId = session.getUserId();
        }

        if (userId == null) {
            final String msg = "No authenticated user.";
            LOGGER.warn(msg);
            throw new AbortWithHttpErrorCodeException(
                    HttpServletResponse.SC_UNAUTHORIZED, msg);
        }

        final InternalFontFamilyEnum defaultFont = ConfigManager
                .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY);

        final String originatorIp =
                WebAppHelper.getClientIP(attributes.getRequest());

        ApiResultCodeEnum resultCode = ApiResultCodeEnum.OK;
        String resultText = "";

        final Map<String, Boolean> filesStatus = new HashMap<>();

        final Map<String, FileItem> fileItemsToHandle = new HashMap<>();

        ServiceContext.open();

        try {

            if (!WebPrintHelper.isWebPrintEnabled(originatorIp)) {
                throw new UnavailableException(State.PERMANENT,
                        "Service is not available.");
            }

            final MultipartServletWebRequest multiPartRequest =
                    webRequest.newMultipartWebRequest(
                            WebPrintHelper.instance().getMaxUploadSize(),
                            "ignored");

            final InternalFontFamilyEnum selectedFont = EnumUtils.getEnum(
                    InternalFontFamilyEnum.class,
                    multiPartRequest.getUrl()
                            .getQueryParameterValue(UPLOAD_PARAM_NAME_FONT)
                            .toString(defaultFont.toString()));

            /*
             * CRUCIAL: parse the file parts first, before getting the files :-)
             */
            multiPartRequest.parseFileParts();

            final Map<String, List<FileItem>> files =
                    multiPartRequest.getFiles();

            final List<FileItem> fileItemsAll = new ArrayList<>();

            for (final List<FileItem> list : files.values()) {
                for (final FileItem fileItem : list) {
                    fileItemsToHandle.put(fileItem.getName(), fileItem);
                    fileItemsAll.add(fileItem);
                }
            }

            if (files.get(getUploadFileParm()) == null) {
                LOGGER.debug("WebPrint [{}]: no files for key [{}]", userId,
                        getUploadFileParm());
            }

            final int totFiles = fileItemsAll.size();

            if (totFiles == 0) {
                throw new DocContentPrintException("No files uploaded.");
            }

            int nFileWlk = 0;
            int nFileWlkFontWarning = 0;

            for (final FileItem fileItem : fileItemsAll) {

                final String fileKey = fileItem.getName();
                filesStatus.put(fileKey, Boolean.FALSE);

                nFileWlk++;

                final long start = System.currentTimeMillis();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("WebPrint [{}] {}/{} [{}] uploading... [{}]",
                            userId, nFileWlk, totFiles, fileItem.getName(),
                            NumberUtil.humanReadableByteCountSI(
                                    Locale.getDefault(), fileItem.getSize()));
                }
                final DocContentPrintRsp rsp =
                        WebPrintHelper.handleFileUpload(originatorIp, userId,
                                new FileUpload(fileItem), selectedFont);

                if (rsp.getResult() == PrintInResultEnum.FONT_WARNING) {
                    final Locale locale = session.getLocale();
                    nFileWlkFontWarning++;
                    resultCode = ApiResultCodeEnum.INFO;
                    resultText = String.format("%d %s : %s [%s]",
                            nFileWlkFontWarning,
                            NounEnum.FILE.uiText(locale,
                                    nFileWlkFontWarning > 1),
                            NounEnum.FONT.uiText(locale),
                            NounEnum.WARNING.uiText(locale, true));
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("WebPrint [{}] {}/{} [{}] ....uploaded [{}].",
                            userId, nFileWlk, totFiles, fileItem.getName(),
                            DateUtil.formatDuration(
                                    System.currentTimeMillis() - start));
                }
                filesStatus.put(fileKey, Boolean.TRUE);
                fileItemsToHandle.remove(fileKey);
            }

        } catch (UnavailableException | DocContentPrintException e) {

            resultCode = ApiResultCodeEnum.INFO;
            resultText = e.getMessage();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("WebPrint [{}] [{}]: {}", userId,
                        e.getClass().getSimpleName(), e.getMessage());
            }

        } catch (FileUploadException e) {

            resultCode = ApiResultCodeEnum.INFO;
            resultText = e.getMessage();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        String.format("WebPrint [%s] [%s]: %s", userId,
                                e.getClass().getSimpleName(), e.getMessage()),
                        e);
            }

        } catch (IOException e) {

            resultCode = ApiResultCodeEnum.WARN;
            resultText = e.getMessage();

            LOGGER.warn(String.format("WebPrint [%s] [%s]: %s", userId,
                    e.getClass().getSimpleName(), e.getMessage()), e);

        } catch (Exception e) {

            resultCode = ApiResultCodeEnum.ERROR;
            resultText = e.getMessage();

            LOGGER.error(String.format("WebPrint [%s] [%s]: %s", userId,
                    e.getClass().getSimpleName(), e.getMessage()), e);

        } finally {

            // Clean up any file items not handled.
            for (final FileItem fileItem : fileItemsToHandle.values()) {
                fileItem.delete();
            }

            ServiceContext.close();
        }

        writeResponse(resourceResponse, resultCode, resultText, filesStatus);

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
