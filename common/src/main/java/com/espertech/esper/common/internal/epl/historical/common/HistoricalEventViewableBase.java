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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.IterablesArrayIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCache;
import com.espertech.esper.common.internal.epl.historical.execstrategy.PollExecStrategy;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategy;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.UnindexedEventTableList;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Iterator;
import java.util.List;

/**
 * Implements a poller viewable that uses a polling strategy, a cache and
 * some input parameters extracted from event streams to perform the polling.
 */
public abstract class HistoricalEventViewableBase implements Viewable, HistoricalEventViewable {
    protected final HistoricalEventViewableFactoryBase factory;
    protected final PollExecStrategy pollExecStrategy;
    protected final AgentInstanceContext agentInstanceContext;
    protected HistoricalDataCache dataCache;
    protected View child;

    protected static final EventBean[][] NULL_ROWS;

    static {
        NULL_ROWS = new EventBean[1][];
        NULL_ROWS[0] = new EventBean[1];
    }

    public HistoricalEventViewableBase(HistoricalEventViewableFactoryBase factory, PollExecStrategy pollExecStrategy, AgentInstanceContext agentInstanceContext) {
        this.factory = factory;
        this.pollExecStrategy = pollExecStrategy;
        this.agentInstanceContext = agentInstanceContext;
    }

    public void stop(AgentInstanceStopServices services) {
        pollExecStrategy.destroy();
        dataCache.destroy();
    }

    public void setChild(View view) {
        this.child = view;
    }

    public View getChild() {
        return child;
    }

    private static final PollResultIndexingStrategy ITERATOR_INDEXING_STRATEGY = new PollResultIndexingStrategy() {
        public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, AgentInstanceContext agentInstanceContext) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, -1)};
        }
    };

    public EventTable[][] poll(EventBean[][] lookupEventsPerStream, PollResultIndexingStrategy indexingStrategy, ExprEvaluatorContext exprEvaluatorContext) {
        HistoricalDataCache localDataCache = factory.getDataCacheThreadLocal().get();
        boolean strategyStarted = false;

        EventTable[][] resultPerInputRow = new EventTable[lookupEventsPerStream.length][];

        // Get input parameters for each row
        EventBean[] eventsPerStream;
        for (int row = 0; row < lookupEventsPerStream.length; row++) {
            // Build lookup keys
            eventsPerStream = lookupEventsPerStream[row];
            Object lookupValue = factory.evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);

            EventTable[] result = null;

            // try the threadlocal iteration cache, if set
            Object cacheMultiKey = null;
            if (localDataCache != null || dataCache.isActive()) {
                cacheMultiKey = factory.lookupValueToMultiKey.transform(lookupValue);
            }

            if (localDataCache != null) {
                EventTable[] tables = localDataCache.getCached(cacheMultiKey);
                result = tables;
            }

            // try the connection cache
            if (result == null) {
                EventTable[] multi = dataCache.getCached(cacheMultiKey);
                if (multi != null) {
                    result = multi;
                    if (localDataCache != null) {
                        localDataCache.put(cacheMultiKey, multi);
                    }
                }
            }

            // use the result from cache
            if (result != null) {
                // found in cache
                resultPerInputRow[row] = result;
            } else {
                // not found in cache, get from actual polling (db query)
                try {
                    if (!strategyStarted) {
                        pollExecStrategy.start();
                        strategyStarted = true;
                    }

                    // Poll using the polling execution strategy and lookup values
                    List<EventBean> pollResult = pollExecStrategy.poll(lookupValue, agentInstanceContext);

                    // index the result, if required, using an indexing strategy
                    EventTable[] indexTable = indexingStrategy.index(pollResult, dataCache.isActive(), agentInstanceContext);

                    // assign to row
                    resultPerInputRow[row] = indexTable;

                    // save in cache
                    dataCache.put(cacheMultiKey, indexTable);

                    if (localDataCache != null) {
                        localDataCache.put(cacheMultiKey, indexTable);
                    }
                } catch (EPException ex) {
                    if (strategyStarted) {
                        pollExecStrategy.done();
                    }
                    throw ex;
                }
            }
        }

        if (strategyStarted) {
            pollExecStrategy.done();
        }

        return resultPerInputRow;
    }

    public EventType getEventType() {
        return factory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        EventTable[][] tablesPerRow = poll(NULL_ROWS, ITERATOR_INDEXING_STRATEGY, agentInstanceContext);
        return new IterablesArrayIterator(tablesPerRow);
    }

    public ThreadLocal<HistoricalDataCache> getDataCacheThreadLocal() {
        return factory.getDataCacheThreadLocal();
    }

    public boolean hasRequiredStreams() {
        return factory.isHasRequiredStreams();
    }

    public HistoricalDataCache getOptionalDataCache() {
        return dataCache;
    }
}
