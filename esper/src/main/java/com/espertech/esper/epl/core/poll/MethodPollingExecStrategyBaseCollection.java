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
package com.espertech.esper.epl.core.poll;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import net.sf.cglib.reflect.FastMethod;

import java.util.*;

public abstract class MethodPollingExecStrategyBaseCollection extends MethodPollingExecStrategyBase {
    public MethodPollingExecStrategyBaseCollection(EventAdapterService eventAdapterService, FastMethod method, EventType eventType, Object invocationTarget, MethodPollingExecStrategyEnum strategy, VariableReader variableReader, String variableName, VariableService variableService) {
        super(eventAdapterService, method, eventType, invocationTarget, strategy, variableReader, variableName, variableService);
    }

    protected abstract EventBean getEventBean(Object value);

    protected List<EventBean> handleResult(Object invocationResult) {
        Collection collection = (Collection) invocationResult;
        int length = collection.size();
        if (length == 0) {
            return Collections.emptyList();
        }
        if (length == 1) {
            Object value = collection.iterator().next();
            if (checkNonNullArrayValue(value)) {
                EventBean event = getEventBean(value);
                return Collections.singletonList(event);
            }
            return Collections.emptyList();
        }
        ArrayList<EventBean> rowResult = new ArrayList<EventBean>(length);
        Iterator it = collection.iterator();
        for (; it.hasNext(); ) {
            Object value = it.next();
            if (checkNonNullArrayValue(value)) {
                EventBean event = getEventBean(value);
                rowResult.add(event);
            }
        }
        return rowResult;
    }
}
