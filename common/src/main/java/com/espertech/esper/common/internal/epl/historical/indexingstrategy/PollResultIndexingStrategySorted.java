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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTableFactory;

import java.util.List;

public class PollResultIndexingStrategySorted implements PollResultIndexingStrategy {
    private int streamNum;
    private String propertyName;
    private EventPropertyValueGetter valueGetter;
    private Class valueType;
    private PropertySortedEventTableFactory factory;

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, AgentInstanceContext agentInstanceContext) {
        if (!isActiveCache) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, streamNum)};
        }
        EventTable[] tables = factory.makeEventTables(agentInstanceContext, null);
        for (EventTable table : tables) {
            table.add(pollResult.toArray(new EventBean[pollResult.size()]), agentInstanceContext);
        }
        return tables;
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setValueGetter(EventPropertyValueGetter valueGetter) {
        this.valueGetter = valueGetter;
    }

    public void setValueType(Class valueType) {
        this.valueType = valueType;
    }

    public void init() {
        factory = new PropertySortedEventTableFactory(streamNum, propertyName, valueGetter, valueType);
    }
}
