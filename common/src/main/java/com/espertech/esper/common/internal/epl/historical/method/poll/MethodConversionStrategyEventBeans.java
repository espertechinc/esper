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

public class MethodConversionStrategyEventBeans extends MethodConversionStrategyBase {
    public List<EventBean> convert(Object invocationResult, MethodTargetStrategy origin, AgentInstanceContext agentInstanceContext) {
        if (invocationResult == null) {
            return Collections.emptyList();
        }
        if (invocationResult.getClass().isArray()) {
            return Arrays.asList((EventBean[]) invocationResult);
        }
        if (invocationResult instanceof Collection) {
            Collection collection = (Collection) invocationResult;
            int length = collection.size();
            if (length == 0) {
                return Collections.emptyList();
            }
            ArrayList<EventBean> rowResult = new ArrayList<EventBean>(length);
            Iterator<EventBean> it = collection.iterator();
            for (; it.hasNext(); ) {
                EventBean value = it.next();
                if (value != null) {
                    rowResult.add(value);
                }
            }
            return rowResult;
        }
        Iterator<EventBean> it = (Iterator<EventBean>) invocationResult;
        if (!it.hasNext()) {
            return Collections.emptyList();
        }
        ArrayList<EventBean> rowResult = new ArrayList<>();
        for (; it.hasNext(); ) {
            EventBean value = it.next();
            if (value != null) {
                rowResult.add(value);
            }
        }
        return rowResult;
    }
}
