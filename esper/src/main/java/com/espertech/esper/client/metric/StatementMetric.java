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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Reports statement-level instrumentation values.
 */
public class StatementMetric extends MetricEvent {
    private long timestamp;
    private String statementName;
    private AtomicLong cpuTime;
    private AtomicLong wallTime;
    private AtomicLong numInput;
    private AtomicLong numOutputRStream;
    private AtomicLong numOutputIStream;

    /**
     * Ctor.
     *
     * @param engineURI     engine URI
     * @param statementName statement name
     */
    public StatementMetric(String engineURI, String statementName) {
        super(engineURI);
        this.statementName = statementName;
        this.cpuTime = new AtomicLong();
        this.wallTime = new AtomicLong();
        this.numOutputIStream = new AtomicLong();
        this.numOutputRStream = new AtomicLong();
        this.numInput = new AtomicLong();
    }

    /**
     * Returns statement name.
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns CPU time in nanoseconds.
     *
     * @return cpu time
     */
    public long getCpuTime() {
        return cpuTime.get();
    }

    /**
     * Sets engine timestamp.
     *
     * @param timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
     * Adds CPU time.
     *
     * @param delta to add
     */
    public void addCPUTime(long delta) {
        cpuTime.addAndGet(delta);
    }

    /**
     * Adds wall time.
     *
     * @param wall to add
     */
    public void addWallTime(long wall) {
        wallTime.addAndGet(wall);
    }

    /**
     * Returns wall time in nanoseconds.
     *
     * @return wall time
     */
    public long getWallTime() {
        return wallTime.get();
    }

    /**
     * Returns number of output rows in remove stream.
     *
     * @return number of output rows in remove stream
     */
    public long getNumOutputRStream() {
        return numOutputRStream.get();
    }

    /**
     * Returns number of output rows in insert stream.
     *
     * @return number of output rows in insert stream
     */
    public long getNumOutputIStream() {
        return numOutputIStream.get();
    }

    /**
     * Adds number of output rows in insert stream.
     *
     * @param numIStream to add
     */
    public void addNumOutputIStream(int numIStream) {
        numOutputIStream.addAndGet(numIStream);
    }

    /**
     * Adds number of output rows in remove stream.
     *
     * @param numRStream to add
     */
    public void addNumOutputRStream(int numRStream) {
        numOutputRStream.addAndGet(numRStream);
    }

    /**
     * Returns the number of input events.
     *
     * @return number of input events
     */
    public long getNumInput() {
        return numInput.get();
    }

    /**
     * Adds number of input events.
     *
     * @param numInputAdd to add
     */
    public void addNumInput(long numInputAdd) {
        numInput.addAndGet(numInputAdd);
    }
}
