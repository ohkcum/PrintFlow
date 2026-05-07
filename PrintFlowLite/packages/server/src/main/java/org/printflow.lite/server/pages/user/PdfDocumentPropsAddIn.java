/*
 * This file is part of the PrintFlowLite project <https://www.PrintFlowLite.org>.
 * Copyright (c) 2024 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.server.pages.user;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.printflow.lite.core.i18n.NounEnum;
import org.printflow.lite.core.inbox.InboxInfoDto;
import org.printflow.lite.core.inbox.InboxInfoDto.InboxJob;
import org.printflow.lite.core.pdf.PdfInfoDto;
import org.printflow.lite.core.services.DocLogService;
import org.printflow.lite.core.services.InboxService;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.server.pages.AbstractAuthPage;
import org.printflow.lite.server.pages.MarkupHelper;
import org.printflow.lite.server.session.SpSession;

/**
 * Note: Loaded from JavaScript.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfDocumentPropsAddIn extends AbstractAuthPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final DocLogService DOCLOG_SERVICE =
            ServiceContext.getServiceFactory().getDocLogService();
    /** */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /** */
    private static final String WID_PROPS_HEADER = "props-header";

    /**
     * @param parameters
     */
    public PdfDocumentPropsAddIn(final PageParameters parameters) {

        super(parameters);

        final InboxInfoDto jobs =
                INBOX_SERVICE.readInboxInfo(SpSession.get().getUserId());

        final int iJob = Integer.parseInt(this.getParmValue("ijob"));
        final InboxJob job = jobs.getJobs().get(iJob);
        final String uuid = FilenameUtils.getBaseName(job.getFile());

        final PdfInfoDto dto = DOCLOG_SERVICE.getHttpPrintInPdfInfo(
                SpSession.get().getUserDbKey(), UUID.fromString(uuid));

        final MarkupHelper helper = new MarkupHelper(this);

        if (dto == null) {
            helper.discloseLabel(WID_PROPS_HEADER);
            return;
        }

        helper.addLabel(WID_PROPS_HEADER, NounEnum.ORIGINAL);

        final Map<String, String> mapProps = dto.createPropertyMap(getLocale());

        this.add(new PropertyListView<String>("prop-entry",
                new ArrayList<String>(mapProps.keySet())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String key = item.getModelObject();
                item.add(new Label("prop-name", key));
                item.add(new Label("prop-value", mapProps.get(key)));
            }
        });
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
