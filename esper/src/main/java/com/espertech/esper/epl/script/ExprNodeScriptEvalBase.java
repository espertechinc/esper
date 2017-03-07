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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.enummethod.dot.ArrayWrappingCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;

public abstract class ExprNodeScriptEvalBase implements ExprEvaluator, ExprEvaluatorEnumeration {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeScriptEvalBase.class);

    protected final String scriptName;
    protected final String statementName;
    protected final String[] names;
    protected final ExprEvaluator[] parameters;
    protected final Class returnType;
    protected final EventType eventTypeCollection;
    protected final SimpleNumberCoercer coercer;

    public ExprNodeScriptEvalBase(String scriptName, String statementName, String[] names, ExprEvaluator[] parameters, Class returnType, EventType eventTypeCollection) {
        this.scriptName = scriptName;
        this.statementName = statementName;
        this.names = names;
        this.parameters = parameters;
        this.returnType = returnType;
        this.eventTypeCollection = eventTypeCollection;

        if (JavaClassHelper.isNumeric(returnType)) {
            coercer = SimpleNumberCoercerFactory.getCoercer(Number.class, JavaClassHelper.getBoxedType(returnType));
        } else {
            coercer = null;
        }
    }

    public Class getType() {
        return returnType;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return eventTypeCollection;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        if (result.getClass().isArray()) {
            return Arrays.asList((EventBean[]) result);
        }
        return (Collection) result;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (returnType.isArray()) {
            return returnType.getComponentType();
        }
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        return new ArrayWrappingCollection(result);
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }
}
