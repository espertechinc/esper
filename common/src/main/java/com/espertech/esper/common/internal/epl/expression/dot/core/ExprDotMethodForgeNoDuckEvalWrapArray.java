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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

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
    public EPChainableType getTypeInfo() {
        EPTypeClass returnType = ClassHelperGenericType.getMethodReturnEPType(forge.getMethod());
        EPTypeClass componentType = JavaClassHelper.getArrayComponentType(returnType);
        return EPChainableTypeHelper.collectionOfSingleValue(componentType);
    }

    public static CodegenExpression codegenWrapArray(ExprDotMethodForgeNoDuck forge, CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.COLLECTION.getEPType(), ExprDotMethodForgeNoDuckEvalWrapArray.class, codegenClassScope).addParam(innerType, "target");

        EPTypeClass returnType = ClassHelperGenericType.getMethodReturnEPType(forge.getMethod());
        methodNode.getBlock()
                .declareVar(JavaClassHelper.getBoxedType(returnType), "array", ExprDotMethodForgeNoDuckEvalPlain.codegenPlain(forge, ref("target"), innerType, methodNode, exprSymbol, codegenClassScope))
                .methodReturn(CollectionUtil.arrayToCollectionAllowNullCodegen(methodNode, returnType, ref("array"), codegenClassScope));
        return localMethod(methodNode, inner);
    }
}
