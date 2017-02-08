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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class EvalInsertNoWildcardSingleColCoercionAvroWrap extends EvalBaseFirstProp implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalInsertNoWildcardSingleColCoercionAvroWrap.class);

    public EvalInsertNoWildcardSingleColCoercionAvroWrap(SelectExprContext selectExprContext, EventType resultEventType) {
        super(selectExprContext, resultEventType);
    }

    public EventBean processFirstCol(Object result) {
        EventBean wrappedEvent = super.getEventAdapterService().adapterForTypedAvro(result, super.getResultEventType());
        return super.getEventAdapterService().adapterForTypedWrapper(wrappedEvent, Collections.EMPTY_MAP, super.getResultEventType());
    }
}
