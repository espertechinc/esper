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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;

public abstract class EvalBase {

    protected final SelectExprForgeContext context;
    protected final EventType resultEventType;

    public EvalBase(SelectExprForgeContext context, EventType resultEventType) {
        this.context = context;
        this.resultEventType = resultEventType;
    }

    public EventAdapterService getEventAdapterService() {
        return context.getEventAdapterService();
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
