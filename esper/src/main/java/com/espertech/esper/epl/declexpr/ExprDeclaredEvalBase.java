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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.ExpressionResultCacheEntry;
import com.espertech.esper.core.service.ExpressionResultCacheForDeclaredExprLastColl;
import com.espertech.esper.core.service.ExpressionResultCacheForDeclaredExprLastValue;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.LinkedHashMap;

public abstract class ExprDeclaredEvalBase implements ExprEvaluatorTypableReturn, ExprEvaluatorEnumeration {
    private final ExprEvaluator innerEvaluator;
    private final ExprEvaluatorEnumeration innerEvaluatorLambda;
    private final ExpressionDeclItem prototype;
    private final boolean isCache;

    public abstract EventBean[] getEventsPerStreamRewritten(EventBean[] eventsPerStream);

    public ExprDeclaredEvalBase(ExprEvaluator innerEvaluator, ExpressionDeclItem prototype, boolean isCache) {
        this.innerEvaluator = innerEvaluator;
        this.prototype = prototype;
        if (innerEvaluator instanceof ExprEvaluatorEnumeration) {
            innerEvaluatorLambda = (ExprEvaluatorEnumeration) innerEvaluator;
        } else {
            innerEvaluatorLambda = null;
        }
        this.isCache = isCache;
    }

    public ExprEvaluator getInnerEvaluator() {
        return innerEvaluator;
    }

    public Class getType() {
        return innerEvaluator.getType();
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        if (innerEvaluator instanceof ExprEvaluatorTypableReturn) {
            return ((ExprEvaluatorTypableReturn) innerEvaluator).getRowProperties();
        }
        return null;
    }

    public Boolean isMultirow() {
        if (innerEvaluator instanceof ExprEvaluatorTypableReturn) {
            return ((ExprEvaluatorTypableReturn) innerEvaluator).isMultirow();
        }
        return null;
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return ((ExprEvaluatorTypableReturn) innerEvaluator).evaluateTypableSingle(eventsPerStream, isNewData, context);
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return ((ExprEvaluatorTypableReturn) innerEvaluator).evaluateTypableMulti(eventsPerStream, isNewData, context);
    }

    public final Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDeclared(prototype);
        }

        // rewrite streams
        EventBean[] events = getEventsPerStreamRewritten(eventsPerStream);

        Object result;
        if (isCache) {      // no the same cache as for iterator
            ExpressionResultCacheForDeclaredExprLastValue cache = context.getExpressionResultCacheService().getAllocateDeclaredExprLastValue();
            ExpressionResultCacheEntry<EventBean[], Object> entry = cache.getDeclaredExpressionLastValue(prototype, events);
            if (entry != null) {
                return entry.getResult();
            }
            result = innerEvaluator.evaluate(events, isNewData, context);
            cache.saveDeclaredExpressionLastValue(prototype, events, result);
        } else {
            result = innerEvaluator.evaluate(events, isNewData, context);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDeclared(result);
        }
        return result;
    }

    public final Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {

        // rewrite streams
        EventBean[] events = getEventsPerStreamRewritten(eventsPerStream);

        Collection<EventBean> result;
        if (isCache) {
            ExpressionResultCacheForDeclaredExprLastColl cache = context.getExpressionResultCacheService().getAllocateDeclaredExprLastColl();
            ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> entry = cache.getDeclaredExpressionLastColl(prototype, events);
            if (entry != null) {
                return entry.getResult();
            }

            result = innerEvaluatorLambda.evaluateGetROCollectionEvents(events, isNewData, context);
            cache.saveDeclaredExpressionLastColl(prototype, events, result);
            return result;
        } else {
            result = innerEvaluatorLambda.evaluateGetROCollectionEvents(events, isNewData, context);
        }

        return result;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {

        // rewrite streams
        EventBean[] events = getEventsPerStreamRewritten(eventsPerStream);

        Collection result;
        if (isCache) {
            ExpressionResultCacheForDeclaredExprLastColl cache = context.getExpressionResultCacheService().getAllocateDeclaredExprLastColl();
            ExpressionResultCacheEntry<EventBean[], Collection<EventBean>> entry = cache.getDeclaredExpressionLastColl(prototype, events);
            if (entry != null) {
                return entry.getResult();
            }

            result = innerEvaluatorLambda.evaluateGetROCollectionScalar(events, isNewData, context);
            cache.saveDeclaredExpressionLastColl(prototype, events, result);
            return result;
        } else {
            result = innerEvaluatorLambda.evaluateGetROCollectionScalar(events, isNewData, context);
        }

        return result;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (innerEvaluatorLambda != null) {
            return innerEvaluatorLambda.getComponentTypeCollection();
        }
        return null;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (innerEvaluatorLambda != null) {
            return innerEvaluatorLambda.getEventTypeCollection(eventAdapterService, statementId);
        }
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (innerEvaluatorLambda != null) {
            return innerEvaluatorLambda.getEventTypeSingle(eventAdapterService, statementId);
        }
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return innerEvaluatorLambda.evaluateGetEventBean(eventsPerStream, isNewData, context);
    }
}