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
package com.espertech.esper.client.context;

import com.espertech.esper.core.context.mgr.ContextControllerInitTerm;

import java.util.Map;

/**
 * Context partition identifier for overlapping and non-overlapping contexts.
 */
public class ContextPartitionIdentifierInitiatedTerminated extends ContextPartitionIdentifier {
    private static final long serialVersionUID = 1035193605492122638L;
    private Map<String, Object> properties;
    private long startTime;
    private Long endTime;

    /**
     * Ctor.
     */
    public ContextPartitionIdentifierInitiatedTerminated() {
    }

    /**
     * Ctor.
     *
     * @param properties of triggering object
     * @param startTime  start time
     * @param endTime    optional end time
     */
    public ContextPartitionIdentifierInitiatedTerminated(Map<String, Object> properties, long startTime, Long endTime) {
        this.properties = properties;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Event or pattern information.
     *
     * @return starting or initiating information
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Sets the event or pattern information.
     *
     * @param properties starting or initiating information
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the start time of the context partition.
     *
     * @return start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of the context partition.
     *
     * @param startTime start time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the end time of the context partition, if it can be computed
     *
     * @return end time
     */
    public Long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time of the context partition, if it can be computed
     *
     * @param endTime end time
     */
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public boolean compareTo(ContextPartitionIdentifier other) {
        if (!(other instanceof ContextPartitionIdentifierInitiatedTerminated)) {
            return false;
        }
        ContextPartitionIdentifierInitiatedTerminated ito = (ContextPartitionIdentifierInitiatedTerminated) other;
        return ContextControllerInitTerm.compare(startTime, properties, endTime, ito.startTime, ito.properties, ito.endTime);
    }

    public String toString() {
        return "ContextPartitionIdentifierInitiatedTerminated{" +
                "properties=" + properties +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
