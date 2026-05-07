/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2024 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.printflow.lite.core.json.PdfProperties;

/**
 * Creates {@link PDAnnotationLink} on URI text.
 *
 * @author Rijk Ravestein
 *
 */
public final class PDFBoxUrlAnnotator extends PDFTextStripper
        implements IPdfUrlAnnotator {

    /** */
    private final PDDocument document;

    /** */
    private final PDBorderStyleDictionary borderULine =
            new PDBorderStyleDictionary();

    /** */
    private final PDColor underlineColor;

    /** */
    private float pageHeightWlk;

    /** */
    private List<PDAnnotation> pageAnnotationsWlk;

    /**
     * Percentage of text rectangle height used to calculate annotation
     * underline padding.
     */
    private static final float UNDERLINE_PADDING_PERC = 0.30f;

    /**
     * @param doc
     *            PDF document.
     * @param links
     *            PDF links specification.
     */
    private PDFBoxUrlAnnotator(final PDDocument doc,
            final PdfProperties.PdfLinks links) {

        this.document = doc;

        this.borderULine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
        this.borderULine.setWidth(links.getWidth().getWidth());

        this.underlineColor = new PDColor(links.getColor().asFloatArray(),
                PDDeviceRGB.INSTANCE);
    }

    /**
     * Writes text.
     *
     * @throws IOException
     */
    private void writeText() throws IOException {
        this.writeText(this.document,
                new OutputStreamWriter(OutputStream.nullOutputStream()));
    }

    /**
     * Annotates URL links in PDF document.
     *
     * @param document
     *            PDF document.
     * @param links
     *            PDF links specification.
     * @throws IOException
     *             If IO error.
     */
    public static void annotate(final PDDocument document,
            final PdfProperties.PdfLinks links) throws IOException {
        new PDFBoxUrlAnnotator(document, links).writeText();
    }

    @Override
    protected void startPage(final PDPage page) throws IOException {
        super.startPage(page);
        this.pageHeightWlk = page.getMediaBox().getHeight();
        this.pageAnnotationsWlk = page.getAnnotations();
    }

    @Override
    protected void writeString(final String string,
            final List<TextPosition> textPositions) throws IOException {

        for (final PdfUrlAnnotationMatch match : PdfUrlAnnotationMatch
                .findLinks(string)) {

            final TextPosition textStart = textPositions.get(match.getStart());
            final TextPosition textEnd = textPositions.get(match.getEnd() - 1);
            final float underlinePadding =
                    textStart.getHeight() * UNDERLINE_PADDING_PERC;
            /*
             * Rectangle containing the markup.
             */
            final PDRectangle position = new PDRectangle();
            position.setLowerLeftX(textStart.getXDirAdj());
            position.setLowerLeftY(this.pageHeightWlk - textStart.getYDirAdj()
                    - underlinePadding);

            position.setUpperRightX(textEnd.getXDirAdj() + textEnd.getWidth());
            position.setUpperRightY(this.pageHeightWlk - textEnd.getYDirAdj()
                    + textEnd.getHeight());
            /*
             * URI action.
             */
            final PDActionURI action = new PDActionURI();
            action.setURI(match.getUrl().toString());
            /*
             * Add link annotation.
             */
            final PDAnnotationLink txtLink = new PDAnnotationLink();
            txtLink.setBorderStyle(this.borderULine);
            txtLink.setColor(this.underlineColor);
            txtLink.setRectangle(position);
            txtLink.setAction(action);

            this.pageAnnotationsWlk.add(txtLink);
        }
    }

}
