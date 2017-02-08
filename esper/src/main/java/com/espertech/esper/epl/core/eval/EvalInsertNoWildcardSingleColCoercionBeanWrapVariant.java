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
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class EvalInsertNoWildcardSingleColCoercionBeanWrapVariant extends EvalBaseFirstProp implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalInsertNoWildcardSingleColCoercionBeanWrapVariant.class);

    private final ValueAddEventProcessor vaeProcessor;

    public EvalInsertNoWildcardSingleColCoercionBeanWrapVariant(SelectExprContext selectExprContext, EventType resultEventType, ValueAddEventProcessor vaeProcessor) {
        super(selectExprContext, resultEventType);
        this.vaeProcessor = vaeProcessor;
    }

    public EventBean processFirstCol(Object result) {
        EventBean wrappedEvent = super.getEventAdapterService().adapterForBean(result);
        EventBean variant = vaeProcessor.getValueAddEventBean(wrappedEvent);
        return super.getEventAdapterService().adapterForTypedWrapper(variant, Collections.EMPTY_MAP, super.getResultEventType());
    }
}
