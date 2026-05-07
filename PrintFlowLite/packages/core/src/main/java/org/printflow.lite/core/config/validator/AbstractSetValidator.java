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
package org.printflow.lite.core.config.validator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractSetValidator implements ConfigPropValidator {

    /**
     * Characters used as the delimiters between string parts.
     */
    protected static final String SEPARATOR_CHARS = " ,;:";

    /**
     * {@code true} if value is optional.
     */
    private final boolean isOptional;

    /**
     * @param optional
     *            {@code true} if value is optional.
     */
    public AbstractSetValidator(final boolean optional) {
        super();
        this.isOptional = optional;
    }

    @Override
    public final ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        final Set<String> set = new HashSet<>();

        boolean isValid = true;

        if (StringUtils.isNotBlank(value)) {

            for (final String item : StringUtils.split(value,
                    SEPARATOR_CHARS)) {
                if (set.contains(item) || !this.onItem(item)) {
                    isValid = false;
                    break;
                }
                set.add(item);
            }
        } else if (!this.isOptional) {
            isValid = false;
        }

        if (!isValid) {
            res.setStatus(ValidationStatusEnum.ERROR_ENUM);
            res.setMessage(String.format("Invalid value [%s].", value));
        }

        return res;
    }

    /**
     * Notifies item in the Set.
     *
     * @param item
     *            The item to validate.
     * @return {@code true} when item is valid.
     */
    protected abstract boolean onItem(String item);

    /**
     * @param value
     *            String representation
     * @return set of strings.
     */
    public static Set<String> getSet(final String value) {
        if (value == null) {
            return new HashSet<>();
        }
        return Stream.of(StringUtils.split(value, SEPARATOR_CHARS))
                .collect(Collectors.toSet());
    }

}
