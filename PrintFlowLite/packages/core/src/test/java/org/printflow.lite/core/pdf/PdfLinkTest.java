/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PdfLinkTest {

    /**
     * .
     */
    public void testMatched(final String textAfterLink) {

        final Set<String> linkSet = new HashSet<>();

        final String[] links = new String[] { //
                //
                "https://www.example.com/index.php?x=1&2=5",
                //
                "http://example.com/index.php?x=1&2=5",
                //
                "http://www.example.com/",
                //
                "www.example.com/path",
                //
                "http://www.example.com/path",
                //
                "http://WWW.example.com/",
                //
                "ftp://www.example.com",
                //
                "info@example.com",
                //
                "mailto:info@example.com",
                //
                "http://localhost:81/intranet/xyz",
                //
                "https://secure.example.com/path",
                //
        };

        /*
         * Build text, embed links in noise words.
         */
        final StringBuilder builder = new StringBuilder(512);

        for (final String link : links) {
            linkSet.add(link);
            builder.append(textAfterLink).append(link);
        }

        /*
         * Every match must match entry in the linkSet.
         */
        final Set<String> matchSet = new HashSet<>();

        for (final PdfUrlAnnotationMatch match : PdfUrlAnnotationMatch
                .findLinks(builder.toString())) {
            final String link = match.getText();
            matchSet.add(link);
            assertTrue(linkSet.contains(link),
                    String.format("[%s] must be found", link));
        }

        /*
         * Every entry in the link Set must be matched.
         */
        assertTrue(matchSet.size() == linkSet.size(), "All links are matched");

        for (final String link : linkSet.toArray(new String[0])) {
            assertTrue(matchSet.contains(link),
                    String.format("[%s] is matched", link));
        }

    }

    /**
     * .
     */
    @Test
    public void testMatched() {
        testMatched(" some text in between ");
        testMatched(". A new sentence ");
        testMatched(", a clause, ");
        testMatched(": a statement. ");
        testMatched("; some text. ");
    }

    /**
     * .
     */
    @Test
    public void testNonMatched() {

        final String[] links = new String[] { //
                //
                "xxxx://example.com/index.php?x=1&2=5",
                //
                "example.com/index.php?x=1&2=5",
                //
                "info$example.com",
                //
                "mailto:info#example.com",
                //
                "httpx://localhost:81/intranet/xyz",
                //
                "httpxs://secure.example.com/path"
                //
                // String below are matched on "www." and xxx@xxx.xx
                //
                // "xxxx://www.example.com/index.php?x=1&2=5",
                //
                // "xxxx://www.example.com/",
                //
                // "xxxx://WWW.example.com/",
                //
                // "ftpx://www.example.com",
                //
                // "@info@example.com",
                //
        };

        /*
         * Build text, embed links in noise words.
         */
        final StringBuilder builder = new StringBuilder(512);

        for (final String link : links) {
            builder.append(" some text ").append(link);
        }

        /*
         * Every match must match entry in the linkSet.
         */
        final List<PdfUrlAnnotationMatch> matchList =
                PdfUrlAnnotationMatch.findLinks(builder.toString());

        for (final PdfUrlAnnotationMatch match : matchList) {
            System.out.println(match.getText());
        }

        /*
         * Every entry in the link Set must be matched.
         */
        assertTrue(matchList.isEmpty(), "All links are non-matched");

    }

    /**
     * .
     */
    @Test
    public void testOneLine() {

        final String urlA = "www.x.nl";
        final String urlB = "info@x.nl";
        final int nSpacing = 10;

        final String text = String.format("%s%s%s", urlA,
                StringUtils.repeat(' ', nSpacing), urlB);

        final List<PdfUrlAnnotationMatch> matchList =
                PdfUrlAnnotationMatch.findLinks(text);

        assertTrue(matchList.size() == 2, "All links are non-matched");

        PdfUrlAnnotationMatch match = matchList.get(0);

        assertTrue(match.getStart() == 0, "match [0] starts at position");
        assertTrue(match.getEnd() == urlA.length(),
                "match [0] ends on position");

        //
        match = matchList.get(1);

        assertTrue(match.getStart() == urlA.length() + nSpacing,
                "match [1] starts at position");
        assertTrue(match.getEnd() == text.length(),
                "match [1] ends on position");

    }

    /**
     * .
     */
    @Test
    public void testSuffixPunctuation() {

        final Set<String> linkSet = new HashSet<>();

        final String[] links = new String[] {
                //
                // "www.example.com/path",
                //
                "http://www.example.com/path" };

        /*
         * Build text, embed links in noise words.
         */
        final StringBuilder builder = new StringBuilder(512);

        for (final String link : links) {
            linkSet.add(link);
            builder.append(". some text ").append(link);
        }

        /*
         * Every match must match entry in the linkSet.
         */
        final Set<String> matchSet = new HashSet<>();

        for (final PdfUrlAnnotationMatch match : PdfUrlAnnotationMatch
                .findLinks(builder.toString())) {
            final String link = match.getText();
            matchSet.add(link);
            assertTrue(linkSet.contains(link),
                    String.format("[%s] must be found", link));
        }

        /*
         * Every entry in the link Set must be matched.
         */
        assertTrue(matchSet.size() == linkSet.size(), "All links are matched");

        for (final String link : linkSet.toArray(new String[0])) {
            assertTrue(matchSet.contains(link),
                    String.format("[%s] is matched", link));
        }

    }

}
