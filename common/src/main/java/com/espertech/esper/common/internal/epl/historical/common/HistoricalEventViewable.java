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
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCache;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategy;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.view.core.Viewable;

/**
 * Interface for views that poll data based on information from other streams.
 */
public interface HistoricalEventViewable extends Viewable, AgentInstanceStopCallback {

    public EventType getEventType();

    /**
     * Poll for stored historical or reference data using events per stream and
     * returing for each event-per-stream row a separate list with events
     * representing the poll result.
     *
     * @param lookupEventsPerStream is the events per stream where the
     *                              first dimension is a number of rows (often 1 depending on windows used) and
     *                              the second dimension is the number of streams participating in a join.
     * @param indexingStrategy      the strategy to use for converting poll results into a indexed table for fast lookup
     * @param exprEvaluatorContext  context for expression evalauation
     * @return array of lists with one list for each event-per-stream row
     */
    public EventTable[][] poll(EventBean[][] lookupEventsPerStream, PollResultIndexingStrategy indexingStrategy, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Returns true if the parameters expressions to the historical require other stream's data,
     * or false if there are no parameters or all parameter expressions are only contants and variables without
     * properties of other stream events.
     *
     * @return indicator whether properties are required for parameter evaluation
     */
    public boolean hasRequiredStreams();

    public ThreadLocal<HistoricalDataCache> getDataCacheThreadLocal();

    public HistoricalDataCache getOptionalDataCache();
}
