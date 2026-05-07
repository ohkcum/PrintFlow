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
// Header of interface for PScript5 & Unidrv UI plug-ins.
// --------------------------------------------------------------------

#pragma once

////////////////////////////////////////////////////////////////////////////////
//
// IOemUI
//
class IOemUI: public IPrintOemUI
{
public:
    // *** IUnknown methods ***

    STDMETHOD(QueryInterface) (THIS_ REFIID riid, LPVOID FAR* ppvObj);

    STDMETHOD_(ULONG,AddRef)  (THIS);

    // the __drv_at tag here tells prefast that once release 
    // is called, the memory should not be considered leaked
    __drv_at(this, __drv_freesMem(object)) STDMETHOD_(ULONG,Release)(THIS);

    //
    // Method for publishing Driver interface.
    //
    STDMETHOD(PublishDriverInterface)(THIS_ IUnknown *pIUnknown);

    //
    // Get OEM dll related information
    //

    STDMETHOD(GetInfo) (THIS_ DWORD  dwMode, PVOID  pBuffer, DWORD  cbSize,
                           PDWORD pcbNeeded);

    //
    // OEMDevMode
    //

    STDMETHOD(DevMode) (THIS_  DWORD  dwMode, POEMDMPARAM pOemDMParam) ;

    //
    // OEMCommonUIProp
    //

    STDMETHOD(CommonUIProp) (THIS_
            DWORD  dwMode,
            POEMCUIPPARAM   pOemCUIPParam
            );

    //
    // OEMDocumentPropertySheets
    //

    STDMETHOD(DocumentPropertySheets) (THIS_
            PPROPSHEETUI_INFO   pPSUIInfo,
            LPARAM              lParam
            );

    //
    // OEMDevicePropertySheets
    //

    STDMETHOD(DevicePropertySheets) (THIS_
            PPROPSHEETUI_INFO   pPSUIInfo,
            LPARAM              lParam
            );


    //
    // OEMDevQueryPrintEx
    //

    STDMETHOD(DevQueryPrintEx) (THIS_
            POEMUIOBJ               poemuiobj,
            PDEVQUERYPRINT_INFO     pDQPInfo,
            PDEVMODE                pPublicDM,
            PVOID                   pOEMDM
            );

    //
    // OEMDeviceCapabilities
    //

    STDMETHOD(DeviceCapabilities) (THIS_
            POEMUIOBJ   poemuiobj,
            HANDLE      hPrinter,
            __in PWSTR  pDeviceName,
            WORD        wCapability,
            PVOID       pOutput,
            PDEVMODE    pPublicDM,
            PVOID       pOEMDM,
            DWORD       dwOld,
            DWORD       *dwResult
            );

    //
    // OEMUpgradePrinter
    //

    STDMETHOD(UpgradePrinter) (THIS_
            DWORD   dwLevel,
            PBYTE   pDriverUpgradeInfo
            );

    //
    // OEMPrinterEvent
    //

    STDMETHOD(PrinterEvent) (THIS_
            __in PWSTR   pPrinterName,
            INT          iDriverEvent,
            DWORD        dwFlags,
            LPARAM       lParam
            );

    //
    // OEMDriverEvent
    //

    STDMETHOD(DriverEvent)(THIS_
            DWORD   dwDriverEvent,
            DWORD   dwLevel,
            LPBYTE  pDriverInfo,
            LPARAM  lParam
            );

    //
    // OEMQueryColorProfile
    //

    STDMETHOD(QueryColorProfile) (THIS_
            HANDLE      hPrinter,
            POEMUIOBJ   poemuiobj,
            PDEVMODE    pPublicDM,
            PVOID       pOEMDM,
            ULONG       ulReserved,
            VOID       *pvProfileData,
            ULONG      *pcbProfileData,
            FLONG      *pflProfileData);

    //
    // OEMFontInstallerDlgProc
    //

    STDMETHOD(FontInstallerDlgProc) (THIS_
            HWND    hWnd,
            UINT    usMsg,
            WPARAM  wParam,
            LPARAM  lParam
            );
    //
    // UpdateExternalFonts
    //

    STDMETHOD(UpdateExternalFonts) (THIS_
            HANDLE       hPrinter,
            HANDLE       hHeap,
            __in PWSTR   pwstrCartridges
            );


    IOemUI() { m_cRef = 1; m_pOEMHelp = NULL; };
    ~IOemUI();

protected:
    LONG                m_cRef;
    IPrintOemDriverUI*  m_pOEMHelp;
};

