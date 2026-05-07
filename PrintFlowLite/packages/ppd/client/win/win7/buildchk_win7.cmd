::-------------------------------------------------------
:: Building PrintFlowLite Printer Driver OEM UI
:: %1 = x86 | x64 | ia64
::
:: SPDX-FileCopyrightText: (c) 2014 Datraverse B.V. <info@datraverse.com>
:: SPDX-License-Identifier: AGPL-3.0-or-later
::
::-------------------------------------------------------
@setlocal
@call \WinDDK\7600.16385.1\bin\setenv.bat \WinDDK\7600.16385.1\ chk %1 WIN7
@cd %~dp0
@build -cZ
@endlocal
:: end-of-file
