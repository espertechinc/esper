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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EvalInsertWildcardWrapper extends EvalBaseMap implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalInsertWildcardWrapper.class);

    public EvalInsertWildcardWrapper(SelectExprContext selectExprContext, EventType resultEventType) {
        super(selectExprContext, resultEventType);
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[0];
        // Using a wrapper bean since we cannot use the same event type else same-type filters match.
        // Wrapping it even when not adding properties is very inexpensive.
        return super.getEventAdapterService().adapterForTypedWrapper(theEvent, props, super.getResultEventType());
    }
}
