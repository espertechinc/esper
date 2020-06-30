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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgePropertyFragment implements ExprDotEval, ExprDotForge {

    private final EventPropertyGetterSPI getter;
    private final EPChainableType returnType;

    public ExprDotForgePropertyFragment(EventPropertyGetterSPI getter, EPChainableType returnType) {
        this.getter = getter;
        this.returnType = returnType;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (!(target instanceof EventBean)) {
            return null;
        }
        return getter.getFragment((EventBean) target);
    }

    public EPChainableType getTypeInfo() {
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

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        EPTypeClass type = EPChainableTypeHelper.getCodegenReturnType(returnType);
        if (innerType.getType() == EventBean.class) {
            return CodegenLegoCast.castSafeFromObjectType(type, getter.eventBeanFragmentCodegen(inner, parent, classScope));
        }
        CodegenMethod methodNode = parent.makeChild(type, ExprDotForgePropertyFragment.class, classScope).addParam(innerType, "target");

        methodNode.getBlock()
                .ifInstanceOf("target", EventBean.EPTYPE)
                .blockReturn(CodegenLegoCast.castSafeFromObjectType(type, getter.eventBeanFragmentCodegen(cast(EventBean.EPTYPE, inner), methodNode, classScope)))
                .methodReturn(constantNull());
        return localMethod(methodNode, inner);
    }
}
