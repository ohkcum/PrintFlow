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
package org.printflow.lite.core.util;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class FileSystemHelper {

    /**
     *
     */
    private FileSystemHelper() {
    }

    /**
     * Performs a file move as an ATOMIC_MOVE file operation. If the file system
     * does not support an atomic move, an exception is thrown. With an
     * ATOMIC_MOVE you can move a file into a directory and be guaranteed that
     * any process watching the directory accesses a complete file.
     *
     * @param source
     *            The source path.
     * @param target
     *            The target path.
     * @throws IOException
     *             If any IO error occurs.
     */
    public static void doAtomicFileMove(final Path source, final Path target)
            throws IOException {

        java.nio.file.Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Removes a directory by recursively deleting files and sub directories.
     *
     * @param path
     *            The {@link Path} of the directory to remove.
     * @throws IOException
     *             When IO errors.
     */
    public static void removeDir(final Path path) throws IOException {

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir,
                    final IOException exc) throws IOException {

                if (exc == null) {
                    Files.delete(dir);
                    return CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }

    /**
     *
     * @param path
     *            The directory path.
     * @param size
     *            The returned size in bytes.
     * @throws IOException
     *             When IO error occurs.
     */
    public static void calcDirSize(final Path path, final MutableLong size)
            throws IOException {

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {

                if (attrs.isDirectory()) {
                    calcDirSize(file, size); // recurse
                } else {
                    size.add(attrs.size());
                }
                return CONTINUE;
            }
        });
    }

    /**
     * Replaces original file with a new version.
     *
     * @param originalFile
     *            The target file.
     * @param newFile
     *            The source file that is <i>moved</i> to the target file.
     * @throws IOException
     *             When IO error.
     */
    public static void replaceWithNewVersion(final File originalFile,
            final File newFile) throws IOException {

        final Path source =
                FileSystems.getDefault().getPath(newFile.getAbsolutePath());

        final Path target = FileSystems.getDefault()
                .getPath(originalFile.getAbsolutePath());

        Files.move(source, target,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Applies permissions to file in path.
     *
     * @param path
     * @param perms
     * @throws IOException
     */
    public static void applyPosixFilePermissions(final Path path,
            final FilePermissionEnum perms) throws IOException {
        Files.setPosixFilePermissions(path, perms.getPosixFilePermissions());
    }

    /**
     * Creates a directory if it does not exist.
     *
     * @param path
     * @param permissions
     * @throws IOException
     */
    public static void createDirectory(final Path path,
            final FilePermissionEnum permissions) throws IOException {
        createDirectory(path, permissions, false);
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     *
     * @param path
     * @param permissions
     * @throws IOException
     */
    public static void createDirectoryPath(final Path path,
            final FilePermissionEnum permissions) throws IOException {
        createDirectory(path, permissions, true);
    }

    /**
     * Creates a directory if it does not exist.
     *
     * @param path
     * @param permissions
     * @param createParents
     *            If {@code true}, all nonexistent parent directories are
     *            created first
     * @throws IOException
     */
    private static void createDirectory(final Path path,
            final FilePermissionEnum permissions, final boolean createParents)
            throws IOException {

        if (permissions == null) {
            throw new RuntimeException("permissions missing.");
        }

        final FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions
                        .asFileAttribute(permissions.getPosixFilePermissions());
        if (createParents) {
            Files.createDirectories(path, attr);
        } else {
            Files.createDirectory(path, attr);
        }
    }

}
