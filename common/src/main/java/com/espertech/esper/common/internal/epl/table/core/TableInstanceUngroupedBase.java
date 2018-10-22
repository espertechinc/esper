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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

public abstract class TableInstanceUngroupedBase extends TableInstanceBase implements TableInstanceUngrouped {

    public TableInstanceUngroupedBase(Table table, AgentInstanceContext agentInstanceContext) {
        super(table, agentInstanceContext);
    }

    protected ObjectArrayBackedEventBean createRowIntoTable() {
        EventType eventType = table.getMetaData().getInternalEventType();
        AggregationRow aggregationRow = table.getAggregationRowFactory().make();
        Object[] data = new Object[eventType.getPropertyDescriptors().length];
        data[0] = aggregationRow;
        ObjectArrayBackedEventBean row = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedObjectArray(data, eventType);
        addEvent(row);
        return row;
    }

    public void addExplicitIndex(String indexName, String indexModuleName, QueryPlanIndexItem explicitIndexDesc, boolean isRecoveringResilient) throws ExprValidationException {
        throw new UnsupportedOperationException("Ungrouped tables do not allow explicit indexes");
    }

    public void removeExplicitIndex(String indexName) {
        throw new UnsupportedOperationException("Ungrouped tables do not allow explicit indexes");
    }
}
