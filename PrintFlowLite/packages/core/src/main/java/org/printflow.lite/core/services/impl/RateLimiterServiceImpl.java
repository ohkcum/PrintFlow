/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.services.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.ratelimiter.IRateLimiter;
import org.printflow.lite.core.ratelimiter.TokenBucketRateLimiter;
import org.printflow.lite.core.services.RateLimiterService;
import org.printflow.lite.core.services.helpers.IRateLimiterListener;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RateLimiterServiceImpl extends AbstractService
        implements RateLimiterService {

    /**
     * Rate limiters by ID by {@link LimiterApp}.
     */
    private Map<LimitEnum, ConcurrentHashMap<String, IRateLimiter>> //
    limitApps = new HashMap<LimitEnum, //
            ConcurrentHashMap<String, IRateLimiter>>();

    /**
     * Creates an HashMap of Rate limiters by ID.
     *
     * @return HashMap.
     */
    private ConcurrentHashMap<String, IRateLimiter> createRateLimiterMap() {
        return new ConcurrentHashMap<String, IRateLimiter>();
    }

    /**
     * @param limitEnum
     * @return instance
     */
    private IRateLimiter createRateLimiter(final LimitEnum limitEnum) {

        switch (limitEnum) {
        case USER_AUTH_FAILURE_BY_IP:
            return TokenBucketRateLimiter.createMaxTokensPerMinute(
                    ConfigManager.instance().getConfigLong(
                            Key.SYS_RATE_LIMITING_USER_AUTH_FAILURES_PER_MIN));
        case USER_REG_BY_IP:
            return TokenBucketRateLimiter.createMaxTokensPerHour(
                    ConfigManager.instance().getConfigLong(
                            Key.SYS_RATE_LIMITING_USER_REG_MAX_PER_HOUR));
        case API_FAILURE_BY_IP:
            return TokenBucketRateLimiter.createMaxTokensPerMinute(
                    ConfigManager.instance().getConfigLong(
                            Key.SYS_RATE_LIMITING_API_FAILURES_PER_MIN));
        case PRINT_IN_FAILURE_BY_ADDR:
            return TokenBucketRateLimiter.createMaxTokensPerMinute(
                    ConfigManager.instance().getConfigLong(
                            Key.SYS_RATE_LIMITING_PRINT_IN_FAILURES_PER_MIN));
        default:
            throw new SpException(
                    String.format("[%s] not supported", limitEnum.toString()));
        }

    }

    /**
     * Gets the rate limiter by ID.
     *
     * @param limitEnum
     * @param id
     * @return rate limiter
     */
    private IRateLimiter getRateLimiter(final LimitEnum limitEnum,
            final String id) {

        final IRateLimiter limiter;

        final ConcurrentHashMap<String, IRateLimiter> limiterMap =
                this.limitApps.get(limitEnum);

        final IRateLimiter limiterCur = limiterMap.get(id);

        if (limiterCur == null) {

            final IRateLimiter limiterNew = this.createRateLimiter(limitEnum);
            final IRateLimiter limiterPrv =
                    limiterMap.putIfAbsent(id, limiterNew);

            if (limiterPrv == null) {
                limiter = limiterNew;
            } else {
                limiter = limiterPrv;
            }
        } else {
            limiter = limiterCur;
        }
        return limiter;
    }

    @Override
    public int cleanUp(final LimitEnum limitEnum) {

        final Iterator<Map.Entry<String, IRateLimiter>> iterator =
                this.limitApps.get(limitEnum).entrySet().iterator();

        int nRemoved = 0;

        while (iterator.hasNext()) {

            final Map.Entry<String, IRateLimiter> entry = iterator.next();
            final IRateLimiter limiter = entry.getValue();

            if (limiter.isIdle()) {
                iterator.remove();
                nRemoved++;
            }
        }
        return nRemoved;
    }

    @Override
    public int cleanUp() {

        final Iterator<LimitEnum> iterator = this.limitApps.keySet().iterator();
        int nRemoved = 0;

        while (iterator.hasNext()) {
            nRemoved += this.cleanUp(iterator.next());
        }
        return nRemoved;
    }

    @Override
    public boolean consumeEvent(final LimitEnum limitEnum, final IEvent event) {
        return this.getRateLimiter(limitEnum, event.id()).consumeEvent();
    }

    @Override
    public long waitTimeForEvent(final LimitEnum limitEnum,
            final IEvent event) {
        return this.getRateLimiter(limitEnum, event.id()).waitTimeForEvent();
    }

    @Override
    public long consumeOrWaitForEvent(final LimitEnum limitType,
            final RateLimiterService.IEvent event,
            final IRateLimiterListener listener) throws EndlessWaitException {

        final long waitMsec;

        if (this.consumeEvent(limitType, event)) {
            waitMsec = 0;
        } else {
            waitMsec = this.waitTimeForEvent(limitType, event);
            if (waitMsec < 0) {
                throw new EndlessWaitException();
            }
            listener.onRateLimited(event, waitMsec);
            try {
                Thread.sleep(waitMsec);
            } catch (InterruptedException e) {
                throw new EndlessWaitException();
            }
        }
        return waitMsec;
    }

    @Override
    public void start() {
        for (LimitEnum val : LimitEnum.values()) {
            this.limitApps.put(val, this.createRateLimiterMap());
        }
    }

    @Override
    public void shutdown() {
        // no code intended
    }

}
