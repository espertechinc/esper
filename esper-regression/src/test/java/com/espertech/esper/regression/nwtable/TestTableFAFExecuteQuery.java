/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import junit.framework.TestCase;

public class TestTableFAFExecuteQuery extends TestCase implements IndexBackingTableInfo
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        config.addEventType("SupportBean", SupportBean.class.getName());
        config.addEventType("SupportBean_A", SupportBean_A.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testFAFInsert() {
        String[] propertyNames = "p0,p1".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("create table MyTable as (p0 string, p1 int)");

        String eplInsertInto = "insert into MyTable (p0, p1) select 'a', 1";
        EPOnDemandQueryResult resultOne = epService.getEPRuntime().executeQuery(eplInsertInto);
        assertFAFInsertResult(resultOne, propertyNames, stmt);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}});
    }

    public void testFAFDelete() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("create table MyTable as (p0 string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable select theString, sum(intPrimitive) as thesum from SupportBean group by theString");
        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("G" + i, i));
        }
        assertEquals(10L, getTableCount(stmt));
        epService.getEPRuntime().executeQuery("delete from MyTable");
        assertEquals(0L, getTableCount(stmt));
    }

    public void testFAFUpdate() {
        String[] fields = "p0,p1".split(",");
        epService.getEPAdministrator().createEPL("@Name('TheTable') create table MyTable as (p0 string primary key, p1 string, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable select theString, sum(intPrimitive) as thesum from SupportBean group by theString");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("update MyTable set p1 = 'ABC'");
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("TheTable").iterator(), fields, new Object[][]{{"E1", "ABC"}, {"E2", "ABC"}});
    }

    public void testFAFSelect() {
        String[] fields = "p0".split(",");
        epService.getEPAdministrator().createEPL("@Name('TheTable') create table MyTable as (p0 string primary key, thesum sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable select theString, sum(intPrimitive) as thesum from SupportBean group by theString");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyTable");
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, new Object[][] {{"E1"}, {"E2"}});
    }

    private long getTableCount(EPStatement stmt) {
        return EPAssertionUtil.iteratorCount(stmt.iterator());
    }

    private void assertFAFInsertResult(EPOnDemandQueryResult resultOne, String[] propertyNames, EPStatement stmt) {
        assertEquals(0, resultOne.getArray().length);
        assertSame(resultOne.getEventType(), stmt.getEventType());
    }
}
