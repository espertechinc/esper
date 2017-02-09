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
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategyNoIndex;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.view.HistoricalEventViewable;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinSetComposerPrototypeHistorical2StreamImpl implements JoinSetComposerPrototype {

    private static final Logger log = LoggerFactory.getLogger(JoinSetComposerPrototypeFactory.class);

    private final ExprNode optionalFilterNode;
    private final EventType[] streamTypes;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final int polledViewNum;
    private final int streamViewNum;
    private final boolean isOuterJoin;
    private final ExprNode outerJoinEqualsNode;
    private final Pair<HistoricalIndexLookupStrategy, PollResultIndexingStrategy> indexStrategies;
    private final boolean isAllHistoricalNoSubordinate;
    private final OuterJoinDesc[] outerJoinDescList;
    private final boolean allowIndexInit;

    public JoinSetComposerPrototypeHistorical2StreamImpl(ExprNode optionalFilterNode, EventType[] streamTypes, ExprEvaluatorContext exprEvaluatorContext, int polledViewNum, int streamViewNum, boolean outerJoin, ExprNode outerJoinEqualsNode, Pair<HistoricalIndexLookupStrategy, PollResultIndexingStrategy> indexStrategies, boolean allHistoricalNoSubordinate, OuterJoinDesc[] outerJoinDescList, boolean allowIndexInit) {
        this.optionalFilterNode = optionalFilterNode;
        this.streamTypes = streamTypes;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.polledViewNum = polledViewNum;
        this.streamViewNum = streamViewNum;
        isOuterJoin = outerJoin;
        this.outerJoinEqualsNode = outerJoinEqualsNode;
        this.indexStrategies = indexStrategies;
        isAllHistoricalNoSubordinate = allHistoricalNoSubordinate;
        this.outerJoinDescList = outerJoinDescList;
        this.allowIndexInit = allowIndexInit;
    }

    public JoinSetComposerDesc create(Viewable[] streamViews, boolean isFireAndForget, AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        QueryStrategy[] queryStrategies = new QueryStrategy[streamTypes.length];

        HistoricalEventViewable viewable = (HistoricalEventViewable) streamViews[polledViewNum];
        ExprEvaluator outerJoinEqualsNodeEval = outerJoinEqualsNode == null ? null : outerJoinEqualsNode.getExprEvaluator();
        queryStrategies[streamViewNum] = new HistoricalDataQueryStrategy(streamViewNum, polledViewNum, viewable, isOuterJoin, outerJoinEqualsNodeEval,
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
            queryStrategies[polledViewNum] = new HistoricalDataQueryStrategy(polledViewNum, streamViewNum, viewable, isOuterJoin, outerJoinEqualsNodeEval,
                    new HistoricalIndexLookupStrategyNoIndex(), new PollResultIndexingStrategyNoIndex());
        }

        JoinSetComposer composer = new JoinSetComposerHistoricalImpl(allowIndexInit, null, queryStrategies, streamViews, exprEvaluatorContext);
        ExprEvaluator postJoinEval = optionalFilterNode == null ? null : optionalFilterNode.getExprEvaluator();
        return new JoinSetComposerDesc(composer, postJoinEval);
    }
}
