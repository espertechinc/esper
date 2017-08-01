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
package com.espertech.esper.epl.db;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTableList;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportStreamTypeSvc3Stream;
import junit.framework.TestCase;

import java.util.*;

public class TestDatabasePollingViewable extends TestCase {
    private DatabasePollingViewable pollingViewable;
    private PollResultIndexingStrategy indexingStrategy;

    public void setUp() throws Exception {
        List<String> inputProperties = Arrays.asList(new String[]{"s0.intPrimitive"});

        DataCache dataCache = new DataCacheLRUImpl(100);

        Map<String, Object> resultProperties = new HashMap<String, Object>();
        resultProperties.put("myvarchar", String.class);
        EventType resultEventType = SupportEventAdapterService.getService().createAnonymousMapType("test", resultProperties, true);

        Map<MultiKey<Object>, List<EventBean>> pollResults = new HashMap<MultiKey<Object>, List<EventBean>>();
        pollResults.put(new MultiKey<Object>(new Object[]{-1}), new LinkedList<EventBean>());
        pollResults.put(new MultiKey<Object>(new Object[]{500}), new LinkedList<EventBean>());
        SupportPollingStrategy supportPollingStrategy = new SupportPollingStrategy(pollResults);

        pollingViewable = new DatabasePollingViewable(1, inputProperties, supportPollingStrategy, dataCache, resultEventType);

        Map<Integer, List<ExprNode>> sqlParameters = new HashMap<Integer, List<ExprNode>>();
        sqlParameters.put(1, Collections.singletonList((ExprNode) new ExprIdentNodeImpl("intPrimitive", "s0")));
        pollingViewable.validate(SupportEngineImportServiceFactory.make(), new SupportStreamTypeSvc3Stream(), null, null, null, null, null, null, null, sqlParameters, null, SupportStatementContextFactory.makeContext());

        indexingStrategy = new PollResultIndexingStrategy() {
            public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, StatementContext statementContext) {
                return new EventTable[]{new UnindexedEventTableList(pollResult, -1)};
            }

            public String toQueryPlan() {
                return this.getClass().getSimpleName() + " unindexed";
            }
        };
    }

    public void testPoll() {
        EventBean[][] input = new EventBean[2][2];
        input[0] = new EventBean[]{makeEvent(-1), null};
        input[1] = new EventBean[]{makeEvent(500), null};
        EventTable[][] resultRows = pollingViewable.poll(input, indexingStrategy, null);

        // should have joined to two rows
        assertEquals(2, resultRows.length);
        assertTrue(resultRows[0][0].isEmpty());
        assertTrue(resultRows[1][0].isEmpty());
    }

    private EventBean makeEvent(int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        return SupportEventAdapterService.getService().adapterForBean(bean);
    }
}
