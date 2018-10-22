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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;

import java.util.*;

public abstract class MethodConversionStrategyCollection extends MethodConversionStrategyBase {
    protected abstract EventBean getEventBean(Object value, AgentInstanceContext agentInstanceContext);

    public List<EventBean> convert(Object invocationResult, MethodTargetStrategy origin, AgentInstanceContext agentInstanceContext) {
        Collection collection = (Collection) invocationResult;
        int length = collection.size();
        if (length == 0) {
            return Collections.emptyList();
        }
        if (length == 1) {
            Object value = collection.iterator().next();
            if (checkNonNullArrayValue(value, origin)) {
                EventBean event = getEventBean(value, agentInstanceContext);
                return Collections.singletonList(event);
            }
            return Collections.emptyList();
        }
        ArrayList<EventBean> rowResult = new ArrayList<EventBean>(length);
        Iterator it = collection.iterator();
        for (; it.hasNext(); ) {
            Object value = it.next();
            if (checkNonNullArrayValue(value, origin)) {
                EventBean event = getEventBean(value, agentInstanceContext);
                rowResult.add(event);
            }
        }
        return rowResult;
    }
}
