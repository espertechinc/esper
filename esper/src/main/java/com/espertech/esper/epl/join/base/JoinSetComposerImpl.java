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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.factory.StatementAgentInstancePostLoadIndexVisitor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.plan.TableLookupIndexReqKey;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

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
    protected Set<MultiKey<EventBean>> oldResults = new LinkedHashSet<MultiKey<EventBean>>();
    protected Set<MultiKey<EventBean>> newResults = new LinkedHashSet<MultiKey<EventBean>>();

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

    public UniformPair<Set<MultiKey<EventBean>>> join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qJoinCompositionWinToWin();
        }

        oldResults.clear();
        newResults.clear();

        // join old data
        if (joinRemoveStream) {
            for (int i = 0; i < oldDataPerStream.length; i++) {
                if (oldDataPerStream[i] != null) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qJoinCompositionQueryStrategy(false, i, oldDataPerStream[i]);
                    }
                    queryStrategies[i].lookup(oldDataPerStream[i], oldResults, exprEvaluatorContext);
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aJoinCompositionQueryStrategy();
                    }
                }
            }
        }

        // We add and remove data in one call to each index.
        // Most indexes will add first then remove as newdata and olddata may contain the same event.
        // Unique indexes may remove then add.
        for (int stream = 0; stream < newDataPerStream.length; stream++) {
            for (int j = 0; j < repositories[stream].length; j++) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qJoinCompositionStepUpdIndex(stream, newDataPerStream[stream], oldDataPerStream[stream]);
                }
                repositories[stream][j].addRemove(newDataPerStream[stream], oldDataPerStream[stream], exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aJoinCompositionStepUpdIndex();
                }
            }
        }

        // join new data
        for (int i = 0; i < newDataPerStream.length; i++) {
            if (newDataPerStream[i] != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qJoinCompositionQueryStrategy(true, i, newDataPerStream[i]);
                }
                queryStrategies[i].lookup(newDataPerStream[i], newResults, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aJoinCompositionQueryStrategy();
                }
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

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aJoinCompositionWinToWin(newResults, oldResults);
        }
        return new UniformPair<Set<MultiKey<EventBean>>>(newResults, oldResults);
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

    public Set<MultiKey<EventBean>> staticJoin() {
        Set<MultiKey<EventBean>> result = new LinkedHashSet<MultiKey<EventBean>>();
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

    public void visitIndexes(StatementAgentInstancePostLoadIndexVisitor visitor) {
        visitor.visit(repositories);
    }
}
