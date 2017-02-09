/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
/***************************************************************************************
 * Attribution Notice
 *
 * This file is imported from Metrics (https://github.com/codahale/metrics subproject metrics-core).
 * Metrics is Copyright (c) 2010-2012 Coda Hale, Yammer.com
 * Metrics is Published under Apache Software License 2.0, see LICENSE in root folder.
 *
 * Thank you for the Metrics developers efforts in making their library available under an Apache license.
 * EsperTech incorporates Metrics version 0.2.2 in source code form since Metrics depends on SLF4J
 * and this dependency is not possible to introduce for Esper.
 * *************************************************************************************
 */
package com.espertech.esper.metrics.codahale_metrics.metrics.core;


import com.espertech.esper.metrics.codahale_metrics.metrics.stats.ExponentiallyDecayingSample;
import com.espertech.esper.metrics.codahale_metrics.metrics.stats.Sample;
import com.espertech.esper.metrics.codahale_metrics.metrics.stats.Snapshot;
import com.espertech.esper.metrics.codahale_metrics.metrics.stats.UniformSample;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.sqrt;

/**
 * A metric which calculates the distribution of a value.
 *
 * @see <a href="http://www.johndcook.com/standard_deviation.html">Accurately computing running
 * variance</a>
 */
public class Histogram implements Metric, Sampling, Summarizable {
    private static final int DEFAULT_SAMPLE_SIZE = 1028;
    private static final double DEFAULT_ALPHA = 0.015;

    /**
     * The type of sampling the histogram should be performing.
     */
    enum SampleType {
        /**
         * Uses a uniform sample of 1028 elements, which offers a 99.9% confidence level with a 5%
         * margin of error assuming a normal distribution.
         */
        UNIFORM {
            @Override
            public Sample newSample() {
                return new UniformSample(DEFAULT_SAMPLE_SIZE);
            }
        },

        /**
         * Uses an exponentially decaying sample of 1028 elements, which offers a 99.9% confidence
         * level with a 5% margin of error assuming a normal distribution, and an alpha factor of
         * 0.015, which heavily biases the sample to the past 5 minutes of measurements.
         */
        BIASED {
            @Override
            public Sample newSample() {
                return new ExponentiallyDecayingSample(DEFAULT_SAMPLE_SIZE, DEFAULT_ALPHA);
            }
        };

        public abstract Sample newSample();
    }

    private final Sample sample;
    private final AtomicLong min = new AtomicLong();
    private final AtomicLong max = new AtomicLong();
    private final AtomicLong sum = new AtomicLong();
    // These are for the Welford algorithm for calculating running variance
    // without floating-point doom.
    private final AtomicReference<double[]> variance =
            new AtomicReference<double[]>(new double[]{-1, 0}); // M, S
    private final AtomicLong count = new AtomicLong();

    /**
     * Creates a new {@link Histogram} with the given sample type.
     *
     * @param type the type of sample to use
     */
    Histogram(SampleType type) {
        this(type.newSample());
    }

    /**
     * Creates a new {@link Histogram} with the given sample.
     *
     * @param sample the sample to create a histogram from
     */
    Histogram(Sample sample) {
        this.sample = sample;
        clear();
    }

    /**
     * Clears all recorded values.
     */
    public void clear() {
        sample.clear();
        count.set(0);
        max.set(Long.MIN_VALUE);
        min.set(Long.MAX_VALUE);
        sum.set(0);
        variance.set(new double[]{-1, 0});
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(int value) {
        update((long) value);
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(long value) {
        count.incrementAndGet();
        sample.update(value);
        setMax(value);
        setMin(value);
        sum.getAndAdd(value);
        updateVariance(value);
    }

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    public long count() {
        return count.get();
    }

    /* (non-Javadoc)
     * @see com.yammer.metrics.core.Summarizable#max()
     */
    @Override
    public double max() {
        if (count() > 0) {
            return max.get();
        }
        return 0.0;
    }

    /* (non-Javadoc)
     * @see com.yammer.metrics.core.Summarizable#min()
     */
    @Override
    public double min() {
        if (count() > 0) {
            return min.get();
        }
        return 0.0;
    }

    /* (non-Javadoc)
     * @see com.yammer.metrics.core.Summarizable#mean()
     */
    @Override
    public double mean() {
        if (count() > 0) {
            return sum.get() / (double) count();
        }
        return 0.0;
    }

    /* (non-Javadoc)
     * @see com.yammer.metrics.core.Summarizable#stdDev()
     */
    @Override
    public double stdDev() {
        if (count() > 0) {
            return sqrt(variance());
        }
        return 0.0;
    }

    /* (non-Javadoc)
     * @see com.yammer.metrics.core.Summarizable#sum()
     */
    @Override
    public double sum() {
        return (double) sum.get();
    }

    @Override
    public Snapshot getSnapshot() {
        return sample.getSnapshot();
    }

    private double variance() {
        if (count() <= 1) {
            return 0.0;
        }
        return variance.get()[1] / (count() - 1);
    }

    private void setMax(long potentialMax) {
        boolean done = false;
        while (!done) {
            final long currentMax = max.get();
            done = currentMax >= potentialMax || max.compareAndSet(currentMax, potentialMax);
        }
    }

    private void setMin(long potentialMin) {
        boolean done = false;
        while (!done) {
            final long currentMin = min.get();
            done = currentMin <= potentialMin || min.compareAndSet(currentMin, potentialMin);
        }
    }

    private void updateVariance(long value) {
        while (true) {
            final double[] oldValues = variance.get();
            final double[] newValues = new double[2];
            if (oldValues[0] == -1) {
                newValues[0] = value;
                newValues[1] = 0;
            } else {
                final double oldM = oldValues[0];
                final double oldS = oldValues[1];

                final double newM = oldM + ((value - oldM) / count());
                final double newS = oldS + ((value - oldM) * (value - newM));

                newValues[0] = newM;
                newValues[1] = newS;
            }
            if (variance.compareAndSet(oldValues, newValues)) {
                return;
            }
        }
    }

    @Override
    public <T> void processWith(MetricProcessor<T> processor, MetricName name, T context) throws Exception {
        processor.processHistogram(name, this, context);
    }
}
