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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class MethodConversionStrategyIterator extends MethodConversionStrategyBase {
    protected abstract EventBean getEventBean(Object value, AgentInstanceContext agentInstanceContext);

    public List<EventBean> convert(Object invocationResult, MethodTargetStrategy origin, AgentInstanceContext agentInstanceContext) {
        Iterator it = (Iterator) invocationResult;
        if (it == null || !it.hasNext()) {
            return Collections.emptyList();
        }
        ArrayList<EventBean> rowResult = new ArrayList<EventBean>(2);
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
