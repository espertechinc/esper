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
package com.espertech.esper.common.internal.epl.script.mvel;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;
import com.espertech.esper.common.internal.epl.script.core.ScriptEvaluatorBase;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ScriptEvaluatorMVEL extends ScriptEvaluatorBase {
    private static final Logger log = LoggerFactory.getLogger(ScriptEvaluatorMVEL.class);

    private final Object executable;

    public ScriptEvaluatorMVEL(String scriptName, String[] parameterNames, ExprEvaluator[] parameters, SimpleNumberCoercer coercer, Object executable) {
        super(scriptName, parameterNames, parameters, coercer);
        this.executable = executable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> paramsList = getMVELScriptParamsList(context);
        for (int i = 0; i < parameterNames.length; i++) {
            paramsList.put(parameterNames[i], parameters[i].evaluate(eventsPerStream, isNewData, context));
        }
        return evaluateInternal(paramsList, context);
    }

    public Object evaluate(Object lookupValues, ExprEvaluatorContext context) {
        Map<String, Object> paramsList = getMVELScriptParamsList(context);
        if (parameterNames.length == 1) {
            paramsList.put(parameterNames[0], lookupValues);
        } else if (parameterNames.length > 1) {
            Object[] mk = (Object[]) lookupValues;
            for (int i = 0; i < parameterNames.length; i++) {
                paramsList.put(parameterNames[i], mk[i]);
            }
        }
        return evaluateInternal(paramsList, context);
    }

    private static Map<String, Object> getMVELScriptParamsList(ExprEvaluatorContext context) {
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put(ExprNodeScript.CONTEXT_BINDING_NAME, context.getAllocateAgentInstanceScriptContext());
        return paramsList;
    }

    private Object evaluateInternal(Map<String, Object> paramsList, ExprEvaluatorContext context) {
        try {
            Object result = MVELInvoker.executeExpression(executable, paramsList);

            if (coercer != null) {
                return coercer.coerceBoxed((Number) result);
            }

            return result;
        } catch (InvocationTargetException ex) {
            Throwable mvelException = ex.getCause();
            String message = "Unexpected exception executing script '" + scriptName + "' for statement '" + context.getStatementName() + "' : " + mvelException.getMessage();
            log.error(message, mvelException);
            throw new EPException(message, ex);
        }
    }
}
