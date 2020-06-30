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
package com.espertech.esper.common.internal.epl.expression.dot.inner;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalRootChildInnerEval;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

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
        EPType type = forge.getRootForge().getEvaluationType();
        if (type == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        CodegenMethod methodNode = codegenMethodScope.makeChild(typeClass, InnerDotScalarUnpackEventEval.class, codegenClassScope);
        methodNode.getBlock()
            .declareVar(EPTypePremade.OBJECT.getEPType(), "target", forge.getRootForge().evaluateCodegen(EPTypePremade.OBJECT.getEPType(), methodNode, exprSymbol, codegenClassScope))
            .ifInstanceOf("target", EventBean.EPTYPE)
            .blockReturn(CodegenLegoCast.castSafeFromObjectType(typeClass, exprDotMethod(cast(EventBean.EPTYPE, ref("target")), "getUnderlying")))
            .methodReturn(CodegenLegoCast.castSafeFromObjectType(typeClass, ref("target")));
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
