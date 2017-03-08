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
package com.espertech.esper.epl.script;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.script.mvel.MVELInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ExprNodeScriptEvalMVEL extends ExprNodeScriptEvalBase implements ExprNodeScriptEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeScriptEvalMVEL.class);

    private final Object executable;

    public ExprNodeScriptEvalMVEL(String scriptName, String statementName, String[] names, ExprEvaluator[] parameters, Class returnType, EventType eventTypeCollection, Object executable) {
        super(scriptName, statementName, names, parameters, returnType, eventTypeCollection);
        this.executable = executable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> paramsList = getParamsList(context);
        for (int i = 0; i < names.length; i++) {
            paramsList.put(names[i], parameters[i].evaluate(eventsPerStream, isNewData, context));
        }
        return evaluateInternal(paramsList);
    }

    public Object evaluate(Object[] lookupValues, ExprEvaluatorContext context) {
        Map<String, Object> paramsList = getParamsList(context);
        for (int i = 0; i < names.length; i++) {
            paramsList.put(names[i], lookupValues[i]);
        }
        return evaluateInternal(paramsList);
    }

    private Map<String, Object> getParamsList(ExprEvaluatorContext context) {
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put(ExprNodeScript.CONTEXT_BINDING_NAME, context.getAllocateAgentInstanceScriptContext());
        return paramsList;
    }

    private Object evaluateInternal(Map<String, Object> paramsList) {
        try {
            Object result = MVELInvoker.executeExpression(executable, paramsList);

            if (coercer != null) {
                return coercer.coerceBoxed((Number) result);
            }

            return result;
        } catch (InvocationTargetException ex) {
            Throwable mvelException = ex.getCause();
            String message = "Unexpected exception executing script '" + scriptName + "' for statement '" + statementName + "' : " + mvelException.getMessage();
            log.error(message, mvelException);
            throw new EPException(message, ex);
        }
    }
}
