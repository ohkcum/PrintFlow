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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.enums.DocLogProtocolEnum;
import org.printflow.lite.core.dao.enums.ReservedIppQueueEnum;
import org.printflow.lite.core.doc.DocContent;
import org.printflow.lite.core.doc.DocContentTypeEnum;
import org.printflow.lite.core.fonts.InternalFontFamilyEnum;
import org.printflow.lite.core.print.server.DocContentPrintException;
import org.printflow.lite.core.print.server.DocContentPrintReq;
import org.printflow.lite.core.print.server.DocContentPrintRsp;
import org.printflow.lite.core.services.QueueService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.InetUtils;
import org.printflow.lite.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebPrintHelper implements IFileUploadHelper {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebPrintHelper.class);

    /**
     * .
     */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final class SingletonHolder {
        /** */
        public static final WebPrintHelper INSTANCE = new WebPrintHelper();
    }

    /**
     * .
     */
    private WebPrintHelper() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static WebPrintHelper instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param originatorIp
     *            The client IP address.
     * @return {@code true} when WebPrint is enabled.
     */
    public static boolean isWebPrintEnabled(final String originatorIp) {
        return ConfigManager.isWebPrintEnabled()
                && InetUtils.isIpAddrInCidrRanges(
                        ConfigManager.instance().getConfigValue(
                                Key.WEB_PRINT_LIMIT_IP_ADDRESSES),
                        originatorIp);
    }

    /**
     *
     * @return {@code true} when WebPrint DropZone is enabled.
     */
    public static boolean isWebPrintDropZoneEnabled() {
        return ConfigManager.instance()
                .isConfigValue(Key.WEB_PRINT_DROPZONE_ENABLE);
    }

    /**
     *
     * @param originatorIp
     *            The client IP address.
     * @return {@code true} when WebPrint and DropZone is enabled.
     */
    public static boolean isWebPrintDropZoneEnabled(final String originatorIp) {
        return isWebPrintEnabled(originatorIp) && ConfigManager.instance()
                .isConfigValue(Key.WEB_PRINT_DROPZONE_ENABLE);
    }

    @Override
    public Bytes getMaxUploadSize() {
        // Note: Bytes.megabytes() is MiB (not MB).
        return Bytes.bytes(ConfigManager.instance().getConfigLong(
                Key.WEB_PRINT_MAX_FILE_MB,
                IConfigProp.WEBPRINT_MAX_FILE_MB_V_DEFAULT)
                * NumberUtil.INT_THOUSAND * NumberUtil.INT_THOUSAND);
    }

    /**
     * Gets the {@link DocContentTypeEnum} objects that are excluded from Web
     * Print.
     *
     * @return The {@link DocContentTypeEnum} objects (can be empty).
     */
    public static Set<DocContentTypeEnum> getExcludeTypes() {

        final Set<DocContentTypeEnum> excludeTypes = new HashSet<>();

        for (final String ext : ConfigManager.instance()
                .getConfigSet(Key.WEB_PRINT_FILE_EXT_EXCLUDE)) {

            final DocContentTypeEnum contentType = DocContent
                    .getContentTypeFromExt(StringUtils.removeStart(ext, "."));

            if (contentType == null) {
                LOGGER.warn(String.format(
                        "Config item [%s]: [%s] extension is not supported.",
                        ConfigManager.instance().getConfigKey(
                                Key.WEB_PRINT_FILE_EXT_EXCLUDE),
                        ext));
            } else {
                excludeTypes.add(contentType);
            }
        }
        return excludeTypes;
    }

    @Override
    public List<String> getSupportedFileExtensions(final boolean dotPfx) {

        final boolean includeImages = ConfigManager.instance()
                .isConfigValue(Key.WEB_PRINT_GRAPHICS_ENABLE);

        final List<String> list = new ArrayList<>();

        final Set<DocContentTypeEnum> excludeTypes = getExcludeTypes();

        for (final DocContentTypeEnum contentType : DocContentTypeEnum
                .values()) {

            if (excludeTypes.contains(contentType)) {
                continue;
            }

            if (!includeImages && DocContent.isImage(contentType)) {
                continue;
            }

            if (DocContent.isSupported(contentType)) {

                for (final String ext : DocContent
                        .getFileExtensions(contentType)) {
                    if (ext == null) {
                        continue;
                    }
                    if (dotPfx) {
                        list.add(String.format(".%s", ext));
                    } else {
                        list.add(ext);
                    }
                }
            }
        }
        return list;
    }

    /**
     * @param originatorIp
     *            The client IP address.
     * @param userId
     *            The unique ID of user who uploaded the file.
     * @param uploadedFile
     *            The uploaded file.
     * @param preferredFont
     *            The default PDF font for text files.
     * @return {@link DocContentPrintRsp}
     * @throws DocContentPrintException
     *             When conversion to PDF failed.
     * @throws IOException
     *             When IO error.
     * @throws UnavailableException
     *             When service is unavailable.
     */
    public static DocContentPrintRsp handleFileUpload(final String originatorIp,
            final String userId, final FileUpload uploadedFile,
            final InternalFontFamilyEnum preferredFont)
            throws DocContentPrintException, IOException, UnavailableException {

        try {
            final String fileName = uploadedFile.getClientFileName();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("User [%s] uploaded file [%s] [%s]",
                        userId, uploadedFile.getContentType(),
                        uploadedFile.getClientFileName()));
            }

            DocContentTypeEnum contentType = DocContent
                    .getContentTypeFromMime(uploadedFile.getContentType());

            if (contentType == null) {
                contentType = DocContent.getContentTypeFromFile(
                        uploadedFile.getClientFileName());

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "No content type found for [{}], "
                                    + "using [{}] based on file extension.",
                            uploadedFile.getContentType(), contentType);
                }
            }

            final DocContentPrintReq docContentPrintReq =
                    new DocContentPrintReq();

            docContentPrintReq.setContentType(contentType);
            docContentPrintReq.setFileName(fileName);
            docContentPrintReq.setOriginatorEmail(null);
            docContentPrintReq.setOriginatorIp(originatorIp);
            docContentPrintReq.setPreferredOutputFont(preferredFont);
            docContentPrintReq.setProtocol(DocLogProtocolEnum.HTTP);
            docContentPrintReq.setTitle(fileName);

            return QUEUE_SERVICE.printDocContent(ReservedIppQueueEnum.WEBPRINT,
                    null, userId, docContentPrintReq,
                    uploadedFile.getInputStream());

        } finally {
            // Close quietly.
            uploadedFile.closeStreams();
            // Don't wait for garbage collect: delete now.
            uploadedFile.delete();
        }
    }

}
