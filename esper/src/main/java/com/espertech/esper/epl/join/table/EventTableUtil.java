/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.EventTableIndexService;
import com.espertech.esper.util.CollectionUtil;

public class EventTableUtil
{
    /**
     * Build an index/table instance using the event properties for the event type.
     *
     * @param indexedStreamNum - number of stream indexed
     * @param eventType - type of event to expect
     * @param optionalIndexName
     * @return table build
     */
    public static EventTable buildIndex(AgentInstanceContext agentInstanceContext, int indexedStreamNum, QueryPlanIndexItem item, EventType eventType, boolean coerceOnAddOnly, boolean unique, String optionalIndexName, Object optionalSerde)
    {
        String[] indexProps = item.getIndexProps();
        Class[] indexCoercionTypes = normalize(item.getOptIndexCoercionTypes());
        String[] rangeProps = item.getRangeProps();
        Class[] rangeCoercionTypes = normalize(item.getOptRangeCoercionTypes());
        EventTableFactoryTableIdentAgentInstance ident = new EventTableFactoryTableIdentAgentInstance(agentInstanceContext);
        EventTableIndexService eventTableIndexService = agentInstanceContext.getStatementContext().getEventTableIndexService();

        EventTable table;
        if (rangeProps == null || rangeProps.length == 0) {
            if (indexProps == null || indexProps.length == 0)
            {
                EventTableFactory factory = agentInstanceContext.getStatementContext().getEventTableIndexService().createUnindexed(indexedStreamNum, optionalSerde);
                table = factory.makeEventTables(ident)[0];
            }
            else
            {
                // single index key
                if (indexProps.length == 1) {
                    if (indexCoercionTypes == null || indexCoercionTypes.length == 0)
                    {
                        EventTableFactory factory = eventTableIndexService.createSingle(indexedStreamNum, eventType, indexProps[0], unique, optionalIndexName, optionalSerde);
                        table = factory.makeEventTables(ident)[0];
                    }
                    else
                    {
                        if (coerceOnAddOnly) {
                            EventTableFactory factory = eventTableIndexService.createSingleCoerceAdd(indexedStreamNum, eventType, indexProps[0], indexCoercionTypes[0], optionalSerde);
                            table = factory.makeEventTables(ident)[0];
                        }
                        else {
                            EventTableFactory factory = eventTableIndexService.createSingleCoerceAll(indexedStreamNum, eventType, indexProps[0], indexCoercionTypes[0], optionalSerde);
                            table = factory.makeEventTables(ident)[0];
                        }
                    }
                }
                // Multiple index keys
                else {
                    if (indexCoercionTypes == null || indexCoercionTypes.length == 0)
                    {
                        EventTableFactory factory = new PropertyIndexedEventTableFactory(indexedStreamNum, eventType, indexProps, unique, optionalIndexName);
                        table = factory.makeEventTables(ident)[0];
                    }
                    else
                    {
                        if (coerceOnAddOnly) {
                            EventTableFactory factory = new PropertyIndexedEventTableCoerceAddFactory(indexedStreamNum, eventType, indexProps, indexCoercionTypes);
                            table = factory.makeEventTables(ident)[0];
                        }
                        else {
                            EventTableFactory factory = new PropertyIndexedEventTableCoerceAllFactory(indexedStreamNum, eventType, indexProps, indexCoercionTypes);
                            table = factory.makeEventTables(ident)[0];
                        }
                    }
                }
            }
        }
        else {
            if ((rangeProps.length == 1) && (indexProps == null || indexProps.length == 0)) {
                if (rangeCoercionTypes == null) {
                    EventTableFactory factory = new PropertySortedEventTableFactory(indexedStreamNum, eventType, rangeProps[0]);
                    return factory.makeEventTables(ident)[0];
                }
                else {
                    EventTableFactory factory = new PropertySortedEventTableCoercedFactory(indexedStreamNum, eventType, rangeProps[0], rangeCoercionTypes[0]);
                    return factory.makeEventTables(ident)[0];
                }
            }
            else {
                EventTableFactory factory = new PropertyCompositeEventTableFactory(indexedStreamNum, eventType, indexProps, indexCoercionTypes, rangeProps, rangeCoercionTypes);
                return factory.makeEventTables(ident)[0];
            }
        }
        return table;
    }

    private static Class[] normalize(Class[] types) {
        if (types == null) {
            return null;
        }
        if (CollectionUtil.isAllNullArray(types)) {
            return null;
        }
        return types;
    }
}
