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
package org.printflow.lite.core.jmx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IServerDataFile;
import org.printflow.lite.core.config.ServerDataFileNameEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JmxRemoteProperties implements IServerDataFile {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JmxRemoteProperties.class);

    private static final String KEY_ADMIN = "admin";

    private static final String BASE_KEY = "com.sun.management.jmxremote";

    private static final String KEY_PORT = BASE_KEY + ".port";

    /** */
    private static Properties theProps;

    /**
     *
     * @return
     */
    public static String getAdminUsername() {
        return KEY_ADMIN;
    }

    /**
     *
     * @return
     */
    public static String getPort() {
        return getProperty(KEY_PORT);
    }

    synchronized private static String getProperty(String key) {
        if (theProps == null) {
            read();
        }
        return theProps.getProperty(key);
    }

    /**
     * @param password
     */
    public static void setAdminPassword(final String password) {

        final File fileProp = ServerDataFileNameEnum.JMXREMOTE_PASSWORD
                .getPathAbsolute(ConfigManager.getServerHomePath()).toFile();

        Properties props = new Properties();

        InputStream istr = null;
        Writer writer = null;

        try {
            if (fileProp.exists()) {
                istr = new java.io.FileInputStream(fileProp);
                props.load(istr);
                istr.close();
                istr = null;
            }

            props.put(KEY_ADMIN, password);

            writer = new FileWriter(fileProp);
            props.store(writer, getPasswordFileComments());

            writer.close();
            writer = null;

        } catch (IOException e) {

            throw new SpException(e);

        } finally {

            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }

            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    /**
     * @return comment header text for the stored
     *         {@link ServerDataFileNameEnum#JMXREMOTE_PASSWORD}.
     */
    private static String getPasswordFileComments() {

        final String line = "---------------------------"
                + "-------------------------------";

        return line + "\n " + CommunityDictEnum.PrintFlowLite.getWord()
                + " JMX Agent"
                + "\n Keep the content of this file at a secure place.\n"
                + line;
    }

    /**
     * @return
     */
    private static void read() {

        theProps = new Properties();

        final File fileProp = ServerDataFileNameEnum.JMXREMOTE_PROPERTIES
                .getPathAbsolute(ConfigManager.getServerHomePath()).toFile();

        InputStream istr = null;

        try {
            if (fileProp.exists()) {
                istr = new java.io.FileInputStream(fileProp);
                theProps.load(istr);
            }

        } catch (IOException e) {

            throw new SpException(e);

        } finally {
            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

}
