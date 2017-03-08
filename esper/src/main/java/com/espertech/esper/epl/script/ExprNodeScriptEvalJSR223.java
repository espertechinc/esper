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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;

public class ExprNodeScriptEvalJSR223 extends ExprNodeScriptEvalBase implements ExprNodeScriptEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeScriptEvalJSR223.class);

    private final CompiledScript executable;

    public ExprNodeScriptEvalJSR223(String scriptName, String statementName, String[] names, ExprEvaluator[] parameters, Class returnType, EventType eventTypeCollection, CompiledScript executable) {
        super(scriptName, statementName, names, parameters, returnType, eventTypeCollection);
        this.executable = executable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Bindings bindings = getBindings(context);
        for (int i = 0; i < names.length; i++) {
            bindings.put(names[i], parameters[i].evaluate(eventsPerStream, isNewData, context));
        }
        return evaluateInternal(bindings);
    }

    public Object evaluate(Object[] lookupValues, ExprEvaluatorContext context) {
        Bindings bindings = getBindings(context);
        for (int i = 0; i < names.length; i++) {
            bindings.put(names[i], lookupValues[i]);
        }
        return evaluateInternal(bindings);
    }

    private Bindings getBindings(ExprEvaluatorContext context) {
        Bindings bindings = executable.getEngine().createBindings();
        bindings.put(ExprNodeScript.CONTEXT_BINDING_NAME, context.getAllocateAgentInstanceScriptContext());
        return bindings;
    }

    private Object evaluateInternal(Bindings bindings) {
        try {
            Object result = executable.eval(bindings);

            if (coercer != null) {
                return coercer.coerceBoxed((Number) result);
            }

            return result;
        } catch (ScriptException e) {
            String message = "Unexpected exception executing script '" + scriptName + "' for statement '" + statementName + "' : " + e.getMessage();
            log.error(message, e);
            throw new EPException(message, e);
        }
    }
}
