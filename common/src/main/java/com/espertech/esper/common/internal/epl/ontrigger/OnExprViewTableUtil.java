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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.Table;

public class OnExprViewTableUtil {
    public static EventBean[] toPublic(EventBean[] matching, Table table, EventBean[] triggers, boolean isNewData, ExprEvaluatorContext context) {
        EventBean[] eventsPerStream = new EventBean[2];
        eventsPerStream[0] = triggers[0];

        EventBean[] events = new EventBean[matching.length];
        for (int i = 0; i < events.length; i++) {
            eventsPerStream[1] = matching[i];
            events[i] = table.getEventToPublic().convert(matching[i], eventsPerStream, isNewData, context);
        }
        return events;
    }
}
