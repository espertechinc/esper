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
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.event.core.NaturalEventBean;

public class OnExprViewTableChangeHandler {
    private final Table table;
    private OneEventCollection coll;

    public OnExprViewTableChangeHandler(Table table) {
        this.table = table;
    }

    public EventBean[] getEvents() {
        if (coll == null) {
            return null;
        }
        return coll.toArray();
    }

    public void add(EventBean theEvent, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (coll == null) {
            coll = new OneEventCollection();
        }
        if (theEvent instanceof NaturalEventBean) {
            theEvent = ((NaturalEventBean) theEvent).getOptionalSynthetic();
        }
        coll.add(table.getEventToPublic().convert(theEvent, eventsPerStream, isNewData, context));
    }
}
