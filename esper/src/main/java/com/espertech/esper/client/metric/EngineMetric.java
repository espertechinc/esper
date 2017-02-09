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
package com.espertech.esper.client.metric;

/**
 * Reports engine-level instrumentation values.
 */
public class EngineMetric extends MetricEvent {
    private final long timestamp;
    private final long inputCount;
    private final long inputCountDelta;
    private final long scheduleDepth;

    /**
     * Ctor.
     *
     * @param engineURI       engine URI
     * @param timestamp       engine timestamp
     * @param inputCount      number of input events
     * @param inputCountDelta number of input events since last
     * @param scheduleDepth   schedule depth
     */
    public EngineMetric(String engineURI, long timestamp, long inputCount, long inputCountDelta, long scheduleDepth) {
        super(engineURI);
        this.timestamp = timestamp;
        this.inputCount = inputCount;
        this.inputCountDelta = inputCountDelta;
        this.scheduleDepth = scheduleDepth;
    }

    /**
     * Returns input count since engine initialization cumulative.
     *
     * @return input count
     */
    public long getInputCount() {
        return inputCount;
    }

    /**
     * Returns schedule depth.
     *
     * @return schedule depth
     */
    public long getScheduleDepth() {
        return scheduleDepth;
    }

    /**
     * Returns engine timestamp.
     *
     * @return timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns input count since last reporting period.
     *
     * @return input count
     */
    public long getInputCountDelta() {
        return inputCountDelta;
    }
}
