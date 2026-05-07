/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfUrlAnnotationMatch {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfUrlAnnotationMatch.class);

    /**
     * Regular expression for email address.
     */
    private static final String REGEX_EMAIL_ADDRESS =
            "[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";

    /**
     * Pattern for email address.
     */
    private static final String PATTERN_EMAIL = "\\b" + REGEX_EMAIL_ADDRESS;

    /**
     * Full mailto: pattern.
     */
    private static final String PATTERN_MAILTO =
            "\\b(mailto:)" + REGEX_EMAIL_ADDRESS;

    /**
     * Note: suffix punctuation {@code :,.;} is ignored.
     */
    private static final String REGEX_URL_WITHOUT_SCHEME =
            "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    /**
     * URL pattern for www.*.
     */
    private static final String PATTERN_WWW =
            "\\b(www|WWW)\\." + REGEX_URL_WITHOUT_SCHEME;

    /**
     * Full URL pattern.
     */
    private static final String PATTERN_URL =
            "\\b(https?|ftp|file)://" + REGEX_URL_WITHOUT_SCHEME;

    /** */
    private final String text;
    private final int start;
    private final int end;
    private final URL url;

    public String getText() {
        return text;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public URL getUrl() {
        return url;
    }

    /**
     *
     * @param text
     * @param start
     * @param end
     * @param url
     */
    public PdfUrlAnnotationMatch(final String text, final int start,
            final int end, final URL url) {

        this.text = text;
        this.start = start;
        this.end = end;
        this.url = url;
    }

    /**
     * Finds {@link AnnotationMatch} instances in a text string.
     *
     * @param pattern
     *            The {@link Pattern} to find the instances.
     * @param text
     *            The input string to search.
     * @param urlFormat
     *            The format string as in
     *            {@link String#format(String, Object...)} to create the
     *            {@link URL} in the {@link AnnotationMatch}.
     * @return A list of {@link AnnotationMatch} instances.
     */
    public static List<PdfUrlAnnotationMatch> findLinks(final Pattern pattern,
            final String text, final String urlFormat) {

        final List<PdfUrlAnnotationMatch> matchList = new ArrayList<>();
        final Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                final String urlText =
                        String.format(urlFormat, matcher.group());
                final URL url = new URL(urlText);
                matchList.add(new PdfUrlAnnotationMatch(matcher.group(),
                        matcher.start(), matcher.end(), url));
            } catch (MalformedURLException e) {
                // Log and ignore
                LOGGER.warn(e.getMessage());
            }
        }

        return matchList;
    }

    /**
     * Finds {@link AnnotationMatch} instances in a text string.
     *
     * @param text
     *            The input string to search.
     * @return A list of {@link AnnotationMatch} instances.
     */
    public static List<PdfUrlAnnotationMatch> findLinks(final String text) {

        final List<PdfUrlAnnotationMatch> matchListTot = new ArrayList<>();

        List<PdfUrlAnnotationMatch> matchList;

        //
        String searchText = text;

        matchList = findLinks(Pattern.compile(PATTERN_URL), searchText, "%s");

        char[] textAsChars = searchText.toCharArray();

        for (final PdfUrlAnnotationMatch match : matchList) {
            /*
             * Wipe to prevent duplicate matches.
             */
            for (int i = match.getStart(); i < match.getEnd(); i++) {
                textAsChars[i] = ' ';
            }
        }
        matchListTot.addAll(matchList);

        //
        searchText = new String(textAsChars);

        matchList = findLinks(Pattern.compile(PATTERN_WWW), searchText,
                "https://%s");
        matchListTot.addAll(matchList);

        //
        matchList =
                findLinks(Pattern.compile(PATTERN_MAILTO), searchText, "%s");

        textAsChars = searchText.toCharArray();

        for (final PdfUrlAnnotationMatch match : matchList) {
            /*
             * Wipe to prevent duplicate matches.
             */
            for (int i = match.getStart(); i < match.getEnd(); i++) {
                textAsChars[i] = ' ';
            }
        }

        matchListTot.addAll(matchList);

        //
        searchText = new String(textAsChars);

        matchList = findLinks(Pattern.compile(PATTERN_EMAIL), searchText,
                "mailto:%s");
        matchListTot.addAll(matchList);

        return matchListTot;
    }

}
