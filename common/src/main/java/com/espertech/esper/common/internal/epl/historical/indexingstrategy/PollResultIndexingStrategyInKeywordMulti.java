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
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableFactory;

import java.util.List;

public class PollResultIndexingStrategyInKeywordMulti implements PollResultIndexingStrategy {
    private int streamNum;
    private String[] propertyNames;
    private EventPropertyValueGetter[] valueGetters;
    private PropertyHashedEventTableFactory[] factories;

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, AgentInstanceContext agentInstanceContext) {
        if (!isActiveCache) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, streamNum)};
        }

        EventTable[] tables = new EventTable[valueGetters.length];
        for (int i = 0; i < valueGetters.length; i++) {
            tables[i] = factories[i].makeEventTables(agentInstanceContext, null)[0];
            tables[i].add(pollResult.toArray(new EventBean[pollResult.size()]), agentInstanceContext);
        }
        return tables;
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setPropertyNames(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }

    public void setValueGetters(EventPropertyValueGetter[] valueGetters) {
        this.valueGetters = valueGetters;
    }

    public void init() {
        factories = new PropertyHashedEventTableFactory[valueGetters.length];
        for (int i = 0; i < propertyNames.length; i++) {
            factories[i] = new PropertyHashedEventTableFactory(streamNum, new String[]{propertyNames[i]}, false, null, valueGetters[i], null);
        }
    }
}
