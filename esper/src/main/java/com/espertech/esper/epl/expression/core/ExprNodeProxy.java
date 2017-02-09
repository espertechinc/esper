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

import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Method;

public class ExprNodeProxy implements java.lang.reflect.InvocationHandler {

    private static Method target = JavaClassHelper.getMethodByName(ExprNode.class, "getExprEvaluator");

    private String engineURI;
    private String statementName;
    private ExprNode exprNode;

    public static Object newInstance(String engineURI, String statementName, ExprNode exprNode) {
        return java.lang.reflect.Proxy.newProxyInstance(
                exprNode.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(exprNode.getClass()),
                new ExprNodeProxy(engineURI, statementName, exprNode));
    }

    public ExprNodeProxy(String engineURI, String statementName, ExprNode exprNode) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.exprNode = exprNode;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        if (!m.equals(target)) {
            return m.invoke(exprNode, args);
        }

        String expressionToString = "undefined";
        try {
            expressionToString = ExprNodeUtility.toExpressionStringMinPrecedenceSafe(exprNode);
        } catch (RuntimeException ex) {
            // no action
        }

        ExprEvaluator evaluator = (ExprEvaluator) m.invoke(exprNode, args);
        return ExprEvaluatorProxy.newInstance(engineURI, statementName, expressionToString, evaluator);
    }
}

