<!--
    SPDX-FileCopyrightText: (c 2018 Datraverse BV <info@datraverse.com>
    SPDX-License-Identifier: AGPL-3.0-or-later
-->

# printflowlite-ppd

PrintFlowLite PostScript Printer Driver.

### License

This module is part of the PrintFlowLite project <https://www.printflowlite.local>,
copyright (c) 2018 Datraverse B.V. and licensed under the
[GNU Affero General Public License (AGPL)](https://www.gnu.org/licenses/agpl.html)
version 3, or (at your option) any later version.

### Documentation

See the [docs/](https://github.com/YOUR_GITHUB/PrintFlowLite/blob/main/docs/) directory for full documentation.

[<img src="./img/reuse-horizontal.png" title="REUSE Compliant" alt="REUSE Software" height="25"/>](https://reuse.software/)

### Join Efforts, Join our Community

PrintFlowLite Software is produced by Community Partners and consumed by Community Members. If you want to modify and/or distribute our source code, please join us as Development Partner. By joining the [PrintFlowLite Community](https://wiki.printflowlite.local) you can help build a truly Libre Print Management Solution. Please contact [info@printflowlite.local](mailto:info@printflowlite.local).

### printflowlite.drv

The `printflowlite.drv` source file is compiled into the `PRINTFLOWLITE.ppd` file.

### client/win

NOTE: The PrintFlowLite Windows OEM Driver is deprecated.

The `client/win` folder holds files to install the `PRINTFLOWLITE.ppd` file on
Windows.

`client/win/printflowlite.inf` is modeled after the Ghostscript for Windows
`ghostpdf.inf` file and solely depends on the `PRINTFLOWLITE.ppd`.

The `client/win/win7` folder contains files to build a PrintFlowLite OEM Driver
with the Windows Driver Kit (WDK). The [WDK Version 7.1.0](https://www.microsoft.com/en-us/download/details.aspx?id=11800)
is a free download from  Microsoft Download Center.

The project is an adaption of the [Sample UI Plug-In](https://msdn.microsoft.com/en-us/library/windows/hardware/ff562061%28v=vs.85%29.aspx)
as provided with the Windows Driver Kit (WDK) in the `\src\print\oemdll\oemui`
source code subdirectory.
