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
package org.printflow.lite.lib.pgp.pdf;

import java.util.List;

import org.printflow.lite.core.json.PdfProperties;
import org.printflow.lite.lib.pgp.PGPPublicKeyInfo;
import org.printflow.lite.lib.pgp.PGPSecretKeyInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PdfPgpSignParms {

    /**
     * @return The secret key to sign with.
     */
    PGPSecretKeyInfo getSecretKeyInfo();

    /**
     * @return Public key of the author ({@code null} when not available.
     */
    PGPPublicKeyInfo getPubKeyAuthor();

    /**
     * @return The public keys to encrypt with.
     */
    List<PGPPublicKeyInfo> getPubKeyInfoList();

    /**
     * @return The verification URL builder.
     */
    PdfPgpVerifyUrl getUrlBuilder();

    /**
     * @return PDF properties used for encryption. If {@code null}, encryption
     *         is not applicable.
     */
    PdfProperties getEncryptionProps();

    /**
     * @return {@code true}, if signature if embedded just before %%EOF.
     *         {@code false} if signature if appended just after %%EOF.
     */
    boolean isEmbeddedSignature();
}
