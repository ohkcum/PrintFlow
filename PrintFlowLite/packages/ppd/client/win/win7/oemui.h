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
// Define common data types, and external function prototypes
// for OEMUI Test Module.
// --------------------------------------------------------------------

#pragma once

////////////////////////////////////////////////////////
//      OEM UI Defines
////////////////////////////////////////////////////////

// OEM Signature and version.
#define PROP_TITLE      L"OEM UI Page"

// Printer registry keys where OEM data is stored.
#define OEMUI_VALUE             TEXT("OEMUI_VALUE")
#define OEMUI_DEVICE_VALUE      TEXT("OEMUI_DEVICE_VALUE")



////////////////////////////////////////////////////////
//      Prototypes
////////////////////////////////////////////////////////

HRESULT hrOEMPropertyPage(DWORD dwMode, POEMCUIPPARAM pOEMUIParam);
HRESULT hrOEMDocumentPropertySheets(PPROPSHEETUI_INFO pPSUIInfo, LPARAM lParam, IPrintOemDriverUI*  pOEMHelp);
HRESULT hrOEMDevicePropertySheets(PPROPSHEETUI_INFO pPSUIInfo, LPARAM lParam);

