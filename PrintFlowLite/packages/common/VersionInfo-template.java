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

/*
 * @DISCLAIMER@
 * @GENERATE_INFO@
 *
 * This file is a result of an Ant filter operation. The @@ tokens in the
 * template were replaced by property values defined in the Ant build script
 * (see build.xml).
 *
 * See http://www.techrepublic.com/article/ant-makes-branding-java-jars-simple-and-foolproof/5085496
 *
 */

package @PACKAGE@;

import java.util.Date;

import org.printflow.lite.common.IUtility;
import org.printflow.lite.common.SystemPropertyEnum;

/**
 * @NAME@ @VERSION_MAJOR@.@VERSION_MINOR@.@VERSION_REVISION@@VERSION_STATUS@.@VERSION_BUILD@
 * - @BUILD_DATE@.
 *
 * @author Rijk Ravestein
 *
 */
public final class VersionInfo implements IUtility {

    /**
     * Utility class.
     */
    private VersionInfo() {
    }

    public static final String LICENSE_NAME = "@LICENSE_NAME@";
    public static final String LICENSE_URL = "@LICENSE_URL@";

    public static final String PRODUCT = "@PRODUCT@";
    public static final String MODULE = "@MODULE@";
    public static final String NAME = "@NAME@";
    public static final String BUILD_DATE = "@BUILD_DATE@";
    public static final String VERSION_A_MAJOR = "@VERSION_MAJOR@";
    public static final String VERSION_B_MINOR = "@VERSION_MINOR@";
    public static final String VERSION_C_REVISION = "@VERSION_REVISION@";
    public static final String VERSION_D_STATUS = "@VERSION_STATUS@";
    public static final String VERSION_E_BUILD = "@VERSION_BUILD@";
    public static final String DB_SCHEMA_VERSION_MAJOR =
            "@DB_SCHEMA_VERSION_MAJOR@";
    public static final String DB_SCHEMA_VERSION_MINOR =
            "@DB_SCHEMA_VERSION_MINOR@";
    public static final String DIR_LAYOUT_VERSION_MAJOR =
            "@DIR_LAYOUT_VERSION_MAJOR@";
    public static final String DIR_LAYOUT_VERSION_MINOR =
            "@DIR_LAYOUT_VERSION_MINOR@";
    public static final long BUILD_EPOCH_SECS = @BUILD_EPOCH_SECS@;

    /**
     * @return The full concatenated version string.
     */
    public static String getVersionString() {
        return NAME + " "
                + (("@VERSION_" + "MAJOR@").equals(VERSION_A_MAJOR) ? ""
                        : (VERSION_A_MAJOR))
                + "."
                + (("@VERSION_" + "MINOR@").equals(VERSION_B_MINOR) ? "0"
                        : (VERSION_B_MINOR))
                + "."
                + (("@VERSION_" + "REVISION@").equals(VERSION_C_REVISION) ? "0"
                        : (VERSION_C_REVISION))
                + (("@VERSION_" + "STATUS@").equals(VERSION_D_STATUS) ? "0"
                        : (VERSION_D_STATUS))
                + "."
                + (("@VERSION_" + "BUILD@").equals(VERSION_E_BUILD) ? "0"
                        : (VERSION_E_BUILD))
                + (("@BUILD_" + "DATE@").equals(BUILD_DATE) ? ""
                        : " [" + (BUILD_DATE) + "]")
                + " " + SystemPropertyEnum.OS_ARCH.getValue() + " "
                + SystemPropertyEnum.OS_NAME.getValue();
    }

    /**
     * Gets the date of the build.
     *
     * @return Date object.
     */
    public static Date getBuildDate() {
        final long epoch = BUILD_EPOCH_SECS * 1000L;
        return new Date(epoch);
    }

}
