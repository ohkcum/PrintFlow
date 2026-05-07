/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.printflow.lite.common.IUtility;
import org.printflow.lite.core.dto.ColorPageCounterDto;
import org.printflow.lite.core.inbox.RangeAtom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects color in various sources.
 *
 * @author Rijk Ravestein
 *
 */
public final class ColorDetectionUtil implements IUtility {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ColorDetectionUtil.class);

    /** Utility class. */
    private ColorDetectionUtil() {
    }

    /**
     * Image resolution.
     *
     * @author Rijk Ravestein
     *
     */
    private enum ImageRenderResolution {
        /** Good enough to detect color. */
        DPI_36(36f),
        /** Suitable for web pages. */
        DPI_72(72f);

        /** */
        private final float dpi;

        ImageRenderResolution(final float res) {
            this.dpi = res;
        }

        /**
         * @return resolution in DPI.
         */
        public float getDPI() {
            return this.dpi;
        }
    }

    /**
     * Checks if RGB image is monochrome.
     *
     * @param imageRGB
     * @return {@code true} if monochrome.
     */
    public static boolean isMonochrome(final BufferedImage imageRGB) {

        final int width = imageRGB.getWidth();
        final int height = imageRGB.getHeight();

        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                final Color pixelColor = new Color(imageRGB.getRGB(x, y));
                final int green = pixelColor.getGreen();

                /*
                 * Monochrome: Red = Green = Blue.
                 */
                if (pixelColor.getRed() != green
                        || green != pixelColor.getBlue()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Collects color page info of PDF file.
     *
     * @param pdfFile
     * @return color page info.
     * @throws IOException
     */
    public static ColorPageCounterDto getColorPagesPDF(final File pdfFile)
            throws IOException {

        final List<RangeAtom> colorRanges = new ArrayList<>();
        int colorPageTotal = 0;
        int pageTotal = 0;

        int nColorStart = 0;

        try (PDDocument document = Loader.loadPDF(pdfFile);) {

            final PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {

                boolean monochrome;

                try {
                    final BufferedImage imageRGB =
                            pdfRenderer.renderImageWithDPI(page,
                                    ImageRenderResolution.DPI_36.getDPI(),
                                    ImageType.RGB);

                    monochrome = isMonochrome(imageRGB);

                } catch (Exception e) { // see Mantis #1330
                    /*
                     * For instance : java.awt.color.CMMException thrown by
                     * PDFRenderer.
                     */
                    LOGGER.debug("{} : {}", e.getClass().getSimpleName(),
                            e.getMessage());

                    monochrome = false; // assume color
                }

                if (monochrome) {
                    if (nColorStart > 0) {
                        colorRanges.add(
                                RangeAtom.fromPageRange(nColorStart, page));
                        nColorStart = 0;
                    }
                } else {
                    colorPageTotal++;
                    if (nColorStart == 0) {
                        nColorStart = page + 1;
                    }
                }
                pageTotal++;
            }
            if (nColorStart > 0) {
                colorRanges.add(RangeAtom.fromPageRange(nColorStart,
                        document.getNumberOfPages()));
            }
        }
        return new ColorPageCounterDto(colorRanges, colorPageTotal, pageTotal);
    }

    /**
     * Checks if base64 encoded SVG contains color RGB. Note: embedded images
     * are <b>not</b> checked.
     *
     * @param svg64
     *            base64 SVG.
     * @return {@code true} if SVG contains color RGB.
     */
    public static boolean isColorRGBinSVG64(final String svg64) {
        final byte[] decoded = Base64.getDecoder().decode(svg64);
        final String svg = new String(decoded, StandardCharsets.UTF_8);
        return isColorRGBinSVG(svg);
    }

    /**
     * Checks if plain SVG contains color RGB. Note: embedded images are
     * <b>not</b> checked.
     *
     * @param svgPlain
     *            base64 SVG.
     * @return {@code true} if SVG contains color RGB.
     */
    public static boolean isColorRGBinSVG(final String svgPlain) {

        final String svg = svgPlain.toLowerCase(); // just to be sure.
        final byte[] svgBytes = svg.getBytes();

        final String rgbTarget = " rgb"; // lower case!
        final int nRGB = 3;
        final String[] rgbPart = new String[nRGB];

        // Read first.
        int iPosStart = 0;
        int iPosFound = StringUtils.indexOf(svg, rgbTarget, iPosStart);

        while (iPosFound >= 0) {
            // Extract R,G,B values
            int iRgbPart = 0;
            for (iPosStart = iPosFound + rgbTarget
                    .length(); svgBytes[iPosStart] != ')'; iPosStart++) {

                final char ch = (char) svgBytes[iPosStart];

                if (Character.isDigit(ch)) {
                    rgbPart[iRgbPart] += ch;
                } else if (ch == ',') {
                    iRgbPart++;
                }
            }
            // Evaluate monochrome: R = G = B
            for (iRgbPart = 1; iRgbPart < rgbPart.length; iRgbPart++) {
                if (!rgbPart[iRgbPart].equals(rgbPart[iRgbPart - 1])) {
                    return true; // RGB is color
                }
            }
            // Read next.
            iPosFound = StringUtils.indexOf(svg, rgbTarget, iPosStart);
        }
        return false;
    }

}
