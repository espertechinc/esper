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
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import com.espertech.esper.util.TriFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class EvalInsertNoWildcardSingleColCoercionRevisionFunc extends EvalBaseFirstProp implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalInsertNoWildcardSingleColCoercionRevisionFunc.class);

    private final ValueAddEventProcessor vaeProcessor;
    private final EventType vaeInnerEventType;
    private final TriFunction<EventAdapterService, Object, EventType, EventBean> func;

    public EvalInsertNoWildcardSingleColCoercionRevisionFunc(SelectExprContext selectExprContext, EventType resultEventType, ValueAddEventProcessor vaeProcessor, EventType vaeInnerEventType, TriFunction<EventAdapterService, Object, EventType, EventBean> func) {
        super(selectExprContext, resultEventType);
        this.vaeProcessor = vaeProcessor;
        this.vaeInnerEventType = vaeInnerEventType;
        this.func = func;
    }

    public EventBean processFirstCol(Object result) {
        EventBean wrappedEvent = func.apply(super.getEventAdapterService(), result, super.getResultEventType());
        return vaeProcessor.getValueAddEventBean(super.getEventAdapterService().adapterForTypedWrapper(wrappedEvent, Collections.EMPTY_MAP, vaeInnerEventType));
    }
}
