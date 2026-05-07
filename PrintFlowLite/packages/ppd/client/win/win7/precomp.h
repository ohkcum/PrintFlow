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
// Optimize compilation of headers that don't change frequently.
// --------------------------------------------------------------------

#pragma once

// Necessary for compiling under VC.
#if(!defined(WINVER) || (WINVER < 0x0500))
	#undef WINVER
	#define WINVER          0x0500
#endif
#if(!defined(_WIN32_WINNT) || (_WIN32_WINNT < 0x0500))
	#undef _WIN32_WINNT
	#define _WIN32_WINNT    0x0500
#endif


// Required header files that shouldn't change often.


#include <STDDEF.H>
#include <STDLIB.H>
#include <OBJBASE.H>
#include <STDARG.H>
#include <STDIO.H>
#include <WINDEF.H>
#include <WINERROR.H>
#include <WINBASE.H>
#include <WINGDI.H>
#include <WINDDI.H>
#include <WINSPOOL.H>
#include <TCHAR.H>
#include <EXCPT.H>
#include <ASSERT.H>
#include <PRSHT.H>
#include <COMPSTUI.H>
#include <WINDDIUI.H>
#include <PRINTOEM.H>
#include <INITGUID.H>
#include <PRCOMOEM.H>

#include <SHELLAPI.H> // PRINTFLOWLITE

// StrSafe.h needs to be included last
// to disallow unsafe string functions.
#include <STRSAFE.H>

