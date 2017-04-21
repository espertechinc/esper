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
package com.espertech.esper.epl.join.pollindex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableFactoryTableIdentStmt;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingleFactory;
import com.espertech.esper.epl.join.table.UnindexedEventTableList;

import java.util.Arrays;
import java.util.List;

/**
 * Strategy for building an index out of poll-results knowing the properties to base the index on.
 */
public class PollResultIndexingStrategyIndexSingleArray implements PollResultIndexingStrategy {
    private final int streamNum;
    private final EventType eventType;
    private final String[] propertyNames;

    /**
     * Ctor.
     *
     * @param streamNum     is the stream number of the indexed stream
     * @param eventType     is the event type of the indexed stream
     * @param propertyNames is the property names to be indexed
     */
    public PollResultIndexingStrategyIndexSingleArray(int streamNum, EventType eventType, String[] propertyNames) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.propertyNames = propertyNames;
    }

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, StatementContext statementContext) {
        if (!isActiveCache) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, streamNum)};
        }
        EventTable[] tables = new EventTable[propertyNames.length];
        ExprEvaluatorContextStatement evaluatorContextStatement = new ExprEvaluatorContextStatement(statementContext, false);
        for (int i = 0; i < propertyNames.length; i++) {
            PropertyIndexedEventTableSingleFactory factory = new PropertyIndexedEventTableSingleFactory(streamNum, eventType, propertyNames[i], false, null);
            tables[i] = factory.makeEventTables(new EventTableFactoryTableIdentStmt(statementContext), evaluatorContextStatement)[0];
            tables[i].add(pollResult.toArray(new EventBean[pollResult.size()]), evaluatorContextStatement);
        }
        return tables;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " properties " + Arrays.toString(propertyNames);
    }
}
