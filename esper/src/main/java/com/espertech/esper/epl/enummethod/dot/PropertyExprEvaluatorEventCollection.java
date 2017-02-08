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
import com.espertech.esper.core.service.ExpressionResultCacheEntry;
import com.espertech.esper.core.service.ExpressionResultCacheForPropUnwrap;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumerationGivenEvent;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;

import java.util.Arrays;
import java.util.Collection;

public class PropertyExprEvaluatorEventCollection implements ExprEvaluatorEnumeration, ExprEvaluatorEnumerationGivenEvent {

    private final String propertyNameCache;
    private final int streamId;
    private final EventType fragmentType;
    private final EventPropertyGetter getter;
    private final boolean disablePropertyExpressionEventCollCache;

    public PropertyExprEvaluatorEventCollection(String propertyNameCache, int streamId, EventType fragmentType, EventPropertyGetter getter, boolean disablePropertyExpressionEventCollCache) {
        this.propertyNameCache = propertyNameCache;
        this.streamId = streamId;
        this.fragmentType = fragmentType;
        this.getter = getter;
        this.disablePropertyExpressionEventCollCache = disablePropertyExpressionEventCollCache;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean eventInQuestion = eventsPerStream[streamId];
        if (eventInQuestion == null) {
            return null;
        }
        return evaluateInternal(eventInQuestion, context);
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        if (event == null) {
            return null;
        }
        return evaluateInternal(event, context);
    }

    private Collection<EventBean> evaluateInternal(EventBean eventInQuestion, ExprEvaluatorContext context) {

        if (disablePropertyExpressionEventCollCache) {
            EventBean[] events = (EventBean[]) getter.getFragment(eventInQuestion);
            return events == null ? null : Arrays.asList(events);
        }

        ExpressionResultCacheForPropUnwrap cache = context.getExpressionResultCacheService().getAllocateUnwrapProp();
        ExpressionResultCacheEntry<EventBean, Collection<EventBean>> cacheEntry = cache.getPropertyColl(propertyNameCache, eventInQuestion);
        if (cacheEntry != null) {
            return cacheEntry.getResult();
        }

        EventBean[] events = (EventBean[]) getter.getFragment(eventInQuestion);
        Collection<EventBean> coll = events == null ? null : Arrays.asList(events);
        cache.savePropertyColl(propertyNameCache, eventInQuestion, coll);
        return coll;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) {
        return fragmentType;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateEventGetEventBean(EventBean event, ExprEvaluatorContext context) {
        return null;
    }
}
