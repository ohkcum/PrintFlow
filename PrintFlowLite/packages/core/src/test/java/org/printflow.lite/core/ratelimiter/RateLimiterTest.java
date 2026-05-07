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
import org.junit.jupiter.api.Test;
import org.printflow.lite.core.ratelimiter.TokenBucketRateLimiter.ITimeSource;

import junit.framework.Assert;

/**
 * Unit Tests for the RateLimiterTokenBucket.
 *
 * @author Rijk Ravestein
 */
public final class RateLimiterTest {

    /** */
    private final ITimeSource timeSourceZero = new ITimeSource() {
        @Override
        public long currentTimeMsec() {
            return 0;
        }
    };

    /** */
    private final class TimeSourceEditable implements ITimeSource {

        /** */
        private long theTime;

        /**
         * @param now
         */
        TimeSourceEditable(final long now) {
            theTime = now;
        }

        @Override
        public long currentTimeMsec() {
            return theTime;
        }

        public void setCurrentTimeMsec(final long time) {
            theTime = time;
        }

        public void addMsec(final long time) {
            theTime += time;
        }

    }

    /** */
    private static final long MSEC_PER_SECOND = 1000;

    /** Evaluation margin in milliseconds. */
    private static final long EVAL_MARGIN_MSEC = 50;

    /**
     * Creates test case.
     */
    public RateLimiterTest() {
    }

    /**
     * Default TimeSource and initial amount in action while waiting.
     */
    @Test
    public void test001() {

        final long ratePerSecondMax = 3;

        for (long ratePerSecond =
                1; ratePerSecond <= ratePerSecondMax; ratePerSecond++) {

            final double maxTokens = 5.0;

            final IRateLimiter limiter =
                    TokenBucketRateLimiter.create(ratePerSecond, maxTokens);
            Assert.assertNotNull(limiter);

            final double tokens2Consume = 1.0;

            long waitExpected = Math
                    .round((tokens2Consume * MSEC_PER_SECOND) / ratePerSecond);
            long waitCalculated = limiter.waitTimeForEvent(tokens2Consume);

            Assert.assertTrue(
                    waitExpected - EVAL_MARGIN_MSEC <= waitCalculated);

            Assert.assertTrue(limiter.waitTimeForEvent(
                    tokens2Consume) <= waitExpected + EVAL_MARGIN_MSEC);

            final long sleepMsec = 500;

            try {
                Thread.sleep(sleepMsec);
            } catch (InterruptedException e) {
                // no code intended
            }

            waitExpected = Math
                    .round((tokens2Consume * MSEC_PER_SECOND) / ratePerSecond)
                    - sleepMsec;
            waitExpected = Math.max(waitExpected, 0);

            waitCalculated = limiter.waitTimeForEvent(tokens2Consume);

            Assert.assertTrue(
                    waitExpected - EVAL_MARGIN_MSEC <= waitCalculated);

            Assert.assertTrue(waitCalculated <= waitExpected);
        }
    }

    /**
     * Testing {@link #timeSourceZero}.
     */
    @Test
    public void test002() {

        // Just "1" and "2" (do not use oneven values, because this leads to
        // fractions and rounding differences.
        final long ratePerSecondMax = 2;

        for (long ratePerSecond =
                1; ratePerSecond <= ratePerSecondMax; ratePerSecond++) {

            final double tokens2Consume = 1.0;

            final IRateLimiter limiter = TokenBucketRateLimiter
                    .create(ratePerSecond, 5.0, 0.0, this.timeSourceZero);
            Assert.assertNotNull(limiter);

            long waitExpected = Math
                    .round((tokens2Consume * MSEC_PER_SECOND) / ratePerSecond);

            // Time is fixed in time source, so result must be exactly the same.
            Assert.assertEquals(waitExpected,
                    limiter.waitTimeForEvent(tokens2Consume));
        }
    }

    /**
     * Testing {@link #timeSourceZero}: as time is fixed, no new tokens become
     * available.
     */
    @Test
    public void test003() {

        final IRateLimiter limiter = TokenBucketRateLimiter.create(1.0, 1.0,
                1.0, this.timeSourceZero);
        Assert.assertNotNull(limiter);

        Assert.assertTrue(limiter.consumeEvent());
        Assert.assertFalse(limiter.consumeEvent());
    }

    /**
     * Testing {@link #timeSourceZero}: wait time.
     */
    @Test
    public void test004() {

        final double initTokens = 1.0;

        final IRateLimiter limiter = TokenBucketRateLimiter.create(1.0, 1.0,
                initTokens, this.timeSourceZero);
        Assert.assertNotNull(limiter);

        Assert.assertEquals(0, limiter.waitTimeForEvent(initTokens));
        Assert.assertEquals(0, limiter.waitTimeForEvent());
    }

    /**
     * Testing {@link TimeSourceEditable}: available tokens after elapsed time.
     */
    @Test
    public void test005() {

        final TimeSourceEditable timeSource = new TimeSourceEditable(0);

        final IRateLimiter limiter =
                TokenBucketRateLimiter.create(1.0, 1.0, 0.0, timeSource);
        Assert.assertNotNull(limiter);

        final long tokens = 1;
        Assert.assertEquals(tokens * MSEC_PER_SECOND,
                limiter.waitTimeForEvent(tokens));
        Assert.assertFalse(limiter.consumeEvent());

        //
        timeSource.setCurrentTimeMsec(MSEC_PER_SECOND - 1);
        Assert.assertFalse(limiter.consumeEvent());

        //
        timeSource.setCurrentTimeMsec(MSEC_PER_SECOND);
        Assert.assertTrue(limiter.consumeEvent());
        Assert.assertFalse(limiter.consumeEvent());
    }

    /**
     * Testing {@link TimeSourceEditable}: no wait if amount is available.
     */
    @Test
    public void test006() {

        final TimeSourceEditable timeSource = new TimeSourceEditable(0);

        final IRateLimiter limiter =
                TokenBucketRateLimiter.create(1.0, 1.0, 1.0, timeSource);
        Assert.assertNotNull(limiter);

        Assert.assertEquals(0, limiter.waitTimeForEvent(1.0));
    }

    /**
     * Testing {@link TimeSourceEditable}: no wait if amount becomes available
     * after elapsed time.
     */
    @Test
    public void test007() {
        final TimeSourceEditable timeSource = new TimeSourceEditable(0);

        final IRateLimiter limiter =
                TokenBucketRateLimiter.create(1.0, 2.0, 1.0, timeSource);
        Assert.assertNotNull(limiter);

        Assert.assertEquals(0, limiter.waitTimeForEvent(1.0));
        Assert.assertEquals(MSEC_PER_SECOND, limiter.waitTimeForEvent(2.0));
        //
        timeSource.addMsec(MSEC_PER_SECOND);
        Assert.assertEquals(0, limiter.waitTimeForEvent(2.0));
    }

    /**
     * Endless wait for amount that exceeds max.
     */
    @Test
    public void test008() {
        final TimeSourceEditable timeSource = new TimeSourceEditable(0);

        final double maxTokens = 2.0;
        final IRateLimiter limiter =
                TokenBucketRateLimiter.create(1.0, maxTokens, 1.0, timeSource);
        Assert.assertNotNull(limiter);

        final double maxTokensEx = maxTokens + 0.1;
        Assert.assertTrue(limiter.waitTimeForEvent(maxTokensEx) < 0);
    }

    /**
     * Endless wait if wait time is too large.
     */
    @Test
    public void test009() {

        final TimeSourceEditable timeSource = new TimeSourceEditable(0);

        // Force wait time too large by setting a very low rate.
        final double ratePerSecond = 1e-9;
        // ... and a very high max
        final double maxTokens = 1e9;

        final IRateLimiter limiter = TokenBucketRateLimiter
                .create(ratePerSecond, maxTokens, 0.0, timeSource);
        Assert.assertNotNull(limiter);

        Assert.assertTrue(limiter.waitTimeForEvent(maxTokens) < 0);
    }

    /**
     * Idle state.
     */
    @Test
    public void test010() {

        final TimeSourceEditable timeSource = new TimeSourceEditable(0);

        final IRateLimiter limiter =
                TokenBucketRateLimiter.createMaxTokensPerMinute(10, timeSource);
        Assert.assertNotNull(limiter);

        Assert.assertTrue(limiter.isIdle());

        timeSource.addMsec(DateUtils.MILLIS_PER_SECOND);

        limiter.consumeEvent();
        Assert.assertFalse(limiter.isIdle());

        timeSource.addMsec(DateUtils.MILLIS_PER_MINUTE / 2);
        Assert.assertFalse(limiter.isIdle());

        // Advance 1 msec before being idle
        timeSource.addMsec(DateUtils.MILLIS_PER_MINUTE / 2 - 1);
        Assert.assertFalse(limiter.isIdle());

        // Advance 1 msec
        timeSource.addMsec(1);
        Assert.assertTrue(limiter.isIdle());

        // Advance 1 msec and consume
        timeSource.addMsec(1);
        limiter.consumeEvent();
        Assert.assertFalse(limiter.isIdle());

    }
}
