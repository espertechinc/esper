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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JoinSetComposerPrototypeHistorical2StreamForge extends JoinSetComposerPrototypeForge {

    private final int polledNum;
    private final int streamNum;
    private final ExprNode outerJoinEqualsEval;
    private final HistoricalIndexLookupStrategyForge historicalIndexLookupStrategy;
    private final PollResultIndexingStrategyForge pollResultIndexingStrategy;
    private final boolean isAllHistoricalNoSubordinate;
    private final boolean[] outerJoinPerStream;

    public JoinSetComposerPrototypeHistorical2StreamForge(EventType[] streamTypes, ExprNode postJoinEvaluator, boolean outerJoins, int polledNum, int streamNum, ExprNode outerJoinEqualsEval, HistoricalIndexLookupStrategyForge historicalIndexLookupStrategy, PollResultIndexingStrategyForge pollResultIndexingStrategy, boolean isAllHistoricalNoSubordinate, boolean[] outerJoinPerStream) {
        super(streamTypes, postJoinEvaluator, outerJoins);
        this.polledNum = polledNum;
        this.streamNum = streamNum;
        this.outerJoinEqualsEval = outerJoinEqualsEval;
        this.historicalIndexLookupStrategy = historicalIndexLookupStrategy;
        this.pollResultIndexingStrategy = pollResultIndexingStrategy;
        this.isAllHistoricalNoSubordinate = isAllHistoricalNoSubordinate;
        this.outerJoinPerStream = outerJoinPerStream;
    }

    protected Class implementation() {
        return JoinSetComposerPrototypeHistorical2Stream.class;
    }

    protected void populateInline(CodegenExpression impl, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref("impl"), "setPolledNum", constant(polledNum))
                .exprDotMethod(ref("impl"), "setStreamNum", constant(streamNum))
                .exprDotMethod(ref("impl"), "setOuterJoinEqualsEval", outerJoinEqualsEval == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(outerJoinEqualsEval.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(ref("impl"), "setLookupStrategy", historicalIndexLookupStrategy.make(method, symbols, classScope))
                .exprDotMethod(ref("impl"), "setIndexingStrategy", pollResultIndexingStrategy.make(method, symbols, classScope))
                .exprDotMethod(ref("impl"), "setAllHistoricalNoSubordinate", constant(isAllHistoricalNoSubordinate))
                .exprDotMethod(ref("impl"), "setOuterJoinPerStream", constant(outerJoinPerStream));
    }

    public QueryPlanForge getOptionalQueryPlan() {
        return null;
    }
}
