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
package com.espertech.esper.epl.metric;

import com.espertech.esper.client.metric.StatementMetric;

/**
 * Metrics execution producing statement metric events.
 */
public class MetricExecStatement implements MetricExec {
    private final MetricEventRouter metricEventRouter;
    private final MetricScheduleService metricScheduleService;
    private final int statementGroup;

    private long interval;

    /**
     * Ctor.
     *
     * @param metricEventRouter     for routing metric events
     * @param metricScheduleService for scheduling a new execution
     * @param interval              for rescheduling the execution
     * @param statementGroup        group number of statement group
     */
    public MetricExecStatement(MetricEventRouter metricEventRouter, MetricScheduleService metricScheduleService, long interval, int statementGroup) {
        this.metricEventRouter = metricEventRouter;
        this.metricScheduleService = metricScheduleService;
        this.interval = interval;
        this.statementGroup = statementGroup;
    }

    public void execute(MetricExecutionContext context) {
        long timestamp = metricScheduleService.getCurrentTime();
        StatementMetric[] metrics = context.getStatementMetricRepository().reportGroup(statementGroup);
        if (metrics != null) {
            for (int i = 0; i < metrics.length; i++) {
                StatementMetric metric = metrics[i];
                if (metric != null) {
                    metric.setTimestamp(timestamp);
                    metricEventRouter.route(metrics[i]);
                }
            }
        }

        if (interval != -1) {
            metricScheduleService.add(interval, this);
        }
    }

    /**
     * Set a new interval, cancels the existing schedule, re-establishes the new schedule if the interval is a
     * positive number.
     *
     * @param newInterval to set
     */
    public void setInterval(long newInterval) {
        interval = newInterval;
        metricScheduleService.remove(this);
        if (interval > 0) {
            metricScheduleService.add(interval, this);
        }
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
