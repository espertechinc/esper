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
package com.espertech.esper.epl.updatehelper;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBeanUpdateHelper {
    private static final Logger log = LoggerFactory.getLogger(EventBeanUpdateHelper.class);

    private final EventBeanCopyMethod copyMethod;
    private final EventBeanUpdateItem[] updateItems;

    public EventBeanUpdateHelper(EventBeanCopyMethod copyMethod, EventBeanUpdateItem[] updateItems) {
        this.copyMethod = copyMethod;
        this.updateItems = updateItems;
    }

    public EventBean updateWCopy(EventBean matchingEvent, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraUpdate(matchingEvent, eventsPerStream, updateItems.length, true);
        }

        EventBean copy = copyMethod.copy(matchingEvent);
        eventsPerStream[0] = copy;
        eventsPerStream[2] = matchingEvent; // initial value

        updateInternal(eventsPerStream, exprEvaluatorContext, copy);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraUpdate(copy);
        }
        return copy;
    }

    public void updateNoCopy(EventBean matchingEvent, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraUpdate(matchingEvent, eventsPerStream, updateItems.length, false);
        }

        updateInternal(eventsPerStream, exprEvaluatorContext, matchingEvent);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraUpdate(matchingEvent);
        }
    }

    public EventBeanUpdateItem[] getUpdateItems() {
        return updateItems;
    }

    public boolean isRequiresStream2InitialValueEvent() {
        return copyMethod != null;
    }

    private void updateInternal(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, EventBean target) {
        for (int i = 0; i < updateItems.length; i++) {
            EventBeanUpdateItem updateItem = updateItems[i];

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qInfraUpdateRHSExpr(i, updateItem);
            }
            Object result = updateItem.getExpression().evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aInfraUpdateRHSExpr(result);
            }

            if (updateItem.getOptionalWriter() != null) {
                if (result == null && updateItem.isNotNullableField()) {
                    log.warn("Null value returned by expression for assignment to property '" + updateItem.getOptionalPropertyName() + " is ignored as the property type is not nullable for expression");
                    continue;
                }

                if (updateItem.getOptionalWidener() != null) {
                    result = updateItem.getOptionalWidener().widen(result);
                }
                updateItem.getOptionalWriter().write(result, target);
            }
        }
    }
}
