/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
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
package org.printflow.lite.core.ipp.attribute.syntax;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.ipp.encoding.IppEncoder;
import org.printflow.lite.core.ipp.encoding.IppValueTag;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppOutOfBand extends AbstractIppAttrSyntax {

    private IppValueTag valueTag;

    @SuppressWarnings("unused")
    private IppOutOfBand() {
        // no code intended
    }

    /**
     *
     * @param valueTag
     */
    public IppOutOfBand(final IppValueTag valueTag) {
        switch (valueTag) {
        case UNKNOWN:
        case UNSUPPORTED:
        case NONE:
            this.valueTag = valueTag;
            break;
        default:
            throw new SpException(
                    valueTag.toString() + " is no valid out-of-band value");
        }

    }

    @Override
    public IppValueTag getValueTag() {
        return valueTag;
    }

    @Override
    public void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        /*
         * For "out-of-band" "value-tag" fields defined in this document, such
         * as "unsupported", the "value-length" MUST be 0 and the "value" empty;
         *
         * the "value" has no meaning when the "value-tag" has one of these
         * "out-of-band" values.
         *
         * For future "out-of-band" "value-tag" fields, the same rule holds
         * unless the definition explicitly states that the "value-length" MAY
         * be non-zero and the "value" non-empty.
         */
        IppEncoder.writeInt16(ostr, 0);
    }

}
