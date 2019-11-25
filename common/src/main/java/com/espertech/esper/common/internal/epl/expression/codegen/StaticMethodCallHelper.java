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
package com.espertech.esper.common.internal.epl.expression.codegen;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeForgeStaticMethodEval;
import com.espertech.esper.common.internal.util.ValueAndFieldDesc;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeForgeStaticMethodEval.METHOD_STATICMETHODEVALHANDLEINVOCATIONEXCEPTION;

public class StaticMethodCallHelper {
    public static StaticMethodCodegenArgDesc[] allArgumentExpressions(ExprForge[] forges, Method method, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        StaticMethodCodegenArgDesc[] args = new StaticMethodCodegenArgDesc[forges.length];
        for (int i = 0; i < forges.length; i++) {
            ExprForge child = forges[i];
            Class childType = child.getEvaluationType();
            String name = "r" + i;
            if (childType == null) {
                args[i] = new StaticMethodCodegenArgDesc(name, method.getParameterTypes()[i], constantNull());
            } else {
                args[i] = new StaticMethodCodegenArgDesc(name, childType, child.evaluateCodegen(childType, codegenMethodScope, exprSymbol, codegenClassScope));
            }
        }
        return args;
    }

    public static void appendArgExpressions(StaticMethodCodegenArgDesc[] args, CodegenBlock block) {
        for (int i = 0; i < args.length; i++) {
            block.declareVar(args[i].getDeclareType(), args[i].getBlockRefName(), args[i].getArgExpression());
        }
    }

    public static void appendCatch(CodegenBlock tryBlock, Method reflectionMethod, String statementName, String classOrPropertyName, boolean rethrow, StaticMethodCodegenArgDesc[] args) {
        CodegenBlock catchBlock = tryBlock.tryEnd().addCatch(Throwable.class, "t")
                .declareVar(Object[].class, "argArray", newArrayByLength(Object.class, constant(args.length)));
        for (int i = 0; i < args.length; i++) {
            catchBlock.assignArrayElement("argArray", constant(i), ref(args[i].getBlockRefName()));
        }

        Class[] paramTypes = reflectionMethod.getParameterTypes();
        catchBlock.staticMethod(ExprDotNodeForgeStaticMethodEval.class, METHOD_STATICMETHODEVALHANDLEINVOCATIONEXCEPTION,
                constant(statementName), constant(reflectionMethod.getName()), constant(paramTypes), constant(classOrPropertyName), ref("argArray"), ref("t"), constant(rethrow));
    }

    public static CodegenExpression codegenInvokeExpression(ValueAndFieldDesc optionalTargetObject, Method reflectionMethod, StaticMethodCodegenArgDesc[] args, CodegenClassScope codegenClassScope) {
        CodegenExpression[] expressions = new CodegenExpression[args.length];
        for (int i = 0; i < expressions.length; i++) {
            expressions[i] = ref(args[i].getBlockRefName());
        }

        if (optionalTargetObject == null) {
            return staticMethod(reflectionMethod.getDeclaringClass(), reflectionMethod.getName(), expressions);
        } else {
            if (optionalTargetObject.getValue() != null && optionalTargetObject.getValue().getClass().isEnum()) {
                return exprDotMethod(enumValue(optionalTargetObject.getValue().getClass(), optionalTargetObject.getValue().toString()), reflectionMethod.getName(), expressions);
            } else {
                return exprDotMethod(publicConstValue(optionalTargetObject.getField().getDeclaringClass(), optionalTargetObject.getField().getName()), reflectionMethod.getName(), expressions);
            }
        }
    }
}
