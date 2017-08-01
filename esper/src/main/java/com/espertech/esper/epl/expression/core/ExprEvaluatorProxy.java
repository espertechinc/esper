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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

public class ExprEvaluatorProxy implements java.lang.reflect.InvocationHandler {

    private static Method targetEvaluate = JavaClassHelper.getMethodByName(ExprEvaluator.class, "evaluate");
    private static Method targetEvaluateCollEvents = JavaClassHelper.getMethodByName(ExprEnumerationEval.class, "evaluateGetROCollectionEvents");
    private static Method targetEvaluateCollScalar = JavaClassHelper.getMethodByName(ExprEnumerationEval.class, "evaluateGetROCollectionScalar");
    private static Method targetEvaluateBean = JavaClassHelper.getMethodByName(ExprEnumerationEval.class, "evaluateGetEventBean");

    private final String engineURI;
    private final String statementName;
    private final String expressionToString;
    private final ExprEvaluator evaluator;

    public static Object newInstance(String engineURI, String statementName, String expressionToString, ExprEvaluator evaluator) {
        return java.lang.reflect.Proxy.newProxyInstance(
                evaluator.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(evaluator.getClass()),
                new ExprEvaluatorProxy(engineURI, statementName, expressionToString, evaluator));
    }

    public ExprEvaluatorProxy(String engineURI, String statementName, String expressionToString, ExprEvaluator evaluator) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.expressionToString = expressionToString;
        this.evaluator = evaluator;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        try {
            if (m.equals(targetEvaluate)) {
                Object result = m.invoke(evaluator, args);
                if (AuditPath.isInfoEnabled()) {
                    AuditPath.auditLog(engineURI, statementName, AuditEnum.EXPRESSION, expressionToString + " result " + result);
                }
                return result;
            }

            if (m.equals(targetEvaluateCollEvents)) {
                Object result = m.invoke(evaluator, args);
                if (AuditPath.isInfoEnabled()) {
                    Collection<EventBean> resultBeans = (Collection<EventBean>) result;
                    String outStr = "null";
                    if (resultBeans != null) {
                        if (resultBeans.isEmpty()) {
                            outStr = "{}";
                        } else {
                            StringWriter buf = new StringWriter();
                            int count = 0;
                            for (EventBean theEvent : resultBeans) {
                                buf.append(" Event ");
                                buf.append(Integer.toString(count++));
                                buf.append(":");
                                EventBeanUtility.appendEvent(buf, theEvent);
                            }
                            outStr = buf.toString();
                        }
                    }
                    AuditPath.auditLog(engineURI, statementName, AuditEnum.EXPRESSION, expressionToString + " result " + outStr);
                }
                return result;
            }

            if (m.equals(targetEvaluateCollScalar)) {
                Object result = m.invoke(evaluator, args);
                if (AuditPath.isInfoEnabled()) {
                    AuditPath.auditLog(engineURI, statementName, AuditEnum.EXPRESSION, expressionToString + " result " + result);
                }
                return result;
            }

            if (m.equals(targetEvaluateBean)) {
                Object result = m.invoke(evaluator, args);
                if (AuditPath.isInfoEnabled()) {
                    String outStr = "null";
                    if (result != null) {
                        StringWriter buf = new StringWriter();
                        EventBeanUtility.appendEvent(buf, (EventBean) result);
                        outStr = buf.toString();
                    }
                    AuditPath.auditLog(engineURI, statementName, AuditEnum.EXPRESSION, expressionToString + " result " + outStr);
                }
                return result;
            }
            return m.invoke(evaluator, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}

