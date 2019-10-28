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
package com.espertech.esper.common.internal.epl.index.base;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;

public class EventTableUtil {
    /**
     * Build an index/table instance using the event properties for the event type.
     *
     * @param indexedStreamNum     - number of stream indexed
     * @param eventType            - type of event to expect
     * @param optionalIndexName    index name
     * @param agentInstanceContext context
     * @param item                 plan item
     * @param optionalValueSerde   value serde if any
     * @param isFireAndForget      indicates fire-and-forget
     * @param unique               indicates unique
     * @param coerceOnAddOnly      indicator whether to coerce on value-add
     * @return table build
     */
    public static EventTable buildIndex(AgentInstanceContext agentInstanceContext, int indexedStreamNum, QueryPlanIndexItem item, EventType eventType, boolean coerceOnAddOnly, boolean unique, String optionalIndexName, DataInputOutputSerde<Object> optionalValueSerde, boolean isFireAndForget) {
        String[] indexProps = item.getHashProps();
        Class[] indexTypes = item.getHashPropTypes();
        EventPropertyValueGetter indexGetter = item.getHashGetter();
        String[] rangeProps = item.getRangeProps();
        Class[] rangeTypes = item.getRangePropTypes();
        EventPropertyValueGetter[] rangeGetters = item.getRangeGetters();
        DataInputOutputSerde[] rangeKeySerdes = item.getRangeKeySerdes();
        EventTableIndexService eventTableIndexService = agentInstanceContext.getStatementContext().getEventTableIndexService();

        EventTable table;
        if (item.getAdvancedIndexProvisionDesc() != null) {
            table = eventTableIndexService.createCustom(optionalIndexName, indexedStreamNum, eventType, item.isUnique(), item.getAdvancedIndexProvisionDesc()).makeEventTables(agentInstanceContext, null)[0];
        } else if (rangeProps == null || rangeProps.length == 0) {
            if (indexProps == null || indexProps.length == 0) {
                EventTableFactory factory = eventTableIndexService.createUnindexed(indexedStreamNum, eventType, optionalValueSerde, isFireAndForget, agentInstanceContext.getStatementContext().getEventTableFactoryContext());
                table = factory.makeEventTables(agentInstanceContext, null)[0];
            } else {
                EventTableFactory factory = eventTableIndexService.createHashedOnly(indexedStreamNum, eventType, indexProps, indexTypes, item.getTransformFireAndForget(), item.getHashKeySerde(), unique, optionalIndexName, indexGetter, optionalValueSerde, isFireAndForget, agentInstanceContext.getStatementContext().getEventTableFactoryContext());
                table = factory.makeEventTables(agentInstanceContext, null)[0];
            }
        } else {
            if ((rangeProps.length == 1) && (indexProps == null || indexProps.length == 0)) {
                EventTableFactory factory = eventTableIndexService.createSorted(indexedStreamNum, eventType, rangeProps[0], rangeTypes[0], rangeGetters[0], rangeKeySerdes[0], optionalValueSerde, isFireAndForget, agentInstanceContext.getStatementContext().getEventTableFactoryContext());
                table = factory.makeEventTables(agentInstanceContext, null)[0];
            } else {
                EventTableFactory factory = eventTableIndexService.createComposite(indexedStreamNum, eventType, indexProps, indexTypes, indexGetter, item.getTransformFireAndForget(), item.getHashKeySerde(), rangeProps, rangeTypes, rangeGetters, rangeKeySerdes, optionalValueSerde, isFireAndForget);
                return factory.makeEventTables(agentInstanceContext, null)[0];
            }
        }
        return table;
    }
}
