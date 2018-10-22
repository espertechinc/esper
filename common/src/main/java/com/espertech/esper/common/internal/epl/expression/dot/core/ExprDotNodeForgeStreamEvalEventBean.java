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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.rettype.EPTypeCodegenSharable;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeForgeStreamEvalEventBean implements ExprEvaluator {
    private final ExprDotNodeForgeStream forge;
    private final ExprDotEval[] evaluators;

    public ExprDotNodeForgeStreamEvalEventBean(ExprDotNodeForgeStream forge, ExprDotEval[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[forge.getStreamNumber()];
        if (theEvent == null) {
            return null;
        }
        return ExprDotNodeUtility.evaluateChain(forge.getEvaluators(), evaluators, theEvent, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(ExprDotNodeForgeStream forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprDotNodeForgeStreamEvalEventBean.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        CodegenExpression typeInformation = constantNull();
        if (codegenClassScope.isInstrumented()) {
            typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(EPTypeHelper.singleEvent(forge.getEventType()), codegenClassScope));
        }

        methodNode.getBlock()
                .declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(forge.getStreamNumber())))
                .apply(InstrumentationCode.instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("event"), constant(forge.getEvaluators().length)))
                .ifRefNull("event")
                .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                .blockReturn(constantNull())
                .declareVar(forge.getEvaluationType(), "result", ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("event"), EventBean.class, forge.getEvaluators(), null))
                .apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                .methodReturn(ref("result"));
        return localMethod(methodNode);
    }
}
