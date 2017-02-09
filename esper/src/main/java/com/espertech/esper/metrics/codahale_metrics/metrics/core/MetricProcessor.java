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

/**
 * A processor of metric instances.
 */
public interface MetricProcessor<T> {
    /**
     * Process the given {@link Metered} instance.
     *
     * @param name    the name of the meter
     * @param meter   the meter
     * @param context the context of the meter
     * @throws Exception if something goes wrong
     */
    void processMeter(MetricName name, Metered meter, T context) throws Exception;

    /**
     * Process the given counter.
     *
     * @param name    the name of the counter
     * @param counter the counter
     * @param context the context of the meter
     * @throws Exception if something goes wrong
     */
    void processCounter(MetricName name, Counter counter, T context) throws Exception;

    /**
     * Process the given histogram.
     *
     * @param name      the name of the histogram
     * @param histogram the histogram
     * @param context   the context of the meter
     * @throws Exception if something goes wrong
     */
    void processHistogram(MetricName name, Histogram histogram, T context) throws Exception;

    /**
     * Process the given timer.
     *
     * @param name    the name of the timer
     * @param timer   the timer
     * @param context the context of the meter
     * @throws Exception if something goes wrong
     */
    void processTimer(MetricName name, Timer timer, T context) throws Exception;

    /**
     * Process the given gauge.
     *
     * @param name    the name of the gauge
     * @param gauge   the gauge
     * @param context the context of the meter
     * @throws Exception if something goes wrong
     */
    void processGauge(MetricName name, Gauge<?> gauge, T context) throws Exception;
}
