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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
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

    public static CodegenExpression codegenWrapArray(ExprDotMethodForgeNoDuck forge, CodegenExpression inner, Class innerType, CodegenContext context, CodegenParamSetExprPremade params) {
        Class returnType = forge.getMethod().getReturnType();
        CodegenMethodId method = context.addMethod(Collection.class, ExprDotMethodForgeNoDuckEvalWrapArray.class).add(innerType, "target").add(params).begin()
                .declareVar(JavaClassHelper.getBoxedType(returnType), "array", ExprDotMethodForgeNoDuckEvalPlain.codegenPlain(forge, ref("target"), innerType, context, params))
                .methodReturn(CollectionUtil.arrayToCollectionAllowNullCodegen(returnType, ref("array"), context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
