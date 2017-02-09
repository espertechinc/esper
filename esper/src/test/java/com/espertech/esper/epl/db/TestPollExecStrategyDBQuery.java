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
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.supportunit.epl.SupportDatabaseService;
import junit.framework.TestCase;

import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestPollExecStrategyDBQuery extends TestCase {
    private PollExecStrategyDBQuery dbPollExecStrategy;

    public void setUp() throws Exception {
        String sql = "select myvarchar from mytesttable where mynumeric = ? order by mybigint asc";

        DatabaseConnectionFactory databaseConnectionFactory = SupportDatabaseService.makeService().getConnectionFactory("mydb");
        ConnectionCache connectionCache = new ConnectionNoCacheImpl(databaseConnectionFactory, sql);

        Map<String, Object> resultProperties = new HashMap<String, Object>();
        resultProperties.put("myvarchar", String.class);
        EventType resultEventType = SupportEventAdapterService.getService().createAnonymousMapType("test", resultProperties, true);

        Map<String, DBOutputTypeDesc> propertiesOut = new HashMap<String, DBOutputTypeDesc>();
        propertiesOut.put("myvarchar", new DBOutputTypeDesc(Types.VARCHAR, null, null));

        dbPollExecStrategy = new PollExecStrategyDBQuery(SupportEventAdapterService.getService(),
                resultEventType, connectionCache, sql, propertiesOut, null, null, false);
    }

    public void testPoll() {
        dbPollExecStrategy.start();

        List<EventBean>[] resultRows = new LinkedList[3];
        resultRows[0] = dbPollExecStrategy.poll(new Object[]{-1}, null);
        resultRows[1] = dbPollExecStrategy.poll(new Object[]{500}, null);
        resultRows[2] = dbPollExecStrategy.poll(new Object[]{200}, null);

        // should have joined to two rows
        assertEquals(0, resultRows[0].size());
        assertEquals(2, resultRows[1].size());
        assertEquals(1, resultRows[2].size());

        EventBean theEvent = resultRows[1].get(0);
        assertEquals("D", theEvent.get("myvarchar"));

        theEvent = resultRows[1].get(1);
        assertEquals("E", theEvent.get("myvarchar"));

        theEvent = resultRows[2].get(0);
        assertEquals("F", theEvent.get("myvarchar"));

        dbPollExecStrategy.done();
        dbPollExecStrategy.destroy();
    }
}
