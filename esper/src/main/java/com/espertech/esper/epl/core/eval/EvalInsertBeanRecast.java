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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;

public class EvalInsertBeanRecast implements SelectExprProcessor {

    private final EventType eventType;
    private final EventAdapterService eventAdapterService;
    private final int streamNumber;

    public EvalInsertBeanRecast(EventType targetType, EventAdapterService eventAdapterService, int streamNumber, EventType[] typesPerStream)
            throws ExprValidationException {
        this.eventType = targetType;
        this.eventAdapterService = eventAdapterService;
        this.streamNumber = streamNumber;

        EventType sourceType = typesPerStream[streamNumber];
        Class sourceClass = sourceType.getUnderlyingType();
        Class targetClass = targetType.getUnderlyingType();
        if (!JavaClassHelper.isSubclassOrImplementsInterface(sourceClass, targetClass)) {
            throw EvalInsertUtil.makeEventTypeCastException(sourceType, targetType);
        }
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamNumber];
        return eventAdapterService.adapterForTypedBean(theEvent.getUnderlying(), eventType);
    }

    public EventType getResultEventType() {
        return eventType;
    }
}
