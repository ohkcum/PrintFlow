/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.printflow.lite.core.dao.DocLogDao;
import org.printflow.lite.core.dao.helpers.DocLogPagerReq;
import org.printflow.lite.core.dao.impl.DaoContextImpl;
import org.printflow.lite.core.dto.AbstractDto;
import org.printflow.lite.core.dto.QuickSearchFilterMailTicketDto;
import org.printflow.lite.core.dto.QuickSearchItemDto;
import org.printflow.lite.core.dto.QuickSearchMailTicketItemDto;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.jpa.User;
import org.printflow.lite.core.services.helpers.MailPrintData;
import org.printflow.lite.server.pages.DocLogItem;

/**
 * Mail Ticket Quick Search.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqMailTicketQuickSearch extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private List<QuickSearchItemDto> items;

        @SuppressWarnings("unused")
        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final QuickSearchFilterMailTicketDto dto = AbstractDto.create(
                QuickSearchFilterMailTicketDto.class, this.getParmValueDto());

        //
        final DocLogPagerReq req = new DocLogPagerReq();
        req.setMaxResults(dto.getMaxResults());
        req.setTicketNumberMailView(Boolean.TRUE);

        final DocLogPagerReq.Sort sort = new DocLogPagerReq.Sort();
        sort.setField(DocLogPagerReq.Sort.FLD_NAME);
        req.setSort(sort);

        final DocLogPagerReq.Select select = new DocLogPagerReq.Select();
        select.setDocType(DocLogDao.Type.IN);
        select.setTicketNumberMail(dto.getFilter());

        req.setSelect(select);

        final EntityManager em = DaoContextImpl.peekEntityManager();
        final DocLogItem.AbstractQuery query =
                DocLogItem.createQuery(DocLogDao.Type.IN);

        //
        final List<QuickSearchItemDto> resultList = new ArrayList<>();
        int nPage = 0;

        while (true) {

            nPage++;
            req.setPage(nPage);

            final List<DocLogItem> itemListWlk =
                    query.getListChunk(em, dto.getUserId(), req, getLocale());

            for (final DocLogItem log : itemListWlk) {

                final boolean hasDocStore =
                        log.isPrintArchive() || log.isPrintJournal();

                if (dto.isDocStore() && !hasDocStore) {
                    continue;
                }

                if (log.getExtData() == null) { // just in case
                    continue;
                }

                final QuickSearchMailTicketItemDto item =
                        new QuickSearchMailTicketItemDto();

                item.setKey(log.getDocLogId());
                item.setText(log.getExtId());
                item.setEmail(MailPrintData.createFromData(log.getExtData())
                        .getFromAddress());
                item.setDocStore(hasDocStore);

                if (log.getPrintOutOfDocIn() != null
                        && log.getPrintOutOfDocIn().size() > 0) {
                    item.setPrintOutJobs(log.getPrintOutOfDocIn().size());
                }
                item.setPaperSize(log.getPaperSize());
                item.setTitle(log.getTitle());
                item.setPages(String.format("%d %s", log.getTotalPages(),
                        NounEnum.PAGE.uiText(getLocale(),
                                log.getTotalPages() > 1)));
                item.setByteCount(log.getHumanReadableByteCount());

                resultList.add(item);

                if (resultList.size() == dto.getMaxResults()) {
                    break;
                }
            }

            if (resultList.size() == dto.getMaxResults()
                    || itemListWlk.size() < dto.getMaxResults()) {
                break;
            }
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(resultList);

        setResponse(rsp);
        setApiResultOk();
    }

}
