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
import com.espertech.esper.epl.join.table.*;

import java.util.List;

/**
 * Strategy for building an index out of poll-results knowing the properties to base the index on.
 */
public class PollResultIndexingStrategySorted implements PollResultIndexingStrategy {
    private final int streamNum;
    private final EventType eventType;
    private final String propertyName;
    private final Class coercionType;

    public PollResultIndexingStrategySorted(int streamNum, EventType eventType, String propertyName, Class coercionType) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.propertyName = propertyName;
        this.coercionType = coercionType;
    }

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, StatementContext statementContext) {
        if (!isActiveCache) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, streamNum)};
        }
        PropertySortedEventTableFactory tableFactory;
        if (coercionType == null) {
            tableFactory = new PropertySortedEventTableFactory(streamNum, eventType, propertyName);
        } else {
            tableFactory = new PropertySortedEventTableCoercedFactory(streamNum, eventType, propertyName, coercionType);
        }
        ExprEvaluatorContextStatement evaluatorContextStatement = new ExprEvaluatorContextStatement(statementContext, false);
        EventTable[] tables = tableFactory.makeEventTables(new EventTableFactoryTableIdentStmt(statementContext), evaluatorContextStatement);
        for (EventTable table : tables) {
            table.add(pollResult.toArray(new EventBean[pollResult.size()]), evaluatorContextStatement);
        }
        return tables;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " property " + propertyName + " coercion " + coercionType;
    }
}
