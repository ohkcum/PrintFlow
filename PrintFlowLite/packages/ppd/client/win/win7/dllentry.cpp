//
// This file is part of the PrintFlowLite project <https://www.printflowlite.local>.
// Copyright (c) 2016 Datraverse B.V.
// Author: Rijk Ravestein.
//
// SPDX-FileCopyrightText: © 2016 Datraverse BV <info@datraverse.com>
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//
// For more information, please contact Datraverse B.V. at this
// address: info@datraverse.com
//

// --------------------------------------------------------------------
// Source module for DLL entry function(s).
// --------------------------------------------------------------------

#include "precomp.h"
#include "debug.h"
#include "globals.h"

// This indicates to Prefast that this is a usermode driver file.
__user_driver;

///////////////////////////////////////////////////////////
//
// DLL entry point
//
BOOL WINAPI DllMain(HINSTANCE hInst, WORD wReason, LPVOID lpReserved)
{
    UNREFERENCED_PARAMETER(lpReserved);

    switch(wReason)
    {
        case DLL_PROCESS_ATTACH:
            VERBOSE("Process attach.\r\n");
            ghInstance = hInst;
            break;

        case DLL_THREAD_ATTACH:
            VERBOSE("Thread attach.\r\n");
            break;

        case DLL_PROCESS_DETACH:
            VERBOSE("Process detach.\r\n");
            break;

        case DLL_THREAD_DETACH:
            VERBOSE("Thread detach.\r\n");
            break;
    }

    return TRUE;
}
