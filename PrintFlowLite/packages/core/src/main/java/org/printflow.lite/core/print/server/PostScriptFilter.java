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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.ipp.attribute.IppDictJobTemplateAttr;
import org.printflow.lite.core.ipp.attribute.syntax.IppKeyword;
import org.printflow.lite.core.ipp.helpers.IppOptionMap;
import org.printflow.lite.core.services.InboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a PostScript stream to detect DRM and maps PostScripts elements to
 * IPP options. See {@link InboxService#getPrintinIppOptions()}.
 *
 * @author Rijk Ravestein
 *
 */
public final class PostScriptFilter implements IUtility {

    /** Utility class. */
    private PostScriptFilter() {
    }

    /**
     * Result of filtering.
     */
    public static class Result {

        private ResultEnum code;
        private String title;
        private String userId;

        private final Map<String, String> ippOptions;

        private final IppOptionMap ippOptionsWrapper;

        /** */
        public Result() {
            this.ippOptions = new HashMap<String, String>();
            this.ippOptionsWrapper = new IppOptionMap(ippOptions);
        }

        /**
         * Number of PostScript Pages (experimental).
         */
        private String numberOfPages;

        public ResultEnum getCode() {
            return code;
        }

        public void setCode(ResultEnum code) {
            this.code = code;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public IppOptionMap getIppOptionMap() {
            return ippOptionsWrapper;
        }

        /**
         * @return Number of PostScript Pages.
         */
        public String getNumberOfPages() {
            return numberOfPages;
        }

        /**
         * @param numberOfPages
         *            Number of PostScript Pages.
         */
        public void setNumberOfPages(String numberOfPages) {
            this.numberOfPages = numberOfPages;
        }

        /**
         * @return {@code true} if "sides" is detected.
         */
        public boolean isSidesDetected() {
            return this.ippOptionsWrapper
                    .isOptionPresent(IppDictJobTemplateAttr.ATTR_SIDES);
        }

        /**
         * @return {@code true} if "print-color-mode" is detected.
         */
        public boolean isPrintColorModeDetected() {
            return this.ippOptionsWrapper.isOptionPresent(
                    IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE);
        }

        /** */
        public void setOneSided() {
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_SIDES,
                    IppKeyword.SIDES_ONE_SIDED);
        }

        /** */
        public void setPrintColorModeColor() {
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                    IppKeyword.PRINT_COLOR_MODE_COLOR);
        }

        /** */
        public void setPrintColorModeMonochrome() {
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                    IppKeyword.PRINT_COLOR_MODE_MONOCHROME);
        }

        /** */
        public void setTwoSidedLongEdge() {
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_SIDES,
                    IppKeyword.SIDES_TWO_SIDED_LONG_EDGE);
        }

        /** */
        public void setTwoSidedShortEdge() {
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_SIDES,
                    IppKeyword.SIDES_TWO_SIDED_SHORT_EDGE);
        }

        /**
         * @return {@code true} if "collate" is detected.
         */
        public boolean isCollateDetected() {
            return this.ippOptionsWrapper
                    .isOptionPresent(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE);
        }

        /**
         *
         * @param collate
         *            {@code true} if collated.
         */
        public void setCollate(final boolean collate) {
            final String ippValue;
            if (collate) {
                ippValue = IppKeyword.SHEET_COLLATE_COLLATED;
            } else {
                ippValue = IppKeyword.SHEET_COLLATE_UNCOLLATED;
            }
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE,
                    ippValue);
        }

        /**
         * @return {@code true} if number of copies is detected.
         */
        public boolean isNumCopiesDetected() {
            return this.ippOptionsWrapper
                    .isOptionPresent(IppDictJobTemplateAttr.ATTR_COPIES);
        }

        public String getNumCopies() {
            return this.ippOptions.get(IppDictJobTemplateAttr.ATTR_COPIES);
        }

        public void setNumCopies(String copies) {
            this.ippOptions.put(IppDictJobTemplateAttr.ATTR_COPIES, copies);
        }

    }

    /** */
    public enum ResultEnum {
        /**
         * No DRM encountered.
         */
        DRM_NO,
        /**
         * DRM encountered: resulting output is broken (invalid).
         */
        DRM_YES,
        /**
         * DRM encountered but neglected (removed).
         */
        DRM_NEGLECTED
    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PostScriptFilter.class);

    /** */
    public static final String KEYWORD_DUPLEX = "Duplex";

    /** */
    public static final String KEYWORD_COLLATE = "Collate";

    /** */
    public static final String KEYWORD_NUMCOPIES = "NumCopies";

    /** */
    public static final String KEYWORD_DUPLEX_ONE_SIDED = "None";

    /** */
    public static final String KEYWORD_DUPLEX_TWO_SIDED_LONG_EDGE =
            "DuplexNoTumble";

    /**  */
    public static final String KEYWORD_DUPLEX_TWO_SIDED_SHORT_EDGE =
            "DuplexTumble";

    /** End-of-comments. */
    private static final String PFX_END_COMMENTS = "%%EndComments";

    /** Begin Prolog. */
    private static final String PFX_BEGIN_PROLOG = "%%BeginProlog";

    /** */
    public static final String PFX_TITLE = "%%Title: ";
    /** */
    public static final String PFX_USERID = "%%For: ";

    /** Observed in some postscript. */
    private static final String PFX_PAGES = "%%Pages: ";

    /** Observed in some postscript. */
    private static final String PFX_PAGES_ATEND = "(atend)";

    /** Observed in some postscript. */
    private static final String PFX_REQUIREMENTS = "%%Requirements: ";

    /** Observed in some postscript. */
    private static final String PFX_REQUIREMENTS_DUPLEX = "duplex";

    /** Observed in some postscript. */
    private static final String PFX_NUM_COPIES = "%RBINumCopies:";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE = "%%BeginFeature: ";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_DUPLEX =
            "*" + KEYWORD_DUPLEX + " ";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_DUPLEX_ONE_SIDED =
            KEYWORD_DUPLEX_ONE_SIDED;

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_DUPLEX_TWO_SIDED_LONG_EDGE =
            KEYWORD_DUPLEX_TWO_SIDED_LONG_EDGE;

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_DUPLEX_TWO_SIDED_SHORT_EDGE =
            KEYWORD_DUPLEX_TWO_SIDED_SHORT_EDGE;

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_COLLATE =
            "*" + KEYWORD_COLLATE + " ";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_COLLATE_TRUE = "True";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_FEATURE_COLLATE_FALSE = "False";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_NON_PPD_FEATURE =
            "%%BeginNonPPDFeature: ";

    /** Observed in some postscript. */
    private static final String PFX_BEGIN_NON_PPD_FEATURE_NUMCOPIES =
            KEYWORD_NUMCOPIES + " ";

    /** Observed in some postscript. */
    private static final String PFX_PROCESS_COLORMODEL =
            "<</ProcessColorModel ";
    /** */
    private static final String PFX_PROCESS_COLORMODEL_COLOR = "/DeviceCMYK";
    /** */
    private static final String PFX_PROCESS_COLORMODEL_MONOCHROME =
            "/DeviceGray";

    /** */
    private static final String PFX_PJL_SET = "@PJL SET ";

    /** Observed in some postscript. */
    private static final String PFX_PJL_SET_LCOLORMODEL =
            PFX_PJL_SET + "LCOLORMODEL";
    /**
     * {@code @PJL SET LCOLORMODEL=CMYK }
     */
    private static final String PFX_PJL_SET_LCOLORMODEL_COLOR = "CMYK";
    /**
     * {@code @PJL SET LCOLORMODEL=BLACK }
     */
    private static final String PFX_PJL_SET_LCOLORMODEL_MONOCHROME = "BLACK";

    /** */
    private static final String SCAN_FEATUREBEGIN_PFX = "featurebegin{<<";

    /**
     * "{@code <tab>featurebegin{<< /Collate true >>
     * setpagedevice}featurecleanup}"
     */
    private static final String SCAN_FEATUREBEGIN_COLLATE_TRUE =
            "/Collate true";
    /**
     * "{@code <tab>featurebegin{<< /Collate true >>
     * setpagedevice}featurecleanup}"
     */
    private static final String SCAN_FEATUREBEGIN_COLLATE_FALSE =
            "/Collate false";

    /** */
    private static final String SCAN_FEATUREBEGIN_PROCESSCOLORMODEL =
            "/ProcessColorModel";
    /**
     * "{@code <tab>featurebegin{<< /ProcessColorModel /DeviceCMYK>>
     * setpagedevice}featurecleanup}"
     */
    private static final String SCAN_FEATUREBEGIN_PROCESSCOLORMODEL_COLOR =
            "/DeviceCMYK";

    /**
     * "{@code <tab>featurebegin{<< /ProcessColorModel /DeviceCMYK>>
     * setpagedevice}featurecleanup}"
     */
    private static final String SCAN_FEATUREBEGIN_PROCESSCOLORMODEL_MONOCHROME =
            "/DeviceGray";

    /** */
    private static final String DRM_SIGNATURE[] = {
            //
            "%ADOBeginClientInjection: DocumentSetup Start \"No Re-Distill\"",
            "%% Removing the following eleven lines is illegal, subject to the Digital Copyright Act of 1998.",
            "mark currentfile eexec",
            "54dc5232e897cbaaa7584b7da7c23a6c59e7451851159cdbf40334cc2600",
            "30036a856fabb196b3ddab71514d79106c969797b119ae4379c5ac9b7318",
            "33471fc81a8e4b87bac59f7003cddaebea2a741c4e80818b4b136660994b",
            "18a85d6b60e3c6b57cc0815fe834bc82704ac2caf0b6e228ce1b2218c8c7",
            "67e87aef6db14cd38dda844c855b4e9c46d510cab8fdaa521d67cbb83ee1",
            "af966cc79653b9aca2a5f91f908bbd3f06ecc0c940097ec77e210e6184dc",
            "2f5777aacfc6907d43f1edb490a2a89c9af5b90ff126c0c3c5da9ae99f59",
            "d47040be1c0336205bf3c6169b1b01cd78f922ec384cd0fcab955c0c20de",
            "000000000000000000000000000000000000000000000000000000000000",
            "cleartomark",
            "%ADOEndClientInjection: DocumentSetup Start \"No Re-Distill\""
            //
    };

    /**
     * Streams the lines from the PostScript reader to the writer.
     *
     * @param reader
     *            The PostScript reader.
     * @param writer
     *            The PostScript writer.
     * @param fRespectDRM
     *            If {@code false}, any DRM signature is omitted from the
     *            stream. If {@code true}, the function immediately returns
     *            {@link EXIT_FAILURE} when a DRM signature is encountered.
     * @return The result including process exit code.
     * @throws IOException
     */
    public static Result process(final BufferedReader reader,
            final BufferedWriter writer, final boolean fRespectDRM)
            throws IOException {

        ResultEnum ret = ResultEnum.DRM_NO;

        final Result result = new Result();

        /*
         * Calculate the minimum signature string length to compare.
         */
        int minStrLenSig = 0;

        for (String line : DRM_SIGNATURE) {
            int len = line.length();
            if (minStrLenSig == 0 || minStrLenSig > len) {
                minStrLenSig = len;
            }
        }

        final int nSigLines = DRM_SIGNATURE.length;

        /*
         * Initial read.
         */
        int iSigLine = 0;

        String line = reader.readLine();

        int nFlushLineCounter = 0;
        final int nFlushLinesThreshold = 500;

        boolean readComments = true;

        final MutableBoolean isPagesAtEnd = new MutableBoolean(false);

        while (line != null) {

            int bytesRead = line.length();

            if (readComments) {
                readComments = parseComment(result, line, isPagesAtEnd);
            } else {
                if (isPagesAtEnd.isTrue()) {
                    checkPagesAtEnd(result, line);
                }
                if (!result.isPrintColorModeDetected()) {
                    checkPrintColorMode(result, line);
                }
                if (!result.isSidesDetected()) {
                    checkFeatureDuplex(result, line);
                }
                if (!result.isCollateDetected()) {
                    checkFeatureCollate(result, line);
                }
                if (!result.isNumCopiesDetected()) {
                    checkNonPPDFeatureNumCopies(result, line);
                }
            }

            if (iSigLine < nSigLines && bytesRead >= minStrLenSig) {

                /*
                 * The line read does NOT have "0x0D 0x0A" or "0x0A" at the end.
                 */
                int bytesCompare = bytesRead;

                /*
                 * Do we have a DRM signature line?
                 */
                if (DRM_SIGNATURE[iSigLine].length() == bytesCompare
                        && line.equals(DRM_SIGNATURE[iSigLine])) {

                    if (fRespectDRM) {
                        /*
                         * This makes any PostScript processor like 'ps2pdf'
                         * return an error when processing this stream.
                         */
                        writer.newLine();
                        writer.write("[Error enforced BY PrintFlowLite}"); // syntax
                                                                      // error
                        writer.newLine();
                        writer.flush(); // !!!
                        result.setCode(ResultEnum.DRM_YES);
                        return result;
                    }

                    ret = ResultEnum.DRM_NEGLECTED;

                    if (iSigLine < 2 || iSigLine > 12) {
                        writer.write(line);
                        writer.newLine();
                    }

                    iSigLine++;

                } else {
                    writer.write(line);
                    writer.newLine();
                }

            } else {
                writer.write(line);
                writer.newLine();
            }

            nFlushLineCounter++;
            if (nFlushLineCounter > nFlushLinesThreshold) {
                writer.flush();
                nFlushLineCounter = 0;
            }

            /*
             * Read next
             */
            line = reader.readLine();
        }
        writer.flush();

        result.setCode(ret);
        return result;
    }

    /**
     * @param strline
     *            Line in postscript stream
     * @return {@code true} if line marks end of comment block
     */
    public static boolean isEndOfCommentLine(final String strline) {
        return strline.startsWith(PFX_END_COMMENTS)
                || strline.startsWith(PFX_BEGIN_PROLOG);
    }

    /**
     * Parses a comment line.
     *
     * @param result
     *            input/output
     * @param strline
     *            Comment to parse.
     * @param isPagesAtEnd
     *            To be filled with "true" if number of pages is found at the
     *            end of postscript stream.
     * @return {@code false} if end-of-comment.
     */
    private static boolean parseComment(final Result result,
            final String strline, final MutableBoolean isPagesAtEnd) {

        if (isEndOfCommentLine(strline)) {
            return false;
        }

        if (result.getTitle() == null && strline.startsWith(PFX_TITLE)) {
            result.setTitle(parseTitleLine(strline));
        }

        if (result.getUserId() == null && strline.startsWith(PFX_USERID)) {
            result.setUserId(parseUserIdLine(strline));
        }

        /*
         * Observed in some postscript.
         */
        if (!result.isNumCopiesDetected()
                && strline.startsWith(PFX_NUM_COPIES)) {
            result.setNumCopies(stripParentheses(
                    StringUtils.removeStart(strline, PFX_NUM_COPIES)));
        }

        /*
         * Observed in some postscript.
         */
        if (strline.startsWith(PFX_REQUIREMENTS)) {
            final String value =
                    StringUtils.removeStart(strline, PFX_REQUIREMENTS);
            if (!result.isSidesDetected()
                    && StringUtils.contains(value, PFX_REQUIREMENTS_DUPLEX)) {
                result.setTwoSidedLongEdge();
            }
        }
        /*
         * Observed in some postscript.
         */
        if (strline.startsWith(PFX_PAGES)) {
            final String value = StringUtils.removeStart(strline, PFX_PAGES);
            if (StringUtils.contains(value, PFX_PAGES_ATEND)) {
                isPagesAtEnd.setValue(Boolean.TRUE);
            }
        }

        /*
         * Observed in some postscript.
         */
        if (strline.startsWith(PFX_PJL_SET_LCOLORMODEL)) {
            final String value =
                    StringUtils.removeStart(strline, PFX_PJL_SET_LCOLORMODEL);
            if (!result.isPrintColorModeDetected()) {
                if (StringUtils.contains(value,
                        PFX_PJL_SET_LCOLORMODEL_COLOR)) {
                    result.setPrintColorModeColor();
                } else if (StringUtils.contains(value,
                        PFX_PJL_SET_LCOLORMODEL_MONOCHROME)) {
                    result.setPrintColorModeMonochrome();
                }

            }
        }

        return true;
    }

    /**
     * Checks a PostScript line for "(atend)"
     * {@link PostScriptFilter#PFX_PAGES}.
     *
     * @param result
     *            input/output
     * @param strline
     *            Comment to parse.
     */
    private static void checkPagesAtEnd(final Result result,
            final String strline) {

        if (result.getNumberOfPages() == null
                && strline.startsWith(PFX_PAGES)) {

            final String value =
                    StringUtils.removeStart(strline, PFX_PAGES).trim();

            if (StringUtils.isNumeric(value)) {
                result.setNumberOfPages(value);
            } else {
                LOGGER.warn("{} {}: {} is NOT numeric", PFX_PAGES,
                        PFX_PAGES_ATEND, value);
            }
        }
    }

    /**
     * Checks a PostScript line for
     * {@link PostScriptFilter#PFX_BEGIN_FEATURE_DUPLEX}
     * {@link PostScriptFilter#PFX_BEGIN_FEATURE}.
     *
     * @param result
     *            input/output
     * @param strline
     *            Line to parse.
     */
    private static void checkFeatureDuplex(final Result result,
            final String strline) {

        if (!result.isSidesDetected()
                && strline.startsWith(PFX_BEGIN_FEATURE)) {

            final String feature =
                    StringUtils.removeStart(strline, PFX_BEGIN_FEATURE).trim();

            if (feature.startsWith(PFX_BEGIN_FEATURE_DUPLEX)) {

                if (feature.endsWith(PFX_BEGIN_FEATURE_DUPLEX_ONE_SIDED)) {
                    result.setOneSided();
                } else if (feature.endsWith(
                        PFX_BEGIN_FEATURE_DUPLEX_TWO_SIDED_LONG_EDGE)) {
                    result.setTwoSidedLongEdge();
                } else if (feature.endsWith(
                        PFX_BEGIN_FEATURE_DUPLEX_TWO_SIDED_SHORT_EDGE)) {
                    result.setTwoSidedShortEdge();
                }
            }
        }
    }

    /**
     * Checks a PostScript line for
     * {@link PostScriptFilter#PFX_BEGIN_NON_PPD_FEATURE_NUMCOPIES}
     * {@link PostScriptFilter#PFX_BEGIN_NON_PPD_FEATURE}.
     *
     * @param result
     *            input/output
     * @param strline
     *            Line to parse.
     */
    private static void checkNonPPDFeatureNumCopies(final Result result,
            final String strline) {

        if (!result.isNumCopiesDetected()
                && strline.startsWith(PFX_BEGIN_NON_PPD_FEATURE)) {

            final String feature = StringUtils
                    .removeStart(strline, PFX_BEGIN_NON_PPD_FEATURE).trim();

            if (feature.startsWith(PFX_BEGIN_NON_PPD_FEATURE_NUMCOPIES)) {
                result.setNumCopies(
                        StringUtils
                                .removeStart(feature,
                                        PFX_BEGIN_NON_PPD_FEATURE_NUMCOPIES)
                                .trim());
            }
        }

    }

    /**
     * Checks a PostScript line for
     * {@link PostScriptFilter#PFX_BEGIN_FEATURE_COLLATE}
     * {@link PostScriptFilter#PFX_BEGIN_FEATURE}.
     *
     * @param result
     *            input/output
     * @param strline
     *            Line to parse.
     */
    private static void checkFeatureCollate(final Result result,
            final String strline) {

        if (!result.isCollateDetected()) {

            if (strline.startsWith(PFX_BEGIN_FEATURE)) {
                final String feature = StringUtils
                        .removeStart(strline, PFX_BEGIN_FEATURE).trim();
                if (feature.startsWith(PFX_BEGIN_FEATURE_COLLATE)) {
                    result.setCollate(
                            feature.endsWith(PFX_BEGIN_FEATURE_COLLATE_TRUE));
                }
            } else if (strline.contains(SCAN_FEATUREBEGIN_PFX)) {
                if (strline.contains(SCAN_FEATUREBEGIN_COLLATE_TRUE)) {
                    result.setCollate(true);
                } else if (strline.contains(SCAN_FEATUREBEGIN_COLLATE_FALSE)) {
                    result.setCollate(false);
                }
            }
        }
    }

    /**
     * Checks a PostScript line for
     * {@link PostScriptFilter#PFX_PROCESS_COLORMODEL}.
     *
     * @param result
     *            input/output
     * @param strline
     *            Line to parse.
     */
    private static void checkPrintColorMode(final Result result,
            final String strline) {

        if (!result.isPrintColorModeDetected()) {

            if (strline.startsWith(PFX_PROCESS_COLORMODEL)) {

                final String feature = StringUtils
                        .removeStart(strline, PFX_PROCESS_COLORMODEL).trim();

                if (feature.startsWith(PFX_PROCESS_COLORMODEL_COLOR)) {
                    result.setPrintColorModeColor();
                } else if (feature
                        .startsWith(PFX_PROCESS_COLORMODEL_MONOCHROME)) {
                    result.setPrintColorModeMonochrome();
                }
            } else if (strline.contains(SCAN_FEATUREBEGIN_PFX)) {
                if (strline.contains(SCAN_FEATUREBEGIN_PROCESSCOLORMODEL)) {
                    if (strline.contains(
                            SCAN_FEATUREBEGIN_PROCESSCOLORMODEL_COLOR)) {
                        result.setPrintColorModeColor();
                    } else if (strline.contains(
                            SCAN_FEATUREBEGIN_PROCESSCOLORMODEL_MONOCHROME)) {
                        result.setPrintColorModeMonochrome();
                    }
                }
            }
        }
    }

    /**
     * Strips leading/trailing parenthesis from a string.
     *
     * @param content
     *            The string to strip.
     * @return The stripped string.
     */
    private static String stripParentheses(final String content) {
        return StringUtils
                .removeEnd(StringUtils.removeStart(content.trim(), "("), ")");
    }

    /**
     * Parses PostScript comment lines and maps content to IPP options.
     *
     * @param headerLines
     *            List of header lines.
     * @return Map of IPP options.
     */
    public static IppOptionMap
            parseCommentLines(final List<String> headerLines) {

        final Result result = new Result();
        final MutableBoolean isPagesAtEnd = new MutableBoolean(false);

        for (final String line : headerLines) {
            if (!parseComment(result, line, isPagesAtEnd)) {
                break;
            }
        }
        return result.ippOptionsWrapper;
    }

    /**
     * Parses a {@link #PFX_TITLE} line.
     *
     * @param strline
     *            Line to parse.
     * @return Parsing result.
     */
    public static String parseTitleLine(final String strline) {
        return PostScriptTitleProcessor.process(
                stripParentheses(StringUtils.removeStart(strline, PFX_TITLE)));
    }

    /**
     * Parses a {@link #PFX_USERID} line.
     *
     * @param strline
     *            Line to parse.
     * @return Parsing result.
     */
    public static String parseUserIdLine(final String strline) {
        return stripParentheses(
                StringUtils.removeStart(strline, PostScriptFilter.PFX_USERID));
    }

}
