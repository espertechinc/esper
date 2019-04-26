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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.historical.datacache.HistoricalDataCacheClearableMap;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableVisitor;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.strategy.QueryStrategy;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the function to determine a join result set using tables/indexes and query strategy
 * instances for each stream.
 */
public class JoinSetComposerHistoricalImpl implements JoinSetComposer {
    private final boolean allowInitIndex;
    private final EventTable[][] repositories;
    private final QueryStrategy[] queryStrategies;

    // Set semantic eliminates duplicates in result set, use Linked set to preserve order
    private Set<MultiKeyArrayOfKeys<EventBean>> oldResults = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
    private Set<MultiKeyArrayOfKeys<EventBean>> newResults = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
    private EventTable[][] tables = new EventTable[0][];
    private Viewable[] streamViews;
    private ExprEvaluatorContext staticEvalExprEvaluatorContext;

    public JoinSetComposerHistoricalImpl(boolean allowInitIndex, Map<TableLookupIndexReqKey, EventTable>[] repositories, QueryStrategy[] queryStrategies, Viewable[] streamViews,
                                         ExprEvaluatorContext staticEvalExprEvaluatorContext) {
        this.allowInitIndex = allowInitIndex;
        this.repositories = JoinSetComposerUtil.toArray(repositories, streamViews.length);
        this.queryStrategies = queryStrategies;
        this.streamViews = streamViews;
        this.staticEvalExprEvaluatorContext = staticEvalExprEvaluatorContext;
    }

    public boolean allowsInit() {
        return allowInitIndex;
    }

    public void init(EventBean[][] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (!allowInitIndex) {
            throw new IllegalStateException("Initialization by events not supported");
        }

        if (repositories == null) {
            return;
        }

        for (int i = 0; i < eventsPerStream.length; i++) {
            if ((eventsPerStream[i] != null) && (repositories[i] != null)) {
                for (int j = 0; j < repositories[i].length; j++) {
                    repositories[i][j].add(eventsPerStream[i], exprEvaluatorContext);
                }
            }
        }
    }

    public void destroy() {
        if (repositories == null) {
            return;
        }

        for (int i = 0; i < repositories.length; i++) {
            if (repositories[i] != null) {
                for (EventTable table : repositories[i]) {
                    table.destroy();
                }
            }
        }
    }

    public UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qJoinCompositionHistorical();

        oldResults.clear();
        newResults.clear();

        // join old data
        for (int i = 0; i < oldDataPerStream.length; i++) {
            if (oldDataPerStream[i] != null) {
                instrumentationCommon.qJoinCompositionQueryStrategy(false, i, oldDataPerStream[i]);
                queryStrategies[i].lookup(oldDataPerStream[i], oldResults, exprEvaluatorContext);
                instrumentationCommon.aJoinCompositionQueryStrategy();
            }
        }

        if (repositories != null) {
            // We add and remove data in one call to each index.
            // Most indexes will add first then remove as newdata and olddata may contain the same event.
            // Unique indexes may remove then add.
            for (int stream = 0; stream < newDataPerStream.length; stream++) {
                instrumentationCommon.qJoinCompositionStepUpdIndex(stream, newDataPerStream[stream], oldDataPerStream[stream]);
                for (int j = 0; j < repositories[stream].length; j++) {
                    repositories[stream][j].addRemove(newDataPerStream[stream], oldDataPerStream[stream], exprEvaluatorContext);
                }
                instrumentationCommon.aJoinCompositionStepUpdIndex();
            }
        }

        // join new data
        for (int i = 0; i < newDataPerStream.length; i++) {
            if (newDataPerStream[i] != null) {
                instrumentationCommon.qJoinCompositionQueryStrategy(true, i, newDataPerStream[i]);
                queryStrategies[i].lookup(newDataPerStream[i], newResults, exprEvaluatorContext);
                instrumentationCommon.aJoinCompositionQueryStrategy();
            }
        }

        instrumentationCommon.aJoinCompositionHistorical(newResults, oldResults);
        return new UniformPair<>(newResults, oldResults);
    }

    /**
     * Returns tables.
     *
     * @return tables for stream.
     */
    protected EventTable[][] getTables() {
        return tables;
    }

    /**
     * Returns query strategies.
     *
     * @return query strategies
     */
    protected QueryStrategy[] getQueryStrategies() {
        return queryStrategies;
    }

    public Set<MultiKeyArrayOfKeys<EventBean>> staticJoin() {
        Set<MultiKeyArrayOfKeys<EventBean>> result = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
        EventBean[] lookupEvents = new EventBean[1];

        // Assign a local cache for the thread's evaluation of the join
        // This ensures that if a SQL/method generates a row for a result set based on an input parameter, the event instance is the same
        // in the join, and thus the same row does not appear twice.
        HistoricalDataCacheClearableMap[] caches = new HistoricalDataCacheClearableMap[queryStrategies.length];
        assignThreadLocalCache(streamViews, caches);

        // perform join
        try {
            // for each stream, perform query strategy
            for (int stream = 0; stream < queryStrategies.length; stream++) {
                if (streamViews[stream] instanceof HistoricalEventViewable) {
                    HistoricalEventViewable historicalViewable = (HistoricalEventViewable) streamViews[stream];
                    if (historicalViewable.hasRequiredStreams()) {
                        continue;
                    }

                    // there may not be a query strategy since only a full outer join may need to consider all rows
                    if (queryStrategies[stream] != null) {
                        Iterator<EventBean> streamEvents = historicalViewable.iterator();
                        for (; streamEvents.hasNext(); ) {
                            lookupEvents[0] = streamEvents.next();
                            queryStrategies[stream].lookup(lookupEvents, result, staticEvalExprEvaluatorContext);
                        }
                    }
                } else {
                    Iterator<EventBean> streamEvents = streamViews[stream].iterator();
                    for (; streamEvents.hasNext(); ) {
                        lookupEvents[0] = streamEvents.next();
                        queryStrategies[stream].lookup(lookupEvents, result, staticEvalExprEvaluatorContext);
                    }
                }
            }
        } finally {
            deassignThreadLocalCache(streamViews, caches);
        }

        return result;
    }

    public void accept(EventTableVisitor visitor) {
        visitor.visit(repositories);
    }

    private void assignThreadLocalCache(Viewable[] streamViews, HistoricalDataCacheClearableMap[] caches) {
        for (int stream = 0; stream < streamViews.length; stream++) {
            if (streamViews[stream] instanceof HistoricalEventViewable) {
                HistoricalEventViewable historicalViewable = (HistoricalEventViewable) streamViews[stream];
                caches[stream] = new HistoricalDataCacheClearableMap();
                historicalViewable.getDataCacheThreadLocal().set(caches[stream]);
            }
        }
    }

    private void deassignThreadLocalCache(Viewable[] streamViews, HistoricalDataCacheClearableMap[] caches) {
        for (int stream = 0; stream < streamViews.length; stream++) {
            if (streamViews[stream] instanceof HistoricalEventViewable) {
                HistoricalEventViewable historicalViewable = (HistoricalEventViewable) streamViews[stream];
                historicalViewable.getDataCacheThreadLocal().set(null);
                caches[stream].clear();
            }
        }
    }
}
