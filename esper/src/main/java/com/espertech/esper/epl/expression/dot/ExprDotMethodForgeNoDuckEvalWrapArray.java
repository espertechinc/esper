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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class ExprDotMethodForgeNoDuckEvalWrapArray extends ExprDotMethodForgeNoDuckEvalPlain {
    public ExprDotMethodForgeNoDuckEvalWrapArray(ExprDotMethodForgeNoDuck forge, ExprEvaluator[] parameters) {
        super(forge, parameters);
    }

    @Override
    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = super.evaluate(target, eventsPerStream, isNewData, exprEvaluatorContext);
        if (result == null || !result.getClass().isArray()) {
            return null;
        }
        return CollectionUtil.arrayToCollectionAllowNull(result);
    }

    @Override
    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfSingleValue(forge.getMethod().getReturnType().getComponentType());
    }

    public static CodegenExpression codegenWrapArray(ExprDotMethodForgeNoDuck forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Collection.class, ExprDotMethodForgeNoDuckEvalWrapArray.class, codegenClassScope).addParam(innerType, "target");

        Class returnType = forge.getMethod().getReturnType();
        methodNode.getBlock()
                .declareVar(JavaClassHelper.getBoxedType(returnType), "array", ExprDotMethodForgeNoDuckEvalPlain.codegenPlain(forge, ref("target"), innerType, methodNode, exprSymbol, codegenClassScope))
                .methodReturn(CollectionUtil.arrayToCollectionAllowNullCodegen(methodNode, returnType, ref("array"), codegenClassScope));
        return localMethod(methodNode, inner);
    }
}
