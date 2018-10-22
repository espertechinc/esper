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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprForgeProxy implements java.lang.reflect.InvocationHandler {

    private static final Method TARGET_EVALUATECODEGEN;

    static {
        TARGET_EVALUATECODEGEN = JavaClassHelper.getMethodByName(ExprForge.class, "evaluateCodegen");
        if (TARGET_EVALUATECODEGEN == null) {
            throw new RuntimeException("Failed to find required methods");
        }
    }

    private final String expressionToString;
    private final ExprForge forge;

    public static Object newInstance(String expressionToString, ExprForge forge) {
        return java.lang.reflect.Proxy.newProxyInstance(
                forge.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(forge.getClass()),
                new ExprForgeProxy(expressionToString, forge));
    }

    public ExprForgeProxy(String expressionToString, ExprForge forge) {
        this.expressionToString = expressionToString;
        this.forge = forge;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        try {
            if (m.equals(TARGET_EVALUATECODEGEN)) {
                Class evaluationType = forge.getEvaluationType();
                Class requiredType = (Class) args[args.length - 4];
                CodegenMethodScope parent = (CodegenMethodScope) args[args.length - 3];
                ExprForgeCodegenSymbol symbols = (ExprForgeCodegenSymbol) args[args.length - 2];
                CodegenClassScope codegenClassScope = (CodegenClassScope) args[args.length - 1];

                if (evaluationType == null) {
                    return forge.evaluateCodegen(requiredType, parent, symbols, codegenClassScope);
                }
                CodegenMethod method = parent.makeChild(evaluationType, ExprForgeProxy.class, codegenClassScope);
                if (evaluationType == void.class) {
                    method.getBlock().expression(forge.evaluateCodegen(requiredType, method, symbols, codegenClassScope))
                            .expression(exprDotMethodChain(symbols.getAddExprEvalCtx(method)).add("getAuditProvider").add("expression", constant(expressionToString), constant("(void)"), symbols.getAddExprEvalCtx(method)))
                            .methodEnd();
                } else {
                    method.getBlock().declareVar(evaluationType, "result", forge.evaluateCodegen(evaluationType, method, symbols, codegenClassScope))
                            .expression(exprDotMethodChain(symbols.getAddExprEvalCtx(method)).add("getAuditProvider").add("expression", constant(expressionToString), ref("result"), symbols.getAddExprEvalCtx(method)))
                            .methodReturn(ref("result"));
                }
                return localMethod(method);
            }

            return m.invoke(forge, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}

