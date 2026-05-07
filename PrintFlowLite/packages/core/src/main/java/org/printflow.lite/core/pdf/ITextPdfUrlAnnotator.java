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
package org.printflow.lite.core.pdf;

import java.io.IOException;
import java.net.URL;

import org.printflow.lite.core.json.PdfProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfBorderDictionary;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.ContentByteUtils;
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

/**
 * Creates {@link PdfAnnotation} on URL text.
 *
 * @author Rijk Ravestein
 *
 */
public final class ITextPdfUrlAnnotator
        implements TextExtractionStrategy, IPdfUrlAnnotator {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ITextPdfUrlAnnotator.class);

    /**
     * Percentage of text rectangle height used to calculate annotation
     * rectangle padding. Note: this is a simulation/approximation of font
     * ascend/descend.
     */
    private static final float ANNOTATION_TEXT_PADDING_PERC = 0.25f;

    /** */
    private final BaseColor underlineColor;

    /**
     * The {@link PdfStamper} to annotate the PDF links on.
     */
    private final PdfStamper stamper;

    /**
     * The 1-based page ordinal of the stamper to add the annotation on.
     */
    private final int nStamperPage;

    /** */
    private final boolean isPageSeenAsLandscape;

    /** */
    private final int pageRotation;

    /** */
    private final Rectangle pageRectangle;

    /**
     * Annotation border width or {@code 0} (zero) if not applicable.
     */
    private final float borderWidth;

    /** */
    private TextRenderInfo textRenderInfoStartWlk;

    /** */
    private Rectangle rectangleFirstWlk;

    /** */
    private Rectangle rectangleLastWlk;

    /** */
    private StringBuilder collectedTextWlk = new StringBuilder();

    /**
     * Constructor.
     *
     * @param target
     *            The {@link PdfStamper} to annotate the PDF links on.
     * @param nPage
     *            The 1-based page ordinal of the stamper to add the annotation
     *            on.
     * @param links
     *            PDF links specification.
     */
    public ITextPdfUrlAnnotator(final PdfStamper target, final int nPage,
            final PdfProperties.PdfLinks links) {

        this.stamper = target;
        this.nStamperPage = nPage;

        this.borderWidth = links.getWidth().getWidth();

        final float[] underlineRGB = links.getColor().asFloatArray();
        this.underlineColor = new BaseColor(underlineRGB[0], underlineRGB[1],
                underlineRGB[2]);

        try {
            final PdfReader reader = this.stamper.getReader();

            this.isPageSeenAsLandscape = PdfPageRotateHelper
                    .isSeenAsLandscape(reader, this.nStamperPage);
            this.pageRotation = reader.getPageRotation(nStamperPage);
            this.pageRectangle = reader.getPageSize(this.nStamperPage);

        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Adds an {@link URL} annotation to the {@link PdfStamper}.
     *
     * @param llx
     *            Lower left x.
     * @param lly
     *            Lower left y.
     * @param urx
     *            Upper Right x.
     * @param ury
     *            Upper Right y.
     * @param url
     *            The {@link URL}.
     */
    private void addAnnotation(final float llx, final float lly,
            final float urx, final float ury, final URL url) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    String.format("PDF url [%s] at x|y lower %f|%f upper %f|%f",
                            url.toExternalForm(), llx, lly, urx, ury));
        }

        final PdfAction action = new PdfAction(url);

        final PdfAnnotation annLink = new PdfAnnotation(stamper.getWriter(),
                llx, lly, urx, ury, action);
        annLink.setColor(this.underlineColor);

        if (this.borderWidth > 0) {
            annLink.setBorderStyle(new PdfBorderDictionary(this.borderWidth,
                    PdfBorderDictionary.STYLE_UNDERLINE));
        }

        stamper.addAnnotation(annLink, this.nStamperPage);
    }

    /**
     * @param info
     *            The {@link TextRenderInfo}.
     * @return The bounding rectangle of the rendered text.
     */
    private Rectangle getRectangle(final TextRenderInfo info) {

        final Vector lowerLeftWlk = info.getBaseline().getStartPoint();
        final Vector upperRightWlk = info.getAscentLine().getEndPoint();

        return new Rectangle(lowerLeftWlk.get(0), lowerLeftWlk.get(1),
                upperRightWlk.get(0), upperRightWlk.get(1));
    }

    /**
     * Checks the collected text for links and adds annotations.
     */
    private void checkCollectedText() {

        if (this.textRenderInfoStartWlk == null) {
            return;
        }
        if (this.rectangleFirstWlk.getTop() < this.rectangleFirstWlk
                .getBottom()) {
            LOGGER.warn("Skip text: unsupported rotation.");
            return;
        }
        /*
         * Annotation coordinates must be converted from technical PDF text
         * coordinates to the "logical" coordinates as perceived by user.
         */
        final float llx;
        final float lly;
        final float urx;
        final float ury;

        // Padding
        final float llxPadding;
        final float llyPadding;
        final float urxPadding;
        final float uryPadding;

        final float techPadding;

        // Determine technical orientation of text.
        final boolean textTechHorizontal = this.rectangleFirstWlk
                .getLeft() < this.rectangleFirstWlk.getRight();

        final boolean textSeenVertical;

        if (textTechHorizontal) {

            textSeenVertical =
                    this.isPageSeenAsLandscape && this.pageRotation != 0;

            if (textSeenVertical) {

                if (this.pageRotation != PdfPageRotateHelper.ROTATION_90) {
                    // TODO
                    LOGGER.warn(
                            "Page [{}] Rotation [{}] to Landscape: "
                                    + "not implemented yet.",
                            this.nStamperPage, this.pageRotation);
                    return;
                }

                final float perceivedPageHeight = this.pageRectangle.getWidth();

                llx = this.rectangleFirstWlk.getBottom();
                lly = perceivedPageHeight - this.rectangleLastWlk.getRight();
                urx = this.rectangleLastWlk.getTop();
                ury = perceivedPageHeight - this.rectangleFirstWlk.getLeft();

                techPadding = (urx - llx) * ANNOTATION_TEXT_PADDING_PERC;

                llxPadding = 0; // -techPadding;
                llyPadding = -techPadding;
                urxPadding = 0; // techPadding;
                uryPadding = techPadding;

            } else {

                llx = this.rectangleFirstWlk.getLeft();
                lly = this.rectangleFirstWlk.getBottom();
                urx = this.rectangleLastWlk.getRight();
                ury = this.rectangleLastWlk.getTop();

                techPadding = (ury - lly) * ANNOTATION_TEXT_PADDING_PERC;

                llxPadding = 0; // -techPadding;
                llyPadding = -techPadding;
                urxPadding = 0; // techPadding;
                uryPadding = techPadding;
            }

        } else {

            textSeenVertical = false;

            // PDF Text is technically vertical.
            final float heightCorrection;

            if (this.isPageSeenAsLandscape && this.pageRotation != 0) {
                heightCorrection = this.pageRectangle.getWidth();
            } else {
                heightCorrection = this.pageRectangle.getHeight();
            }

            llx = this.rectangleFirstWlk.getBottom();
            lly = heightCorrection - this.rectangleFirstWlk.getLeft();
            urx = this.rectangleLastWlk.getTop();
            ury = heightCorrection - this.rectangleLastWlk.getRight();

            techPadding = (ury - lly) * ANNOTATION_TEXT_PADDING_PERC;

            llxPadding = 0; // -techPadding;
            llyPadding = -techPadding;
            urxPadding = 0; // techPadding;
            uryPadding = techPadding;
        }

        final Rectangle infoRectTotal = new Rectangle(llx, lly, urx, ury);

        //
        final TextRenderInfo info = this.textRenderInfoStartWlk;
        final String text = this.collectedTextWlk.toString();
        final float fontWidthTotal = info.getFont().getWidth(text);

        for (final PdfUrlAnnotationMatch match : PdfUrlAnnotationMatch
                .findLinks(text)) {

            final String prefix = text.substring(0, match.getStart());

            /*
             * Get the font width of text parts.
             */
            final float fontWidthPrefix = info.getFont().getWidth(prefix);

            final float fontWidthAnnotation =
                    info.getFont().getWidth(match.getText());

            final float llxWlk;
            final float llyWlk;
            final float urxWlk;
            final float uryWlk;

            if (textSeenVertical) {

                final float infoWidthPrefix = infoRectTotal.getHeight()
                        * fontWidthPrefix / fontWidthTotal;

                final float infoWidthAnnotation = infoRectTotal.getHeight()
                        * fontWidthAnnotation / fontWidthTotal;

                final float infoTopWlk =
                        infoRectTotal.getTop() - infoWidthPrefix;
                final float infoBottomWlk = infoTopWlk - infoWidthAnnotation;

                llxWlk = infoRectTotal.getLeft();
                llyWlk = infoBottomWlk;
                urxWlk = infoRectTotal.getRight();
                uryWlk = infoTopWlk;

            } else {

                final float infoWidthPrefix = infoRectTotal.getWidth()
                        * fontWidthPrefix / fontWidthTotal;

                final float infoWidthAnnotation = infoRectTotal.getWidth()
                        * fontWidthAnnotation / fontWidthTotal;

                final float infoLeftWlk =
                        infoRectTotal.getLeft() + infoWidthPrefix;
                final float infoRightWlk = infoLeftWlk + infoWidthAnnotation;

                llxWlk = infoLeftWlk;
                llyWlk = infoRectTotal.getBottom();
                urxWlk = infoRightWlk;
                uryWlk = infoRectTotal.getBottom() + infoRectTotal.getHeight();
            }

            this.addAnnotation(llxWlk + llxPadding, llyWlk + llyPadding,
                    urxWlk + urxPadding, uryWlk + uryPadding, match.getUrl());
        }

        this.textRenderInfoStartWlk = null;
    }

    @Override
    public void renderText(final TextRenderInfo info) {

        final String text = info.getText();
        final Rectangle rectangle = getRectangle(info);

        final boolean checkCollectedText;

        if (this.textRenderInfoStartWlk != null //
                // same line
                && rectangle.getBottom() == this.rectangleFirstWlk.getBottom()
                // same font
                && info.getFont().getPostscriptFontName()
                        .equals(this.textRenderInfoStartWlk.getFont()
                                .getPostscriptFontName())) {
            /*
             * How to check same word consistently for all kind of PDFs?
             *
             * For now, if x-left of this rendered text is less then half a
             * space of x-right of the previous rendered text, we consider same
             * word.
             */
            final boolean sameWord =
                    (rectangle.getLeft() - rectangleLastWlk.getRight()) < info
                            .getSingleSpaceWidth() / 2;

            checkCollectedText = !sameWord;

        } else if (this.textRenderInfoStartWlk != null //
                // rotated
                && rectangle.getLeft() > rectangle.getRight() //
                // same rotated line
                && rectangle.getLeft() == this.rectangleFirstWlk.getLeft()
                // same font
                && info.getFont().getPostscriptFontName()
                        .equals(this.textRenderInfoStartWlk.getFont()
                                .getPostscriptFontName())) {
            /*
             * How to check same word consistently for all kind of PDFs?
             *
             * For now, if y-bottom of this rendered text is less then half a
             * space of y-top of the previous rendered text, we consider same
             * word.
             */
            final boolean sameWord =
                    (rectangle.getBottom() - rectangleLastWlk.getTop()) < info
                            .getSingleSpaceWidth() / 2;

            checkCollectedText = !sameWord;

        } else {
            checkCollectedText = true;
        }

        if (checkCollectedText) {

            this.checkCollectedText();

            this.textRenderInfoStartWlk = info;
            this.collectedTextWlk = new StringBuilder();
            this.rectangleFirstWlk = rectangle;
        }

        this.collectedTextWlk.append(text);

        this.rectangleLastWlk = rectangle;
    }

    @Override
    public void renderImage(final ImageRenderInfo arg0) {
        // noop
    }

    @Override
    public void endTextBlock() {
        // noop
    }

    @Override
    public void beginTextBlock() {
        // noop
    }

    @Override
    public String getResultantText() {
        return this.collectedTextWlk.toString();
    }

    /**
     * Annotates URL links in PDF file.
     *
     * @param reader
     *            PDF in.
     * @param stamper
     *            PDF out.
     * @param links
     *            PDF links specification.
     * @throws IOException
     *             If IO error.
     */
    public static void annotate(final PdfReader reader,
            final PdfStamper stamper, final PdfProperties.PdfLinks links)
            throws IOException {

        final int pageCount = reader.getNumberOfPages();

        for (int i = 1; i <= pageCount; i++) {

            final ITextPdfUrlAnnotator delegate =
                    new ITextPdfUrlAnnotator(stamper, i, links);

            final FilteredTextRenderListener listener =
                    new FilteredTextRenderListener(delegate);

            final PdfContentStreamProcessor processor =
                    new PdfContentStreamProcessor(listener);

            final PdfDictionary pageDic = reader.getPageN(i);

            final PdfDictionary resourcesDic =
                    pageDic.getAsDict(PdfName.RESOURCES);

            try {
                final byte[] content =
                        ContentByteUtils.getContentBytesForPage(reader, i);

                processor.processContent(content, resourcesDic);

            } catch (ExceptionConverter e) {
                // TODO
                LOGGER.warn(String.format("%s [%s]",
                        e.getClass().getSimpleName(), e.getMessage()));
                LOGGER.debug(e.getMessage(), e);
            }

            // Flush remaining text
            delegate.checkCollectedText();
        }

    }
}
