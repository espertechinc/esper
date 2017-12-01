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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ExprNodeProxy implements java.lang.reflect.InvocationHandler {

    private static final Method TARGET_GETFORGE;
    private static final Method TARGET_EQUALSNODE;

    static {
        TARGET_GETFORGE = JavaClassHelper.getMethodByName(ExprNode.class, "getForge");
        TARGET_EQUALSNODE = JavaClassHelper.getMethodByName(ExprNode.class, "equalsNode");
        if (TARGET_GETFORGE == null || TARGET_EQUALSNODE == null) {
            throw new RuntimeException("Failed to find required methods");
        }
    }

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

        try {
            if (m.equals(TARGET_GETFORGE)) {
                return handleGetForge(m, args);
            }

            if (m.equals(TARGET_EQUALSNODE)) {
                return handleEqualsNode(m, args);
            }

            return m.invoke(exprNode, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public ExprNode getOriginalObject() {
        return exprNode;
    }

    private Object handleEqualsNode(Method m, Object[] args) {
        ExprNode otherNode;
        try {
            otherNode = ((ExprNodeProxy) Proxy.getInvocationHandler(args[0])).getOriginalObject();
        } catch (IllegalArgumentException ex) {
            otherNode = (ExprNode) args[0];
        }
        return exprNode.equalsNode(otherNode, (boolean) args[1]);
    }

    private Object handleGetForge(Method m, Object[] args) throws Exception {
        String expressionToString = "undefined";
        try {
            expressionToString = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(exprNode);
        } catch (RuntimeException ex) {
            // no action
        }

        ExprForge forge = (ExprForge) m.invoke(exprNode, args);
        return ExprForgeProxy.newInstance(engineURI, statementName, expressionToString, forge);
    }
}

