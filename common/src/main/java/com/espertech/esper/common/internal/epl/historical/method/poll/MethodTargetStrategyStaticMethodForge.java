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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class MethodTargetStrategyStaticMethodForge implements MethodTargetStrategyForge {
    private final Class clazz;
    private final Method reflectionMethod;

    public MethodTargetStrategyStaticMethodForge(Class clazz, Method reflectionMethod) {
        this.clazz = clazz;
        this.reflectionMethod = reflectionMethod;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(MethodTargetStrategyStaticMethod.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(MethodTargetStrategyStaticMethod.class, "target", newInstance(MethodTargetStrategyStaticMethod.class))
                .exprDotMethod(ref("target"), "setClazz", constant(clazz))
                .exprDotMethod(ref("target"), "setMethodName", constant(reflectionMethod.getName()))
                .exprDotMethod(ref("target"), "setMethodParameters", constant(reflectionMethod.getParameterTypes()))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref("target")))
                .methodReturn(ref("target"));
        return localMethod(method);
    }
}
