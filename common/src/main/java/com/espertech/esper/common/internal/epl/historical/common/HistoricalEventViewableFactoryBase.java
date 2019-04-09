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
package com.espertech.esper.common.internal.epl.historical.common;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCache;

/**
 * Implements a poller viewable that uses a polling strategy, a cache and
 * some input parameters extracted from event streams to perform the polling.
 */
public abstract class HistoricalEventViewableFactoryBase implements HistoricalEventViewableFactory {
    private final ThreadLocal<HistoricalDataCache> dataCacheThreadLocal = new ThreadLocal<>();

    protected int streamNumber;
    protected EventType eventType;
    protected boolean hasRequiredStreams;
    protected int scheduleCallbackId;
    protected ExprEvaluator evaluator;
    protected HistoricalEventViewableLookupValueToMultiKey lookupValueToMultiKey;

    private static final EventBean[][] NULL_ROWS;

    static {
        NULL_ROWS = new EventBean[1][];
        NULL_ROWS[0] = new EventBean[1];
    }

    public EventType getEventType() {
        return eventType;
    }


    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public ThreadLocal<HistoricalDataCache> getDataCacheThreadLocal() {
        return dataCacheThreadLocal;
    }

    public void setHasRequiredStreams(boolean hasRequiredStreams) {
        this.hasRequiredStreams = hasRequiredStreams;
    }

    public boolean isHasRequiredStreams() {
        return hasRequiredStreams;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public ExprEvaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(ExprEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public HistoricalEventViewableLookupValueToMultiKey getLookupValueToMultiKey() {
        return lookupValueToMultiKey;
    }

    public void setLookupValueToMultiKey(HistoricalEventViewableLookupValueToMultiKey lookupValueToMultiKey) {
        this.lookupValueToMultiKey = lookupValueToMultiKey;
    }
}
