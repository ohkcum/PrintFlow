/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2011-2025 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2025 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.ratelimiter;

import org.apache.commons.lang3.time.DateUtils;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.util.DateUtil;

/**
 * An IRateLimiter implementation of the
 * <a href="https://en.wikipedia.org/wiki/Token_bucket">Token Bucket
 * algorithm</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class TokenBucketRateLimiter implements IRateLimiter {

    /**
     * Time source to get the current time from.
     */
    public interface ITimeSource {
        /**
         * @return current time in milliseconds.
         */
        long currentTimeMsec();
    }

    /** */
    private static final ITimeSource DEFAULT_TIMESOURCE = new ITimeSource() {
        @Override
        public long currentTimeMsec() {
            return System.currentTimeMillis();
        }
    };

    /**
     * Endless wait as negative value.
     */
    private static final long ENDLESS_WAIT = -1;

    /** */
    private static final long LONG_INFINITY = (long) Double.POSITIVE_INFINITY;

    /** */
    private static final double DOUBLE_ZERO = 0.0;
    /** */
    private static final double DOUBLE_ONE = 1.0;
    /** */
    private static final double DOUBLE_THOUSAND = 1000.0;

    /**
     * Number of tokens that become available at each idle millisecond.
     */
    private final double tokensReleasedPerMsec;

    /**
     * Max tokens this bucket can hold.
     */
    private final double tokensMax;

    /**
     * Source that provides the current (now) time.
     */
    private final ITimeSource timeSource;

    /**
     * Last time (milliseconds) this bucket was updated.
     */
    private long lastUpdateTimeMsec;

    /**
     * Last time (milliseconds) event was consumed.
     */
    private long lastConsumedTimeMsec;

    /**
     * Number of tokens available in this bucket.
     */
    private double tokensAvailable;

    /** */
    private long idlePeriodMsec;

    /**
     * @param tokens
     * @param seconds
     *            period length in seconds.
     * @return rate limiter
     */
    public static IRateLimiter createMaxTokensPerPeriodSecs(final long tokens,
            final int seconds) {
        return createMaxTokensPerPeriodSecs(tokens, seconds,
                DEFAULT_TIMESOURCE);
    }

    /**
     * @param tokens
     * @param seconds
     *            period length in seconds.
     * @param source
     * @return rate limiter
     */
    public static IRateLimiter createMaxTokensPerPeriodSecs(final long tokens,
            final int seconds, final ITimeSource source) {

        final double maxTokens = tokens;
        final double ratePerSecond = tokens / (double) seconds;

        return new TokenBucketRateLimiter(ratePerSecond, maxTokens, maxTokens,
                source, DateUtils.MILLIS_PER_SECOND * seconds);
    }

    /**
     * @param tokens
     * @return rate limiter
     */
    public static IRateLimiter createMaxTokensPerMinute(final long tokens) {
        return createMaxTokensPerPeriodSecs(tokens, DateUtil.SECONDS_IN_MINUTE);
    }

    /**
     * @param tokens
     * @return rate limiter
     */
    public static IRateLimiter createMaxTokensPerHour(final long tokens) {
        return createMaxTokensPerPeriodSecs(tokens,
                DateUtil.SECONDS_IN_MINUTE * DateUtil.MINUTES_IN_HOUR);
    }

    /**
     * @param tokens
     * @param source
     * @return rate limiter
     */
    public static IRateLimiter createMaxTokensPerMinute(final long tokens,
            final ITimeSource source) {
        return createMaxTokensPerPeriodSecs(tokens, DateUtil.SECONDS_IN_MINUTE,
                source);
    }

    /**
     * @param ratePerSecond
     * @param maxTokens
     * @return rate limiter
     */
    public static IRateLimiter create(final double ratePerSecond,
            final double maxTokens) {

        return create(ratePerSecond, maxTokens, DOUBLE_ZERO,
                DEFAULT_TIMESOURCE);
    }

    /**
     * @param ratePerSecond
     * @param maxTokens
     * @param initTokens
     * @return rate limiter
     */
    public static IRateLimiter create(final double ratePerSecond,
            final double maxTokens, final double initTokens) {

        return create(ratePerSecond, maxTokens, initTokens, DEFAULT_TIMESOURCE);
    }

    /**
     * @param ratePerSecond
     * @param maxTokens
     * @param initTokens
     * @param source
     * @return rate limiter
     */
    public static IRateLimiter create(final double ratePerSecond,
            final double maxTokens, final double initTokens,
            final ITimeSource source) {

        return new TokenBucketRateLimiter(ratePerSecond, maxTokens, initTokens,
                source, DateUtils.MILLIS_PER_SECOND);
    }

    /**
     * @param releaseRatePerSecond
     *            number of tokens released per second.
     * @param maxTokens
     *            max tokens in bucket.
     * @param initTokens
     *            available tokens in bucket.
     * @param timeSrc
     *            time source.
     * @param idleTimeMsec
     */
    private TokenBucketRateLimiter(final double releaseRatePerSecond,
            final double maxTokens, final double initTokens,
            final ITimeSource timeSrc, final long idleTimeMsec) {

        this.idlePeriodMsec = idleTimeMsec;
        this.tokensReleasedPerMsec = releaseRatePerSecond / DOUBLE_THOUSAND;
        this.tokensMax = maxTokens;
        this.tokensAvailable = initTokens;
        this.timeSource = timeSrc;
        this.lastUpdateTimeMsec = timeSrc.currentTimeMsec();
        this.lastConsumedTimeMsec = 0;
    }

    /**
     * Check if requested tokens are greater than zero, if not an unchecked
     * exception is thrown.
     *
     * @param tokens
     *            tokens requested.
     */
    private static void checkTokensRequested(final double tokens) {
        if (tokens <= DOUBLE_ZERO) {
            throw new SpException("Error requested tokens [" + tokens
                    + "] : must be greater than zero");
        }
    }

    @Override
    public boolean consumeEvent() {
        return consumeEvent(DOUBLE_ONE);
    }

    @Override
    public boolean consumeEvent(final double tokensRequested) {

        checkTokensRequested(tokensRequested);

        this.updateTokensAvailable();

        final boolean success = this.tokensAvailable >= tokensRequested;

        if (success) {
            // Token request accepted: reduce available tokens ...
            this.tokensAvailable -= tokensRequested;
            // ... and set time consumed.
            this.lastConsumedTimeMsec = this.lastUpdateTimeMsec;
        }
        return success;
    }

    @Override
    public long waitTimeForEvent() {
        return waitTimeForEvent(DOUBLE_ONE);
    }

    @Override
    public long waitTimeForEvent(final double tokensRequested) {

        checkTokensRequested(tokensRequested);

        if (tokensRequested <= this.tokensAvailable) {
            return 0;
        }

        this.updateTokensAvailable();

        if (tokensRequested <= this.tokensAvailable) {
            return 0;
        }

        if (tokensRequested > this.tokensMax) {
            return ENDLESS_WAIT;
        }

        final double lack = tokensRequested - this.tokensAvailable;
        final double waitTime = Math.ceil(lack / this.tokensReleasedPerMsec);

        if (waitTime > LONG_INFINITY) {
            return ENDLESS_WAIT;
        }
        return (long) waitTime;
    }

    /**
     * Uses current time to update {@link #tokensAvailable}.
     */
    private void updateTokensAvailable() {

        final long now = this.timeSource.currentTimeMsec();
        final long msecPassed = now - this.lastUpdateTimeMsec;

        if (msecPassed > 0) {

            // Tokens available during idle time.
            final double tokensReleased =
                    msecPassed * this.tokensReleasedPerMsec;

            // Calculate tokens available.
            final double tokensAvailableCalc =
                    tokensReleased + this.tokensAvailable;

            // Calculated available tokens cannot exceed bucket capacity.
            this.tokensAvailable =
                    Math.min(tokensAvailableCalc, this.tokensMax);

            this.lastUpdateTimeMsec = now;
        }
    }

    @Override
    public boolean isIdle() {

        final boolean idle;

        if (this.lastConsumedTimeMsec == 0) {
            idle = true;
        } else {
            final long msecPassed = this.timeSource.currentTimeMsec()
                    - this.lastConsumedTimeMsec;
            idle = msecPassed >= this.idlePeriodMsec;
        }
        return idle;
    }

}
