#
# This file is part of the PrintFlowLite project <https://www.printflowlite.org>.
# Copyright (c) 2011-2020 Datraverse B.V.
# Author: Rijk Ravestein.
#
# SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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

#-------------------------------------------------------------------------
# Prerequisite for make:
# 	$ sudo apt-get install libcups2-dev
# 	(Just for the header file(s), libcups.a gives unresolved externals)
#-------------------------------------------------------------------------
# Prerequisite for make:
# 	https://gitlab.com/printflowlite/xmlrpcpp
#-------------------------------------------------------------------------

#-------------------------------------------------------------------------
# 'make' command line parameters:
# 	PRODUCT_VERSION=x.x.x
# 	CUPS_1_4 (optional)  
#-------------------------------------------------------------------------

LIBS_CLIENT := -l cups
LIBS_CLIENT += ../xmlrpcpp/libXmlRpc.a 
LIBS_CLIENT += -Wl,-Bsymbolic-functions

#
CFLAGS := -I ../xmlrpcpp/src

ifdef CUPS_1_4
CFLAGS += -DCUPS_1_4=$(CUPS_1_4) 
endif

ifdef PRODUCT_VERSION 
CFLAGS += -DPRODUCT_VERSION=\"$(PRODUCT_VERSION)\"
endif

#
# Static link of C and C++ libs. 
#
LDFLAGS := -static-libstdc++ -static-libgcc

CUPS_NOTIFIER := printflowlite-notifier
CUPS_NOTIFIER_EXE := target/$(CUPS_NOTIFIER)
CUPS_NOTIFIER_OBJ := target/$(CUPS_NOTIFIER).o

#$(CC)

.PHONY: all
all: init $(CUPS_NOTIFIER_EXE)

.PHONY: init
init:
	@mkdir -p target

#
$(CUPS_NOTIFIER_EXE) : $(CUPS_NOTIFIER_OBJ)
	g++ $^ $(LIBS_CLIENT) $(LDFLAGS) -o $@ 
	strip $@

$(CUPS_NOTIFIER_OBJ) : $(CUPS_NOTIFIER).cc
	g++ -c $(INCLUDES) $(CFLAGS) -o $@ $< 
	 
#
.PHONY: clean
clean:
	@rm -f $(CUPS_NOTIFIER_EXE) $(CUPS_NOTIFIER_OBJ)	

# end-of-file
