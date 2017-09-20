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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.util.AuditPath.METHOD_AUDITLOG;

public class ExprForgeProxy implements java.lang.reflect.InvocationHandler {

    private static final Method TARGET_GETEXPREVALUATOR;
    private static final Method TARGET_EVALUATECODEGEN;

    static {
        TARGET_GETEXPREVALUATOR = JavaClassHelper.getMethodByName(ExprForge.class, "getExprEvaluator");
        TARGET_EVALUATECODEGEN = JavaClassHelper.getMethodByName(ExprForge.class, "evaluateCodegen");
        if (TARGET_EVALUATECODEGEN == null || TARGET_GETEXPREVALUATOR == null) {
            throw new RuntimeException("Failed to find required methods");
        }
    }

    private final String engineURI;
    private final String statementName;
    private final String expressionToString;
    private final ExprForge forge;

    public static Object newInstance(String engineURI, String statementName, String expressionToString, ExprForge forge) {
        return java.lang.reflect.Proxy.newProxyInstance(
                forge.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(forge.getClass()),
                new ExprForgeProxy(engineURI, statementName, expressionToString, forge));
    }

    public ExprForgeProxy(String engineURI, String statementName, String expressionToString, ExprForge forge) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.expressionToString = expressionToString;
        this.forge = forge;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        try {
            if (m.equals(TARGET_GETEXPREVALUATOR)) {
                ExprEvaluator evaluator = (ExprEvaluator) m.invoke(forge, args);
                return ExprEvaluatorProxy.newInstance(engineURI, statementName, expressionToString, evaluator);
            }

            if (m.equals(TARGET_EVALUATECODEGEN)) {
                Class evaluationType = forge.getEvaluationType();
                Class requiredType = (Class) args[args.length - 4];
                CodegenMethodNode parentMethod = (CodegenMethodNode) args[args.length - 3];
                ExprForgeCodegenSymbol exprSymbol = (ExprForgeCodegenSymbol) args[args.length - 2];
                CodegenClassScope codegenClassScope = (CodegenClassScope) args[args.length - 1];
                if (evaluationType == null) {
                    return forge.evaluateCodegen(requiredType, parentMethod, exprSymbol, codegenClassScope);
                }
                CodegenMethodNode methodNode = parentMethod.makeChild(evaluationType, ExprForgeProxy.class, codegenClassScope);
                CodegenBlock block = methodNode.getBlock();
                if (evaluationType == void.class) {
                    block.expression(forge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                            .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                            .staticMethod(AuditPath.class, METHOD_AUDITLOG, constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRESSION"), constant(expressionToString))
                            .blockEnd()
                            .methodEnd();
                } else {
                    block.declareVar(evaluationType, "result", forge.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope))
                            .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                            .staticMethod(AuditPath.class, METHOD_AUDITLOG, constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRESSION"), op(constant(expressionToString + " result "), "+", ref("result")))
                            .blockEnd()
                            .methodReturn(ref("result"));
                }
                return localMethod(methodNode);
            }

            return m.invoke(forge, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}

