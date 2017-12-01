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
package com.espertech.esper.epl.expression.dot.inner;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEvalRootChildInnerEval;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class InnerDotScalarUnpackEventEval implements ExprDotEvalRootChildInnerEval {

    private ExprEvaluator rootEvaluator;

    public InnerDotScalarUnpackEventEval(ExprEvaluator rootEvaluator) {
        this.rootEvaluator = rootEvaluator;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object target = rootEvaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (target instanceof EventBean) {
            return ((EventBean) target).getUnderlying();
        }
        return target;
    }

    public static CodegenExpression codegenEvaluate(InnerDotScalarUnpackEventForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.getRootForge().getEvaluationType(), InnerDotScalarUnpackEventEval.class, codegenClassScope);

        methodNode.getBlock()
                .declareVar(Object.class, "target", forge.getRootForge().evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                .ifInstanceOf("target", EventBean.class)
                .blockReturn(CodegenLegoCast.castSafeFromObjectType(forge.getRootForge().getEvaluationType(), exprDotMethod(cast(EventBean.class, ref("target")), "getUnderlying")))
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(forge.getRootForge().getEvaluationType(), ref("target")));
        return localMethod(methodNode);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

}
