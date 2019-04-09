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
package com.espertech.esper.common.internal.epl.script.jsr223;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;
import com.espertech.esper.common.internal.epl.script.core.ScriptEvaluatorBase;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;

public class ScriptEvaluatorJSR223 extends ScriptEvaluatorBase {

    private static final Logger log = LoggerFactory.getLogger(ScriptEvaluatorJSR223.class);

    private final CompiledScript executable;

    public ScriptEvaluatorJSR223(String scriptName, String[] parameterNames, ExprEvaluator[] parameters, SimpleNumberCoercer coercer, CompiledScript executable) {
        super(scriptName, parameterNames, parameters, coercer);
        this.executable = executable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Bindings bindings = getBindings(context);
        for (int i = 0; i < parameterNames.length; i++) {
            bindings.put(parameterNames[i], parameters[i].evaluate(eventsPerStream, isNewData, context));
        }
        return evaluateInternal(bindings, context);
    }

    public Object evaluate(Object lookupValues, ExprEvaluatorContext context) {
        Bindings bindings = getBindings(context);
        if (parameterNames.length == 1) {
            bindings.put(parameterNames[0], lookupValues);
        } else if (parameterNames.length > 1) {
            Object[] mk = (Object[]) lookupValues;
            for (int i = 0; i < parameterNames.length; i++) {
                bindings.put(parameterNames[i], mk[i]);
            }
        }
        return evaluateInternal(bindings, context);
    }

    private Bindings getBindings(ExprEvaluatorContext context) {
        Bindings bindings = executable.getEngine().createBindings();
        bindings.put(ExprNodeScript.CONTEXT_BINDING_NAME, context.getAllocateAgentInstanceScriptContext());
        return bindings;
    }

    private Object evaluateInternal(Bindings bindings, ExprEvaluatorContext context) {
        try {
            Object result = executable.eval(bindings);

            if (coercer != null && result != null) {
                return coercer.coerceBoxed((Number) result);
            }

            return result;
        } catch (ScriptException e) {
            String message = "Unexpected exception executing script '" + scriptName + "' for statement '" + context.getStatementName() + "' : " + e.getMessage();
            log.error(message, e);
            throw new EPException(message, e);
        }
    }
}
