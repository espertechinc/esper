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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.plan.TableLookupIndexReqKey;
import com.espertech.esper.epl.join.table.EventTable;

import java.util.Map;
import java.util.Set;

/**
 * Implements the function to determine a join result set using tables/indexes and query strategy
 * instances for each stream.
 */
public class JoinSetComposerFAFImpl extends JoinSetComposerImpl {
    private final boolean isOuterJoins;

    public JoinSetComposerFAFImpl(Map<TableLookupIndexReqKey, EventTable>[] repositories, QueryStrategy[] queryStrategies, boolean isPureSelfJoin, ExprEvaluatorContext exprEvaluatorContext, boolean joinRemoveStream, boolean outerJoins) {
        super(false, repositories, queryStrategies, isPureSelfJoin, exprEvaluatorContext, joinRemoveStream);
        isOuterJoins = outerJoins;
    }

    @Override
    public void init(EventBean[][] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // no action
    }

    @Override
    public void destroy() {
        // no action
    }

    @Override
    public UniformPair<Set<MultiKey<EventBean>>> join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        newResults.clear();

        // We add and remove data in one call to each index.
        // Most indexes will add first then remove as newdata and olddata may contain the same event.
        // Unique indexes may remove then add.
        for (int stream = 0; stream < newDataPerStream.length; stream++) {
            for (int j = 0; j < repositories[stream].length; j++) {
                repositories[stream][j].addRemove(newDataPerStream[stream], oldDataPerStream[stream], exprEvaluatorContext);
            }
        }

        // for outer joins, execute each query strategy
        if (isOuterJoins) {
            for (int i = 0; i < newDataPerStream.length; i++) {
                if (newDataPerStream[i] != null) {
                    queryStrategies[i].lookup(newDataPerStream[i], newResults, exprEvaluatorContext);
                }
            }
        } else {
            // handle all-inner joins by executing the smallest number of event's query strategy
            int minStream = -1;
            int minStreamCount = -1;
            for (int i = 0; i < newDataPerStream.length; i++) {
                if (newDataPerStream[i] != null) {
                    if (newDataPerStream[i].length == 0) {
                        minStream = -1;
                        break;
                    }
                    if (newDataPerStream[i].length > minStreamCount) {
                        minStream = i;
                        minStreamCount = newDataPerStream[i].length;
                    }
                }
            }
            if (minStream != -1) {
                queryStrategies[minStream].lookup(newDataPerStream[minStream], newResults, exprEvaluatorContext);
            }
        }

        return new UniformPair<Set<MultiKey<EventBean>>>(newResults, oldResults);
    }

    @Override
    public Set<MultiKey<EventBean>> staticJoin() {
        // no action
        return null;
    }
}
