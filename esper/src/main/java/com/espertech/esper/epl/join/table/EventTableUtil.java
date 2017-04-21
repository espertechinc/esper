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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.EventTableIndexService;
import com.espertech.esper.util.CollectionUtil;

public class EventTableUtil {
    /**
     * Build an index/table instance using the event properties for the event type.
     *
     * @param indexedStreamNum     - number of stream indexed
     * @param eventType            - type of event to expect
     * @param optionalIndexName    index name
     * @param agentInstanceContext context
     * @param item                 plan item
     * @param optionalSerde        serde if any
     * @param isFireAndForget      indicates fire-and-forget
     * @param unique               indicates unique
     * @param coerceOnAddOnly      indicator whether to coerce on value-add
     * @return table build
     */
    public static EventTable buildIndex(AgentInstanceContext agentInstanceContext, int indexedStreamNum, QueryPlanIndexItem item, EventType eventType, boolean coerceOnAddOnly, boolean unique, String optionalIndexName, Object optionalSerde, boolean isFireAndForget) {
        String[] indexProps = item.getIndexProps();
        Class[] indexCoercionTypes = normalize(item.getOptIndexCoercionTypes());
        String[] rangeProps = item.getRangeProps();
        Class[] rangeCoercionTypes = normalize(item.getOptRangeCoercionTypes());
        EventTableFactoryTableIdentAgentInstance ident = new EventTableFactoryTableIdentAgentInstance(agentInstanceContext);
        EventTableIndexService eventTableIndexService = agentInstanceContext.getStatementContext().getEventTableIndexService();

        EventTable table;
        if (item.getAdvancedIndexProvisionDesc() != null) {
            table = eventTableIndexService.createCustom(optionalIndexName, indexedStreamNum, eventType, item.isUnique(), item.getAdvancedIndexProvisionDesc()).makeEventTables(ident, agentInstanceContext)[0];
        } else if (rangeProps == null || rangeProps.length == 0) {
            if (indexProps == null || indexProps.length == 0) {
                EventTableFactory factory = eventTableIndexService.createUnindexed(indexedStreamNum, optionalSerde, isFireAndForget);
                table = factory.makeEventTables(ident, agentInstanceContext)[0];
            } else {
                // single index key
                if (indexProps.length == 1) {
                    if (indexCoercionTypes == null || indexCoercionTypes.length == 0) {
                        EventTableFactory factory = eventTableIndexService.createSingle(indexedStreamNum, eventType, indexProps[0], unique, optionalIndexName, optionalSerde, isFireAndForget);
                        table = factory.makeEventTables(ident, agentInstanceContext)[0];
                    } else {
                        if (coerceOnAddOnly) {
                            EventTableFactory factory = eventTableIndexService.createSingleCoerceAdd(indexedStreamNum, eventType, indexProps[0], indexCoercionTypes[0], optionalSerde, isFireAndForget);
                            table = factory.makeEventTables(ident, agentInstanceContext)[0];
                        } else {
                            EventTableFactory factory = eventTableIndexService.createSingleCoerceAll(indexedStreamNum, eventType, indexProps[0], indexCoercionTypes[0], optionalSerde, isFireAndForget);
                            table = factory.makeEventTables(ident, agentInstanceContext)[0];
                        }
                    }
                } else {
                    // Multiple index keys
                    if (indexCoercionTypes == null || indexCoercionTypes.length == 0) {
                        EventTableFactory factory = eventTableIndexService.createMultiKey(indexedStreamNum, eventType, indexProps, unique, optionalIndexName, optionalSerde, isFireAndForget);
                        table = factory.makeEventTables(ident, agentInstanceContext)[0];
                    } else {
                        if (coerceOnAddOnly) {
                            EventTableFactory factory = eventTableIndexService.createMultiKeyCoerceAdd(indexedStreamNum, eventType, indexProps, indexCoercionTypes, isFireAndForget);
                            table = factory.makeEventTables(ident, agentInstanceContext)[0];
                        } else {
                            EventTableFactory factory = eventTableIndexService.createMultiKeyCoerceAll(indexedStreamNum, eventType, indexProps, indexCoercionTypes, isFireAndForget);
                            table = factory.makeEventTables(ident, agentInstanceContext)[0];
                        }
                    }
                }
            }
        } else {
            if ((rangeProps.length == 1) && (indexProps == null || indexProps.length == 0)) {
                if (rangeCoercionTypes == null) {
                    EventTableFactory factory = eventTableIndexService.createSorted(indexedStreamNum, eventType, rangeProps[0], isFireAndForget);
                    return factory.makeEventTables(ident, agentInstanceContext)[0];
                } else {
                    EventTableFactory factory = eventTableIndexService.createSortedCoerce(indexedStreamNum, eventType, rangeProps[0], rangeCoercionTypes[0], isFireAndForget);
                    return factory.makeEventTables(ident, agentInstanceContext)[0];
                }
            } else {
                EventTableFactory factory = eventTableIndexService.createComposite(indexedStreamNum, eventType, indexProps, indexCoercionTypes, rangeProps, rangeCoercionTypes, isFireAndForget);
                return factory.makeEventTables(ident, agentInstanceContext)[0];
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
