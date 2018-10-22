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
package com.espertech.esper.common.internal.metrics.stmtmetrics;

import com.espertech.esper.common.client.metric.RuntimeMetric;

/**
 * Metrics execution producing runtime metric events.
 */
public class MetricExecEngine implements MetricExec {
    private final MetricEventRouter metricEventRouter;
    private final String runtimeURI;
    private final MetricScheduleService metricScheduleService;
    private final long interval;
    private RuntimeMetric lastMetric;

    /**
     * Ctor.
     *
     * @param metricEventRouter     for routing metric events
     * @param runtimeURI            runtime URI
     * @param metricScheduleService for scheduling a new execution
     * @param interval              for rescheduling the execution
     */
    public MetricExecEngine(MetricEventRouter metricEventRouter, String runtimeURI, MetricScheduleService metricScheduleService, long interval) {
        this.metricEventRouter = metricEventRouter;
        this.runtimeURI = runtimeURI;
        this.metricScheduleService = metricScheduleService;
        this.interval = interval;
    }

    public void execute(MetricExecutionContext context) {
        long inputCount = context.getFilterService().getNumEventsEvaluated();
        long schedDepth = context.getSchedulingService().getScheduleHandleCount();
        long deltaInputCount = lastMetric == null ? inputCount : inputCount - lastMetric.getInputCount();
        RuntimeMetric metric = new RuntimeMetric(runtimeURI, metricScheduleService.getCurrentTime(), inputCount, deltaInputCount, schedDepth);
        lastMetric = metric;
        metricEventRouter.route(metric);
        metricScheduleService.add(interval, this);
    }

    /**
     * Returns reporting interval.
     *
     * @return reporting interval
     */
    public long getInterval() {
        return interval;
    }
}
