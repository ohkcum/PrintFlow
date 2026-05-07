#!/bin/bash
#
# This file is part of the PrintFlowLite project <https://printflowlite.local>.
# Copyright (c) 2024 Datraverse B.V.
# Author: Rijk Ravestein.
#
# SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
# For more information, please contact Datraverse B.V. at this
# address: info@datraverse.com
#

#----------------------------------------------------------------------------
# Echoes the Application and Database version.  
# This is the single place to specify these versions.
#----------------------------------------------------------------------------
VERSION_MAJOR=1
VERSION_MINOR=7
VERSION_REVISION=0
VERSION_STATUS=-rc

DB_SCHEMA_VERSION_MAJOR=1
DB_SCHEMA_VERSION_MINOR=10

DIR_LAYOUT_VERSION_MAJOR=1
DIR_LAYOUT_VERSION_MINOR=0

if [ "$1" == "major" ]; then
    echo ${VERSION_MAJOR}
    exit;
fi

if [ "$1" == "minor" ]; then
    echo ${VERSION_MINOR}
    exit;
fi

if [ "$1" == "revision" ]; then
    echo ${VERSION_REVISION}
    exit;
fi

if [ "$1" == "status" ]; then
    echo ${VERSION_STATUS}
    exit;
fi

if [ "$1" == "db-schema" ]; then
    echo ${DB_SCHEMA_VERSION_MAJOR}.${DB_SCHEMA_VERSION_MINOR}
    exit;
fi

if [ "$1" == "db-schema-major" ]; then
    echo ${DB_SCHEMA_VERSION_MAJOR}
    exit;
fi

if [ "$1" == "db-schema-minor" ]; then
    echo ${DB_SCHEMA_VERSION_MINOR}
    exit;
fi

if [ "$1" == "dir-layout-major" ]; then
    echo ${DIR_LAYOUT_VERSION_MAJOR}
    exit;
fi

if [ "$1" == "dir-layout-minor" ]; then
    echo ${DIR_LAYOUT_VERSION_MINOR}
    exit;
fi

echo ${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_REVISION}

#end-of-file
