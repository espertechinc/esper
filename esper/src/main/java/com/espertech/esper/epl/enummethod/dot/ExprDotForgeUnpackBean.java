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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.expression.dot.ExprDotForge;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeUnpackBean implements ExprDotForge, ExprDotEval {

    private final EPType returnType;

    public ExprDotForgeUnpackBean(EventType lambdaType) {
        returnType = EPTypeHelper.singleValue(lambdaType.getUnderlyingType());
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        EventBean theEvent = (EventBean) target;
        return theEvent.getUnderlying();
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenContext context, CodegenParamSetExprPremade params) {
        Class resultType = EPTypeHelper.getCodegenReturnType(returnType);
        String method = context.addMethod(resultType, ExprDotForgeUnpackBean.class).add(innerType, "target").add(params).begin()
                .ifRefNullReturnNull("target")
                .methodReturn(cast(resultType, exprDotUnderlying(cast(EventBean.class, ref("target")))));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public EPType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitUnderlyingEvent();
    }

    public ExprDotEval getDotEvaluator() {
        return this;
    }

    public ExprDotForge getDotForge() {
        return this;
    }
}
