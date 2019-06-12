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
package com.espertech.esper.common.client.metric;

/**
 * Reports runtime-level instrumentation values.
 */
public class RuntimeMetric extends MetricEvent {
    private static final long serialVersionUID = 1875223434088366151L;
    private final long timestamp;
    private final long inputCount;
    private final long inputCountDelta;
    private final long scheduleDepth;

    /**
     * Ctor.
     *
     * @param runtimeURI      runtime URI
     * @param timestamp       runtime timestamp
     * @param inputCount      number of input events
     * @param inputCountDelta number of input events since last
     * @param scheduleDepth   schedule depth
     */
    public RuntimeMetric(String runtimeURI, long timestamp, long inputCount, long inputCountDelta, long scheduleDepth) {
        super(runtimeURI);
        this.timestamp = timestamp;
        this.inputCount = inputCount;
        this.inputCountDelta = inputCountDelta;
        this.scheduleDepth = scheduleDepth;
    }

    /**
     * Returns input count since runtime initialization, cumulative.
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
     * Returns runtime timestamp.
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
