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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.rettype.EPChainableTypeCodegenSharable;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class ExprDotNodeForgeStreamEvalMethod implements ExprEvaluator {
    private final ExprDotNodeForgeStream forge;
    private final ExprDotEval[] evaluators;

    public ExprDotNodeForgeStreamEvalMethod(ExprDotNodeForgeStream forge, ExprDotEval[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // get underlying event
        EventBean event = eventsPerStream[forge.getStreamNumber()];
        if (event == null) {
            return null;
        }
        Object inner = event.getUnderlying();

        inner = ExprDotNodeUtility.evaluateChain(forge.getEvaluators(), evaluators, inner, eventsPerStream, isNewData, exprEvaluatorContext);
        return inner;
    }

    public static CodegenExpression codegen(ExprDotNodeForgeStream forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        EPTypeClass evaluationType = forge.getEvaluationType();
        EPTypeClass eventUndType = forge.getEventType().getUnderlyingEPType();
        CodegenMethod methodNode = codegenMethodScope.makeChild(evaluationType, ExprDotNodeForgeStreamEvalMethod.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        CodegenBlock block = methodNode.getBlock()
                .apply(instblock(codegenClassScope, "qExprStreamUndMethod", constant(ExprNodeUtilityPrint.toExpressionStringMinPrecedence(forge))))
                .declareVar(EventBean.EPTYPE, "event", arrayAtIndex(refEPS, constant(forge.getStreamNumber())));
        if (JavaClassHelper.isTypeVoid(evaluationType)) {
            block.ifCondition(equalsNull(ref("event")))
                    .apply(instblock(codegenClassScope, "aExprStreamUndMethod", constantNull()))
                    .blockReturnNoValue();
        } else {
            block.ifRefNull("event")
                    .apply(instblock(codegenClassScope, "aExprStreamUndMethod", constantNull()))
                    .blockReturn(constantNull());
        }

        CodegenExpression typeInformation = constantNull();
        if (codegenClassScope.isInstrumented()) {
            typeInformation = codegenClassScope.addOrGetFieldSharable(new EPChainableTypeCodegenSharable(EPChainableTypeHelper.singleValue(forge.getEventType().getUnderlyingEPType()), codegenClassScope));
        }

        block.declareVar(eventUndType, "inner", cast(eventUndType, exprDotMethod(ref("event"), "getUnderlying")))
                .apply(instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("inner"), constant(forge.getEvaluators().length)));
        CodegenExpression invoke = ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("inner"), eventUndType, forge.getEvaluators(), null);
        if (JavaClassHelper.isTypeVoid(evaluationType)) {
            block.expression(invoke)
                    .apply(instblock(codegenClassScope, "aExprDotChain"))
                    .apply(instblock(codegenClassScope, "aExprStreamUndMethod", constantNull()))
                    .methodEnd();
        } else {
            block.declareVar(evaluationType, "result", invoke)
                    .apply(instblock(codegenClassScope, "aExprDotChain"))
                    .apply(instblock(codegenClassScope, "aExprStreamUndMethod", ref("result")))
                    .methodReturn(ref("result"));
        }
        return localMethod(methodNode);
    }
}
