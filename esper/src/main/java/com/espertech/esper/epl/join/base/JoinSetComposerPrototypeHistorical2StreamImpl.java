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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategyNoIndex;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.view.HistoricalEventViewable;
import com.espertech.esper.view.Viewable;

public class JoinSetComposerPrototypeHistorical2StreamImpl implements JoinSetComposerPrototype {

    private final ExprEvaluator optionalFilterEval;
    private final EventType[] streamTypes;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final int polledViewNum;
    private final int streamViewNum;
    private final boolean isOuterJoin;
    private final ExprEvaluator outerJoinEqualsEval;
    private final Pair<HistoricalIndexLookupStrategy, PollResultIndexingStrategy> indexStrategies;
    private final boolean isAllHistoricalNoSubordinate;
    private final OuterJoinDesc[] outerJoinDescList;
    private final boolean allowIndexInit;

    public JoinSetComposerPrototypeHistorical2StreamImpl(ExprEvaluator optionalFilterEval, EventType[] streamTypes, ExprEvaluatorContext exprEvaluatorContext, int polledViewNum, int streamViewNum, boolean outerJoin, ExprEvaluator outerJoinEqualsEval, Pair<HistoricalIndexLookupStrategy, PollResultIndexingStrategy> indexStrategies, boolean allHistoricalNoSubordinate, OuterJoinDesc[] outerJoinDescList, boolean allowIndexInit) {
        this.optionalFilterEval = optionalFilterEval;
        this.streamTypes = streamTypes;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.polledViewNum = polledViewNum;
        this.streamViewNum = streamViewNum;
        isOuterJoin = outerJoin;
        this.outerJoinEqualsEval = outerJoinEqualsEval;
        this.indexStrategies = indexStrategies;
        isAllHistoricalNoSubordinate = allHistoricalNoSubordinate;
        this.outerJoinDescList = outerJoinDescList;
        this.allowIndexInit = allowIndexInit;
    }

    public JoinSetComposerDesc create(Viewable[] streamViews, boolean isFireAndForget, AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        QueryStrategy[] queryStrategies = new QueryStrategy[streamTypes.length];

        HistoricalEventViewable viewable = (HistoricalEventViewable) streamViews[polledViewNum];
        queryStrategies[streamViewNum] = new HistoricalDataQueryStrategy(streamViewNum, polledViewNum, viewable, isOuterJoin, outerJoinEqualsEval,
                indexStrategies.getFirst(), indexStrategies.getSecond());

        // for strictly historical joins, create a query strategy for the non-subordinate historical view
        if (isAllHistoricalNoSubordinate) {
            boolean isOuterJoin = false;
            if (outerJoinDescList.length > 0) {
                OuterJoinDesc outerJoinDesc = outerJoinDescList[0];
                if (outerJoinDesc.getOuterJoinType().equals(OuterJoinType.FULL)) {
                    isOuterJoin = true;
                } else if ((outerJoinDesc.getOuterJoinType().equals(OuterJoinType.LEFT)) &&
                        (polledViewNum == 0)) {
                    isOuterJoin = true;
                } else if ((outerJoinDesc.getOuterJoinType().equals(OuterJoinType.RIGHT)) &&
                        (polledViewNum == 1)) {
                    isOuterJoin = true;
                }
            }
            viewable = (HistoricalEventViewable) streamViews[streamViewNum];
            queryStrategies[polledViewNum] = new HistoricalDataQueryStrategy(polledViewNum, streamViewNum, viewable, isOuterJoin, outerJoinEqualsEval,
                    new HistoricalIndexLookupStrategyNoIndex(), new PollResultIndexingStrategyNoIndex());
        }

        JoinSetComposer composer = new JoinSetComposerHistoricalImpl(allowIndexInit, null, queryStrategies, streamViews, exprEvaluatorContext);
        return new JoinSetComposerDesc(composer, optionalFilterEval);
    }
}
