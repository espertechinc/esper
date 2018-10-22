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
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JoinSetComposerPrototypeGeneralForge extends JoinSetComposerPrototypeForge {
    private final QueryPlanForge queryPlan;
    private final StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult;
    private final String[] streamNames;
    private final boolean joinRemoveStream;
    private final boolean hasHistorical;

    public JoinSetComposerPrototypeGeneralForge(EventType[] streamTypes, ExprNode postJoinEvaluator, boolean outerJoins, QueryPlanForge queryPlan, StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult, String[] streamNames, boolean joinRemoveStream, boolean hasHistorical) {
        super(streamTypes, postJoinEvaluator, outerJoins);
        this.queryPlan = queryPlan;
        this.streamJoinAnalysisResult = streamJoinAnalysisResult;
        this.streamNames = streamNames;
        this.joinRemoveStream = joinRemoveStream;
        this.hasHistorical = hasHistorical;
    }

    public QueryPlanForge getOptionalQueryPlan() {
        return queryPlan;
    }

    protected Class implementation() {
        return JoinSetComposerPrototypeGeneral.class;
    }

    protected void populateInline(CodegenExpression impl, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref("impl"), "setQueryPlan", queryPlan.make(method, symbols, classScope))
                .exprDotMethod(ref("impl"), "setStreamJoinAnalysisResult", streamJoinAnalysisResult.make(method, symbols, classScope))
                .exprDotMethod(ref("impl"), "setStreamNames", constant(streamNames))
                .exprDotMethod(ref("impl"), "setJoinRemoveStream", constant(joinRemoveStream))
                .exprDotMethod(ref("impl"), "setEventTableIndexService", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETEVENTTABLEINDEXSERVICE))
                .exprDotMethod(ref("impl"), "setHasHistorical", constant(hasHistorical));
    }
}
