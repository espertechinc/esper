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
package com.espertech.esper.common.internal.epl.script.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.enummethod.dot.ArrayWrappingCollection;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.Arrays;
import java.util.Collection;

public abstract class ScriptEvaluatorBase implements ScriptEvaluator {

    protected final String scriptName;
    protected final String[] parameterNames;
    protected final ExprEvaluator[] parameters;
    protected final SimpleNumberCoercer coercer;

    public ScriptEvaluatorBase(String scriptName, String[] parameterNames, ExprEvaluator[] parameters, SimpleNumberCoercer coercer) {
        this.scriptName = scriptName;
        this.parameterNames = parameterNames;
        this.parameters = parameters;
        this.coercer = coercer;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = evaluate(eventsPerStream, isNewData, context);
        return scriptResultToROCollectionEvents(result);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        return new ArrayWrappingCollection(result);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    private static Collection<EventBean> scriptResultToROCollectionEvents(Object result) {
        if (result == null) {
            return null;
        }
        if (result.getClass().isArray()) {
            return Arrays.asList((EventBean[]) result);
        }
        return (Collection<EventBean>) result;
    }
}
