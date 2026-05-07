/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2026 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2026 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.template.dto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateFinancialTrxDto implements TemplateDto {

    /** */
    private TemplateAmountTotalDto deposit;
    /** */
    private TemplateAmountTotalDto external;
    /** */
    private TemplateAmountTotalDto purchase;

    public TemplateAmountTotalDto getDeposit() {
        return deposit;
    }

    public void setDeposit(TemplateAmountTotalDto deposit) {
        this.deposit = deposit;
    }

    public TemplateAmountTotalDto getExternal() {
        return external;
    }

    public void setExternal(TemplateAmountTotalDto external) {
        this.external = external;
    }

    public TemplateAmountTotalDto getPurchase() {
        return purchase;
    }

    public void setPurchase(TemplateAmountTotalDto purchase) {
        this.purchase = purchase;
    }

}
