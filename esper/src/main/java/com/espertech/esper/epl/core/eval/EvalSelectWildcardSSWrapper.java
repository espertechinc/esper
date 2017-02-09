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
import com.espertech.esper.event.DecoratingEventBean;

import java.util.HashMap;
import java.util.Map;

public class EvalSelectWildcardSSWrapper extends EvalBaseMap implements SelectExprProcessor {

    public EvalSelectWildcardSSWrapper(SelectExprContext selectExprContext, EventType resultEventType) {
        super(selectExprContext, resultEventType);
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        // In case of a wildcard and single stream that is itself a
        // wrapper bean, we also need to add the map properties
        DecoratingEventBean wrapper = (DecoratingEventBean) eventsPerStream[0];
        if (wrapper != null) {
            Map<String, Object> map = wrapper.getDecoratingProperties();
            if ((super.getExprNodes().length == 0) && (!map.isEmpty())) {
                props = new HashMap<String, Object>(map);
            } else {
                props.putAll(map);
            }
        }

        EventBean theEvent = eventsPerStream[0];

        // Using a wrapper bean since we cannot use the same event type else same-type filters match.
        // Wrapping it even when not adding properties is very inexpensive.
        return super.getEventAdapterService().adapterForTypedWrapper(theEvent, props, super.getResultEventType());
    }
}