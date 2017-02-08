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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumerationGivenEvent;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class PropertyExprEvaluatorScalarArray implements ExprEvaluatorEnumeration, ExprEvaluatorEnumerationGivenEvent {

    private static final Logger log = LoggerFactory.getLogger(PropertyExprEvaluatorScalarArray.class);

    private final String propertyName;
    private final int streamId;
    private final EventPropertyGetter getter;
    private final Class componentType;

    public PropertyExprEvaluatorScalarArray(String propertyName, int streamId, EventPropertyGetter getter, Class componentType) {
        this.propertyName = propertyName;
        this.streamId = streamId;
        this.getter = getter;
        this.componentType = componentType;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean eventInQuestion = eventsPerStream[streamId];
        return evaluateEventGetROCollectionScalar(eventInQuestion, context);
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        if (event == null) {
            return null;
        }
        return evaluateGetInterval(event);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return componentType;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) {
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateEventGetEventBean(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    private Collection evaluateGetInterval(EventBean event) {
        Object value = getter.get(event);
        if (value == null) {
            return null;
        }
        if (!(value.getClass().isArray())) {
            log.warn("Expected array-type input from property '" + propertyName + "' but received " + value.getClass());
            return null;
        }
        return new ArrayWrappingCollection(value);
    }
}
