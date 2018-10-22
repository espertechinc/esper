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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.util.FilteredEventIterator;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

public class TableStateViewableInternal extends ViewSupport {

    private final TableInstance tableInstance;
    private final ExprEvaluator optionalTableFilter;

    public TableStateViewableInternal(TableInstance tableInstance, ExprEvaluator optionalTableFilter) {
        this.tableInstance = tableInstance;
        this.optionalTableFilter = optionalTableFilter;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        // no action required
    }

    public EventType getEventType() {
        return tableInstance.getTable().getMetaData().getInternalEventType();
    }

    public Iterator<EventBean> iterator() {
        Iterator<EventBean> it = tableInstance.getEventCollection().iterator();
        if (optionalTableFilter != null) {
            return new FilteredEventIterator(optionalTableFilter, it, tableInstance.getAgentInstanceContext());
        }
        return tableInstance.getEventCollection().iterator();
    }
}
