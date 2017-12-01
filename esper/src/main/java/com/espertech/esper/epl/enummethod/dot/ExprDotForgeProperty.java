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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.dot.ExprDotEval;
import com.espertech.esper.epl.expression.dot.ExprDotEvalVisitor;
import com.espertech.esper.epl.expression.dot.ExprDotForge;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeProperty implements ExprDotEval, ExprDotForge {

    private final EventPropertyGetterSPI getter;
    private final EPType returnType;

    public ExprDotForgeProperty(EventPropertyGetterSPI getter, EPType returnType) {
        this.getter = getter;
        this.returnType = returnType;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (!(target instanceof EventBean)) {
            return null;
        }
        return getter.get((EventBean) target);
    }

    public EPType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitPropertySource();
    }

    public ExprDotEval getDotEvaluator() {
        return this;
    }

    public ExprDotForge getDotForge() {
        return this;
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class type = EPTypeHelper.getCodegenReturnType(returnType);
        if (innerType == EventBean.class) {
            return CodegenLegoCast.castSafeFromObjectType(type, getter.eventBeanGetCodegen(inner, codegenMethodScope, codegenClassScope));
        }
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(type, ExprDotForgeProperty.class, codegenClassScope).addParam(innerType, "target");

        methodNode.getBlock()
                .ifInstanceOf("target", EventBean.class)
                .blockReturn(CodegenLegoCast.castSafeFromObjectType(type, getter.eventBeanGetCodegen(cast(EventBean.class, inner), methodNode, codegenClassScope)))
                .methodReturn(constantNull());
        return localMethod(methodNode, inner);
    }
}
