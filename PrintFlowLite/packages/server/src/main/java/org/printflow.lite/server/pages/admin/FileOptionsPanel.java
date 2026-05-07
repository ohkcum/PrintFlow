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
package org.printflow.lite.server.pages.admin;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.util.FileNameExtEnum;
import org.printflow.lite.server.pages.MarkupHelper;

/**
 * Panel with HTML {@code option} tags.
 *
 * @author Rijk Ravestein
 *
 */
public final class FileOptionsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_OPTION_PANEL = "panel";
    /** */
    private static final String WID_OPTION = "option";

    /** */
    private final File scanDirectory;

    /** */
    private final FileNameExtEnum fileNameExtension;

    /**
     *
     * @param widPanel
     *            Wicket ID.
     * @param scanDir
     *            directory to scan for files.
     * @param ext
     *            filename extension.
     */
    public FileOptionsPanel(final String widPanel, final File scanDir,
            final FileNameExtEnum ext) {
        super(widPanel);
        this.scanDirectory = scanDir;
        this.fileNameExtension = ext;
    }

    /** */
    public void populate() {

        final FileOptionsPanel parentClass = this;

        final List<String> fileList = new ArrayList<>();
        fileList.add(""); // add option to de-select.

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path path,
                    final BasicFileAttributes attrs) throws IOException {

                final File file = path.toFile();

                if (file.isFile() && !file.isHidden()
                        && FilenameUtils.isExtension(file.getName(),
                                parentClass.fileNameExtension.ext())) {
                    fileList.add(file.getName());
                }
                return CONTINUE;
            }
        };

        final Set<FileVisitOption> options = new HashSet<>();
        try {
            Files.walkFileTree(Paths.get(this.scanDirectory.getAbsolutePath()),
                    options, 1, visitor);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }

        java.util.Collections.sort(fileList, String.CASE_INSENSITIVE_ORDER);

        this.add(new PropertyListView<String>(WID_OPTION_PANEL, fileList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {

                final String fileName = item.getModel().getObject();

                final Label label = new Label(WID_OPTION, fileName);
                label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                        fileName));
                item.add(label);
            }
        });
    }

}
