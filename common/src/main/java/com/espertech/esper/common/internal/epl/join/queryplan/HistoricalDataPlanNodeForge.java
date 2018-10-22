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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyForge;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanNodeForgeVisitor;
import com.espertech.esper.common.internal.util.IndentWriter;

import java.util.HashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Query plan for performing a historical data lookup.
 * <p>
 * Translates into a particular execution for use in regular and outer joins.
 */
public class HistoricalDataPlanNodeForge extends QueryPlanNodeForge {
    private final int streamNum;
    private final int rootStreamNum;
    private final int lookupStreamNum;
    private final int numStreams;
    private final ExprForge outerJoinExprEval;
    private PollResultIndexingStrategyForge pollResultIndexingStrategy;
    private HistoricalIndexLookupStrategyForge historicalIndexLookupStrategy;

    /**
     * Ctor.
     *
     * @param streamNum         the historical stream num
     * @param rootStreamNum     the stream number of the query plan providing incoming events
     * @param lookupStreamNum   the stream that provides polling/lookup events
     * @param numStreams        number of streams in join
     * @param outerJoinExprEval outer join expression node or null if none defined
     */
    public HistoricalDataPlanNodeForge(int streamNum, int rootStreamNum, int lookupStreamNum, int numStreams, ExprForge outerJoinExprEval) {
        this.streamNum = streamNum;
        this.rootStreamNum = rootStreamNum;
        this.lookupStreamNum = lookupStreamNum;
        this.numStreams = numStreams;
        this.outerJoinExprEval = outerJoinExprEval;
    }

    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        // none to add
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalDataPlanNode.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(HistoricalDataPlanNode.class, "node", newInstance(HistoricalDataPlanNode.class))
                .exprDotMethod(ref("node"), "setStreamNum", constant(streamNum))
                .exprDotMethod(ref("node"), "setNumStreams", constant(numStreams))
                .exprDotMethod(ref("node"), "setIndexingStrategy", pollResultIndexingStrategy.make(method, symbols, classScope))
                .exprDotMethod(ref("node"), "setLookupStrategy", historicalIndexLookupStrategy.make(method, symbols, classScope))
                .exprDotMethod(ref("node"), "setRootStreamNum", constant(rootStreamNum))
                .exprDotMethod(ref("node"), "setOuterJoinExprEval", outerJoinExprEval == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(outerJoinExprEval, method, this.getClass(), classScope))
                .methodReturn(ref("node"));
        return localMethod(method);
    }

    public void setPollResultIndexingStrategy(PollResultIndexingStrategyForge pollResultIndexingStrategy) {
        this.pollResultIndexingStrategy = pollResultIndexingStrategy;
    }

    public void setHistoricalIndexLookupStrategy(HistoricalIndexLookupStrategyForge historicalIndexLookupStrategy) {
        this.historicalIndexLookupStrategy = historicalIndexLookupStrategy;
    }

    protected void print(IndentWriter writer) {
        writer.incrIndent();
        writer.println("HistoricalDataPlanNode streamNum=" + streamNum);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public int getRootStreamNum() {
        return rootStreamNum;
    }

    public int getLookupStreamNum() {
        return lookupStreamNum;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public void accept(QueryPlanNodeForgeVisitor visitor) {
        visitor.visit(this);
    }
}
