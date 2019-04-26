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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the function to determine a join result for a unidirectional stream-to-window joins,
 * in which a single stream's events are ever only evaluated using a query strategy.
 */
public class JoinSetComposerStreamToWinImpl implements JoinSetComposer {
    private final boolean allowInitIndex;
    private final EventTable[][] repositories;
    private final int streamNumber;
    private final QueryStrategy queryStrategy;

    private final boolean isResetSelfJoinRepositories;
    private final boolean[] selfJoinRepositoryResets;

    private Set<MultiKeyArrayOfKeys<EventBean>> emptyResults = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();
    private Set<MultiKeyArrayOfKeys<EventBean>> newResults = new LinkedHashSet<MultiKeyArrayOfKeys<EventBean>>();

    public JoinSetComposerStreamToWinImpl(boolean allowInitIndex, Map<TableLookupIndexReqKey, EventTable>[] repositories, boolean isPureSelfJoin, int streamNumber, QueryStrategy queryStrategy, boolean[] selfJoinRepositoryResets) {
        this.allowInitIndex = allowInitIndex;
        this.repositories = JoinSetComposerUtil.toArray(repositories);
        this.streamNumber = streamNumber;
        this.queryStrategy = queryStrategy;

        this.selfJoinRepositoryResets = selfJoinRepositoryResets;
        if (isPureSelfJoin) {
            isResetSelfJoinRepositories = true;
            Arrays.fill(selfJoinRepositoryResets, true);
        } else {
            boolean flag = false;
            for (boolean selfJoinRepositoryReset : selfJoinRepositoryResets) {
                flag |= selfJoinRepositoryReset;
            }
            this.isResetSelfJoinRepositories = flag;
        }
    }

    public boolean allowsInit() {
        return allowInitIndex;
    }

    public void init(EventBean[][] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (!allowInitIndex) {
            throw new IllegalStateException("Initialization by events not supported");
        }
        for (int i = 0; i < eventsPerStream.length; i++) {
            if ((eventsPerStream[i] != null) && (i != streamNumber)) {
                for (int j = 0; j < repositories[i].length; j++) {
                    repositories[i][j].add(eventsPerStream[i], exprEvaluatorContext);
                }
            }
        }
    }

    public void destroy() {
        for (EventTable[] repository : repositories) {
            if (repository != null) {
                for (EventTable table : repository) {
                    table.destroy();
                }
            }
        }
    }

    public UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qJoinCompositionStreamToWin();

        newResults.clear();

        // We add and remove data in one call to each index.
        // Most indexes will add first then remove as newdata and olddata may contain the same event.
        // Unique indexes may remove then add.
        for (int stream = 0; stream < newDataPerStream.length; stream++) {
            if (stream != streamNumber) {
                instrumentationCommon.qJoinCompositionStepUpdIndex(stream, newDataPerStream[stream], oldDataPerStream[stream]);
                for (int j = 0; j < repositories[stream].length; j++) {
                    repositories[stream][j].addRemove(newDataPerStream[stream], oldDataPerStream[stream], exprEvaluatorContext);
                }
                instrumentationCommon.aJoinCompositionStepUpdIndex();
            }
        }

        // join new data
        if (newDataPerStream[streamNumber] != null) {
            instrumentationCommon.qJoinCompositionQueryStrategy(true, streamNumber, newDataPerStream[streamNumber]);
            queryStrategy.lookup(newDataPerStream[streamNumber], newResults, exprEvaluatorContext);
            instrumentationCommon.aJoinCompositionQueryStrategy();
        }

        // on self-joins there can be repositories which are temporary for join execution
        if (isResetSelfJoinRepositories) {
            for (int i = 0; i < selfJoinRepositoryResets.length; i++) {
                if (!selfJoinRepositoryResets[i]) {
                    continue;
                }
                for (int j = 0; j < repositories[i].length; j++) {
                    repositories[i][j].clear();
                }
            }
        }

        exprEvaluatorContext.getInstrumentationProvider().aJoinCompositionStreamToWin(newResults);

        return new UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>(newResults, emptyResults);
    }

    public Set<MultiKeyArrayOfKeys<EventBean>> staticJoin() {
        throw new UnsupportedOperationException("Iteration over a unidirectional join is not supported");
    }

    public void accept(EventTableVisitor visitor) {
        visitor.visit(repositories);
    }
}
