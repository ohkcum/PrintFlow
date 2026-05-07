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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.doc.DocContent;
import org.printflow.lite.core.pdf.AbstractPdfCreator;
import org.printflow.lite.core.pdf.PdfInfoDto;
import org.printflow.lite.core.util.NumberUtil;
import org.printflow.lite.server.pages.MarkupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.profiles.RuleId;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfValidateUploadHelper implements IFileUploadHelperEx {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfValidateUploadHelper.class);

    /** */
    private static final String IMG_PATH_VALID =
            MarkupHelper.IMG_PATH_GENERIC_CHECKMARK_GREEN;

    /** */
    private static final String IMG_PATH_ERROR =
            MarkupHelper.IMG_PATH_GENERIC_ERROR;

    /** */
    private static final String IMG_PATH_EXCEPTION =
            MarkupHelper.IMG_PATH_GENERIC_CROSS_RED;

    /** */
    private static final class SingletonHolder {
        /** */
        public static final PdfValidateUploadHelper INSTANCE =
                new PdfValidateUploadHelper();
    }

    /** */
    private PdfValidateUploadHelper() {
        VeraGreenfieldFoundryProvider.initialise();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static PdfValidateUploadHelper instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public Bytes getMaxUploadSize() {
        // Note: Bytes.megabytes() is MiB (not MB).
        return Bytes.bytes(ConfigManager.instance().getConfigLong(
                Key.WEBAPP_PDF_VALIDATE_MAX_UPLOAD_FILE_MB,
                IConfigProp.WEBAPP_PDFVALIDATE_MAX_UPLOAD_FILE_MB_V_DEFAULT)
                * NumberUtil.INT_THOUSAND * NumberUtil.INT_THOUSAND);
    }

    @Override
    public List<String> getSupportedFileExtensions(final boolean dotPfx) {

        final List<String> list = new ArrayList<>();

        if (dotPfx) {
            list.add(String.format(".%s", DocContent.FILENAME_EXT_PDF));
        } else {
            list.add(DocContent.FILENAME_EXT_PDF);
        }
        return list;
    }

    @Override
    public void handleFileUpload(final String originatorIp,
            final FileUpload uploadedFile, final StringBuilder feedbackMsg) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[{}] uploaded file [{}] [{}]", originatorIp,
                    uploadedFile.getContentType(),
                    uploadedFile.getClientFileName());
        }

        File fileTemp = null;

        try {
            fileTemp = uploadedFile.writeToTempFile();

            final PdfInfoDto pdfInfoDto = AbstractPdfCreator
                    .createPdfInfo(fileTemp.getAbsolutePath());

            try (PDFAParser parser = Foundries.defaultInstance()
                    .createParser(new FileInputStream(fileTemp))) {

                final PDFAValidator validator = Foundries.defaultInstance()
                        .createValidator(parser.getFlavour(), false);

                final ValidationResult result = validator.validate(parser);

                feedbackMsg.append("<div class=\"sp-pdfvalidate-verify-entry")
                        .append(" ").append(MarkupHelper.CSS_TXT_WRAP)
                        .append(" ");

                final String imgSrc;

                if (result.isCompliant()) {
                    feedbackMsg.append(MarkupHelper.CSS_TXT_VALID).append(" ")
                            .append("sp-pdfvalidate-verify-entry-valid");
                    imgSrc = IMG_PATH_VALID;
                } else {
                    feedbackMsg.append(MarkupHelper.CSS_TXT_WARN).append(" ")
                            .append("sp-pdfvalidate-verify-entry-warn");
                    imgSrc = IMG_PATH_ERROR;
                }
                feedbackMsg.append("\">");

                appendFileInfo(uploadedFile, feedbackMsg, imgSrc);

                //
                feedbackMsg.append(
                        "<small><span class=\"sp-pdfvalidate-file\"><table>");
                feedbackMsg
                        .append("<tr><td valign=\"top\">Producer</td>"
                                + "<td valign=\"top\">:</td><td>")
                        .append(StringUtils
                                .defaultString(pdfInfoDto.getProducer()))
                        .append("</td></tr>");
                feedbackMsg
                        .append("<tr><td valign=\"top\">Creator</td>"
                                + "<td valign=\"top\">:</td><td>")
                        .append(StringUtils
                                .defaultString(pdfInfoDto.getCreator()))
                        .append("</td></tr>");
                feedbackMsg
                        .append("<tr><td valign=\"top\">Author</td>"
                                + "<td valign=\"top\">:</td><td>")
                        .append(StringUtils
                                .defaultString(pdfInfoDto.getAuthor()))
                        .append("</td></tr>");
                feedbackMsg
                        .append("<tr><td valign=\"top\">Title</td>"
                                + "<td valign=\"top\">:</td><td>")
                        .append(StringUtils
                                .defaultString(pdfInfoDto.getTitle()))
                        .append("</td></tr>");
                feedbackMsg.append("</table></span></small>");
                feedbackMsg.append("<br>");

                //
                feedbackMsg.append(
                        result.getValidationProfile().getDetails().getName());
                feedbackMsg.append("<br>");
                feedbackMsg.append(result.getValidationProfile().getDetails()
                        .getDescription());
                feedbackMsg.append(". ")
                        .append(result.getProfileDetails().getCreator())
                        .append(".");

                if (!result.isCompliant()) {

                    feedbackMsg.append("<br><br><small>");
                    feedbackMsg.append(
                            String.format("%d of %d validation tests failed",
                                    result.getTestAssertions().size(),
                                    result.getTotalAssertions()));

                    feedbackMsg.append("<br>");

                    // https://docs.verapdf.org/validation/
                    feedbackMsg.append("<table>");
                    feedbackMsg.append("<tr><th align=\"center\">Tests</th>"
                            + "<th align=\"left\">Rule</th></tr>");

                    for (final Entry<RuleId, Integer> entry : result
                            .getFailedChecks().entrySet()) {

                        feedbackMsg.append("<tr>");

                        feedbackMsg
                                .append("<td valign=\"top\" align=\"center\">")
                                .append(entry.getValue().intValue())
                                .append("</td>");

                        feedbackMsg.append("<td valign=\"top\">")
                                .append(entry.getKey().getClause()).append("-")
                                .append(entry.getKey().getTestNumber())
                                .append(": ")
                                .append(result.getValidationProfile()
                                        .getRuleByRuleId(entry.getKey())
                                        .getDescription())
                                .append(".").append("</td>");

                        feedbackMsg.append("</tr>");
                    }
                    feedbackMsg.append("</table>");
                    feedbackMsg.append("</small>");
                }

                feedbackMsg.append("</div>");
            }

        } catch (Exception e) {

            feedbackMsg.append("<div class=\"sp-pdfvalidate-verify-entry")
                    .append(" ").append(MarkupHelper.CSS_TXT_WRAP).append(" ")
                    .append(MarkupHelper.CSS_TXT_ERROR).append(" ")
                    .append("sp-pdfvalidate-verify-entry-error").append("\">");

            appendFileInfo(uploadedFile, feedbackMsg, IMG_PATH_EXCEPTION);

            feedbackMsg.append("The PDF could not be validated");
            feedbackMsg.append("<br><br>");
            feedbackMsg.append("<small>").append(e.getMessage())
                    .append("</small>");
            feedbackMsg.append("</div>");

        } finally {
            // Close quietly.
            uploadedFile.closeStreams();
            // Don't wait for garbage collect: delete now.
            uploadedFile.delete();
            //
            if (fileTemp != null) {
                fileTemp.delete();
            }
        }
    }

    /**
     *
     * @param uploadedFile
     *            Uploaded file
     * @param feedbackMsg
     *            Message to append on.
     * @param imgSrc
     *            image URL path.
     */
    private static void appendFileInfo(final FileUpload uploadedFile,
            final StringBuilder feedbackMsg, final String imgSrc) {
        feedbackMsg.append("<span class=\"sp-pdfvalidate-file\">").append(
                "<img class=\"sp-pdfvalidate-status-img\" height=\"20\" src=\"")
                .append(imgSrc).append("\">").append("&nbsp;&nbsp;")
                .append(uploadedFile.getClientFileName()).append(" &bull; ")
                .append(NumberUtil.humanReadableByteCountSI(Locale.US,
                        uploadedFile.getSize()))
                .append("</span>");
        feedbackMsg.append("<br><br>");
    }

    @Override
    public boolean isUploadEnabled() {
        return ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_PDF_VALIDATE_ENABLE);
    }
}
