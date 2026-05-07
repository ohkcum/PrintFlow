/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.dao.impl;

import java.util.List;

import javax.persistence.Query;

import org.printflow.lite.core.dao.IAttrDao;
import org.printflow.lite.core.dao.PrinterGroupMemberDao;
import org.printflow.lite.core.dao.enums.PrinterAttrEnum;
import org.printflow.lite.core.dao.helpers.ProxyPrinterName;
import org.printflow.lite.core.jpa.PrinterGroup;
import org.printflow.lite.core.jpa.PrinterGroupMember;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterGroupMemberDaoImpl extends
        GenericDaoImpl<PrinterGroupMember> implements PrinterGroupMemberDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM PrinterGroupMember T";
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PrinterGroup> getGroupsWithJobTicketMembers() {

        final String jpql = "SELECT G FROM PrinterGroup G WHERE G.id IN "
                + "(SELECT DISTINCT(M.group.id) FROM PrinterGroupMember M "
                + "JOIN PrinterAttr A ON (A.printer = M.printer "
                + "AND A.name = :attrName AND A.value = :attrValue))";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("attrName",
                PrinterAttrEnum.JOBTICKET_ENABLE.getDbName());
        query.setParameter("attrValue", IAttrDao.V_YES);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PrinterGroup> getGroups(final String printerName) {

        final String jpql = "SELECT M.group FROM PrinterGroupMember M "
                + "WHERE M.printer.printerName = :name";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("name", ProxyPrinterName.getDaoName(printerName));
        return query.getResultList();
    }

}
