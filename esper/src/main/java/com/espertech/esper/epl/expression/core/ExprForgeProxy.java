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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

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
                CodegenContext context = (CodegenContext) args[args.length - 1];
                CodegenParamSetExprPremade premade = CodegenParamSetExprPremade.INSTANCE;
                if (evaluationType == null) {
                    return forge.evaluateCodegen(premade, context);
                }
                CodegenBlock block = context.addMethod(evaluationType, ExprForgeProxy.class).add(CodegenParamSetExprPremade.INSTANCE).begin();
                String method;
                if (evaluationType == void.class) {
                    method = block.expression(forge.evaluateCodegen(premade, context))
                            .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                            .expression(staticMethod(AuditPath.class, "auditLog", constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRESSION"), constant(expressionToString)))
                            .blockEnd()
                            .methodEnd();
                } else {
                    method = block.declareVar(evaluationType, "result", forge.evaluateCodegen(premade, context))
                            .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                            .expression(staticMethod(AuditPath.class, "auditLog", constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRESSION"), op(constant(expressionToString + " result "), "+", ref("result"))))
                            .blockEnd()
                            .methodReturn(ref("result"));
                }
                return localMethodBuild(method).passAll(premade).call();
            }

            return m.invoke(forge, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}

