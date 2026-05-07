/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
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
package org.printflow.lite.client;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;

import org.printflow.lite.common.SystemPropertyEnum;

/**
 * Class to enforce a single application runtime instance.
 * <p>
 * A flag file with a lock mechanism is used. The idea is to create and lock a
 * file in the {@code java.io.tmpdir} folder with a provided name. A concurrent
 * execution will try to lock the same file and will fail. A special "shutdown
 * hook" is provided to unlock the file when the JVM is shutting down.
 * </p>
 * <p>
 * Code adapted from
 * <a href= "http://www.rgagnon.com/javadetails/java-0288.html" >JustOneLock.
 * java</a> class.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ClientAppLock {

    /**
     *
     */
    private static final String LOCK_FILE_EXT = ".lock";

    /**
     *
     */
    private final String applicationId;

    /**
     *
     */
    private File lockFile;

    /**
     *
     */
    private FileChannel lockFileChannel;

    /**
     *
     */
    private FileLock lockFileLock;

    /**
     *
     * @return
     */
    public static String getLockDir() {
        return SystemPropertyEnum.JAVA_IO_TMPDIR.getValue();
    }

    /**
     *
     * @param applicationId
     *            The unique application ID.
     */
    public ClientAppLock(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Checks if the application is already active. If not, a lock is set and
     * {@code false} is returned.
     *
     * @return {@code true} is application is already active, {@code false} if
     *         not.
     */
    public boolean isAppActive() {

        this.lockFile = FileSystems.getDefault()
                .getPath(getLockDir(), applicationId + LOCK_FILE_EXT).toFile();

        /*
         * Get the lock file and its channel.
         */
        try {
            /*
             * For the lock to work keep RandomAccessFile open, do NOT close().
             */
            @SuppressWarnings("resource")
            final RandomAccessFile randomAccessFile =
                    new RandomAccessFile(lockFile, "rw");
            this.lockFileChannel = randomAccessFile.getChannel();
        } catch (FileNotFoundException e) {
            throw new ClientAppException(e.getMessage());
        }

        /*
         * Try to lock the lock file.
         */
        try {

            this.lockFileLock = this.lockFileChannel.tryLock();

        } catch (IOException e) {

            releaseLockResources();

            throw new ClientAppException(e.getMessage());

        } catch (OverlappingFileLockException e) {
            /*
             * Already locked!
             */
            releaseLockResources();
            return true;
        }

        if (this.lockFileLock == null) {
            releaseLockResources();
            return true;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            // destroy the lock when the JVM is closing
            @Override
            public void run() {
                releaseLockResources();
                deleteLockFile();
            }
        });

        return false;
    }

    /**
     *
     */
    private void releaseLockResources() {

        if (this.lockFileLock != null) {
            try {
                this.lockFileLock.release();
            } catch (Exception e) {
                // silently
            }
        }
        closeQuietly(this.lockFileChannel);
    }

    /**
     *
     * @param closeable
     *            A closeable.
     */
    private static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }

    /**
     *
     */
    private void deleteLockFile() {

        if (this.lockFile != null && this.lockFile.exists()) {
            try {
                this.lockFile.delete();
            } catch (Exception e) {
                // silently
            }
        }
    }

}
