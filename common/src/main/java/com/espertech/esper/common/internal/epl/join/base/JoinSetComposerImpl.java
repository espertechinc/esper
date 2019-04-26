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
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableVisitor;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.strategy.QueryStrategy;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the function to determine a join result set using tables/indexes and query strategy
 * instances for each stream.
 */
public class JoinSetComposerImpl implements JoinSetComposer {
    private final boolean allowInitIndex;
    protected final EventTable[][] repositories;
    protected final QueryStrategy[] queryStrategies;
    private final boolean isPureSelfJoin;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final boolean joinRemoveStream;

    // Set semantic eliminates duplicates in result set, use Linked set to preserve order
    protected Set<MultiKeyArrayOfKeys<EventBean>> oldResults = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
    protected Set<MultiKeyArrayOfKeys<EventBean>> newResults = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();

    public JoinSetComposerImpl(boolean allowInitIndex, Map<TableLookupIndexReqKey, EventTable>[] repositories, QueryStrategy[] queryStrategies, boolean isPureSelfJoin,
                               ExprEvaluatorContext exprEvaluatorContext, boolean joinRemoveStream) {
        this.allowInitIndex = allowInitIndex;
        this.repositories = JoinSetComposerUtil.toArray(repositories);
        this.queryStrategies = queryStrategies;
        this.isPureSelfJoin = isPureSelfJoin;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.joinRemoveStream = joinRemoveStream;
    }

    public boolean allowsInit() {
        return allowInitIndex;
    }

    public void init(EventBean[][] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (!allowInitIndex) {
            throw new IllegalStateException("Initialization by events not supported");
        }

        for (int i = 0; i < eventsPerStream.length; i++) {
            if (eventsPerStream[i] != null) {
                for (int j = 0; j < repositories[i].length; j++) {
                    repositories[i][j].add(eventsPerStream[i], exprEvaluatorContext);
                }
            }
        }
    }

    public void destroy() {
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
        instrumentationCommon.qJoinCompositionWinToWin();

        oldResults.clear();
        newResults.clear();

        // join old data
        if (joinRemoveStream) {
            for (int i = 0; i < oldDataPerStream.length; i++) {
                if (oldDataPerStream[i] != null) {
                    instrumentationCommon.qJoinCompositionQueryStrategy(false, i, oldDataPerStream[i]);
                    queryStrategies[i].lookup(oldDataPerStream[i], oldResults, exprEvaluatorContext);
                    instrumentationCommon.aJoinCompositionQueryStrategy();
                }
            }
        }

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

        // join new data
        for (int i = 0; i < newDataPerStream.length; i++) {
            if (newDataPerStream[i] != null) {
                instrumentationCommon.qJoinCompositionQueryStrategy(true, i, newDataPerStream[i]);
                queryStrategies[i].lookup(newDataPerStream[i], newResults, exprEvaluatorContext);
                instrumentationCommon.aJoinCompositionQueryStrategy();
            }
        }

        // on self-joins there can be repositories which are temporary for join execution
        if (isPureSelfJoin) {
            for (EventTable[] repository : repositories) {
                for (EventTable aRepository : repository) {
                    aRepository.clear();
                }
            }
        }

        instrumentationCommon.aJoinCompositionWinToWin(newResults, oldResults);
        return new UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>(newResults, oldResults);
    }

    /**
     * Returns tables.
     *
     * @return tables for stream.
     */
    protected EventTable[][] getTables() {
        return repositories;
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

        // for each stream, perform query strategy
        for (int stream = 0; stream < queryStrategies.length; stream++) {
            if (repositories[stream] == null) {
                continue;
            }

            Iterator<EventBean> streamEvents = repositories[stream][0].iterator();
            for (; streamEvents.hasNext(); ) {
                lookupEvents[0] = streamEvents.next();
                queryStrategies[stream].lookup(lookupEvents, result, exprEvaluatorContext);
            }
        }

        return result;
    }

    public void accept(EventTableVisitor visitor) {
        visitor.visit(repositories);
    }
}
