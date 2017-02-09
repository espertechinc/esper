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
package com.espertech.esper.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.db.DataCache;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.util.StopCallback;

import java.util.SortedSet;

/**
 * Interface for views that poll data based on information from other streams.
 */
public interface HistoricalEventViewable extends Viewable, ValidatedView, StopCallback {
    /**
     * Returns true if the parameters expressions to the historical require other stream's data,
     * or false if there are no parameters or all parameter expressions are only contants and variables without
     * properties of other stream events.
     *
     * @return indicator whether properties are required for parameter evaluation
     */
    public boolean hasRequiredStreams();

    /**
     * Returns the a set of stream numbers of all streams that provide property values
     * in any of the parameter expressions to the stream.
     *
     * @return set of stream numbers
     */
    public SortedSet<Integer> getRequiredStreams();

    /**
     * Historical views are expected to provide a thread-local data cache
     * for use in keeping row ({@link com.espertech.esper.client.EventBean} references) returned during iteration
     * stable, since the concept of a primary key does not exist.
     *
     * @return thread-local cache, can be null for any thread to indicate no caching
     */
    public ThreadLocal<DataCache> getDataCacheThreadLocal();

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

    public DataCache getOptionalDataCache();
}
