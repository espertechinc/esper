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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.EPOnDemandPreparedQuery;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecNWTableInfraIndexFAFPerf implements RegressionExecution, IndexBackingTableInfo {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(MyEvent.class);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("justCount", InvocationCounter.class.getName(), "justCount");

        runAssertionFAFKeyBTreePerformance(epService, true);
        runAssertionFAFKeyBTreePerformance(epService, false);

        runAssertionFAFKeyAndRangePerformance(epService, true);
        runAssertionFAFKeyAndRangePerformance(epService, false);

        runAssertionFAFRangePerformance(epService, true);
        runAssertionFAFRangePerformance(epService, false);

        runAssertionFAFKeyPerformance(epService, true);
        runAssertionFAFKeyPerformance(epService, false);

        runAssertionFAFInKeywordSingleIndex(epService, true);
        runAssertionFAFInKeywordSingleIndex(epService, false);
    }

    private void runAssertionFAFKeyBTreePerformance(EPServiceProvider epService, boolean namedWindow) {
        // create window one
        String eplCreate = namedWindow ?
                "create window MyInfraFAFKB#keepall as SupportBean" :
                "create table MyInfraFAFKB (theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraFAFKB select theString, intPrimitive from SupportBean");
        EPStatement idx = epService.getEPAdministrator().createEPL("create index idx1 on MyInfraFAFKB(intPrimitive btree)");

        // insert X rows
        int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
        for (int i = 0; i < maxRows; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("A", i));
        }
        epService.getEPRuntime().sendEvent(new SupportBean("B", 100));

        // fire single-key queries
        String eplIdx1One = "select intPrimitive as sumi from MyInfraFAFKB where intPrimitive = 5501";
        runFAFAssertion(epService, eplIdx1One, 5501);

        String eplIdx1Two = "select sum(intPrimitive) as sumi from MyInfraFAFKB where intPrimitive > 9997";
        runFAFAssertion(epService, eplIdx1Two, 9998 + 9999);

        // drop index, create multikey btree
        idx.destroy();
        epService.getEPAdministrator().createEPL("create index idx2 on MyInfraFAFKB(intPrimitive btree, theString btree)");

        String eplIdx2One = "select intPrimitive as sumi from MyInfraFAFKB where intPrimitive = 5501 and theString = 'A'";
        runFAFAssertion(epService, eplIdx2One, 5501);

        String eplIdx2Two = "select sum(intPrimitive) as sumi from MyInfraFAFKB where intPrimitive in [5000:5004) and theString = 'A'";
        runFAFAssertion(epService, eplIdx2Two, 5000 + 5001 + 5003 + 5002);

        String eplIdx2Three = "select sum(intPrimitive) as sumi from MyInfraFAFKB where intPrimitive=5001 and theString between 'A' and 'B'";
        runFAFAssertion(epService, eplIdx2Three, 5001);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraFAFKB", false);
    }

    private void runFAFAssertion(EPServiceProvider epService, String epl, Integer expected) {
        long start = System.currentTimeMillis();
        int loops = 500;

        EPOnDemandPreparedQuery query = epService.getEPRuntime().prepareQuery(epl);
        for (int i = 0; i < loops; i++) {
            runFAFQuery(query, expected);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1500);
    }

    private void runAssertionFAFKeyAndRangePerformance(EPServiceProvider epService, boolean namedWindow) {
        // create window one
        String eplCreate = namedWindow ?
                "create window MyInfraFAFKR#keepall as SupportBean" :
                "create table MyInfraFAFKR (theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraFAFKR select theString, intPrimitive from SupportBean");
        epService.getEPAdministrator().createEPL("create index idx1 on MyInfraFAFKR(theString hash, intPrimitive btree)");

        // insert X rows
        int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
        for (int i = 0; i < maxRows; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("A", i));
        }

        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in [3:9997]", 1 + 2 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in [3:9997)", 1 + 2 + 9997 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in (3:9997]", 1 + 2 + 3 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive not in (3:9997)", 1 + 2 + 3 + 9997 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'B' and intPrimitive not in (3:9997)", null);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive between 200 and 202", 603);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive between 202 and 199", 199 + 200 + 201 + 202);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive >= 200 and intPrimitive <= 202", 603);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive >= 202 and intPrimitive <= 200", null);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive > 9997", 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive >= 9997", 9997 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive < 5", 4 + 3 + 2 + 1);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive <= 5", 5 + 4 + 3 + 2 + 1);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in [200:202]", 603);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in [200:202)", 401);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in (200:202]", 403);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraFAFKR where theString = 'A' and intPrimitive in (200:202)", 201);

        // test no value returned
        EPOnDemandPreparedQuery query = epService.getEPRuntime().prepareQuery("select * from MyInfraFAFKR where theString = 'A' and intPrimitive < 0");
        EPOnDemandQueryResult result = query.execute();
        assertEquals(0, result.getArray().length);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraFAFKR", false);
    }

    private void runAssertionFAFRangePerformance(EPServiceProvider epService, boolean namedWindow) {
        // create window one
        String eplCreate = namedWindow ?
                "create window MyInfraRP#keepall as SupportBean" :
                "create table MyInfraRP (theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraRP select theString, intPrimitive from SupportBean");
        epService.getEPAdministrator().createEPL("create index idx1 on MyInfraRP(intPrimitive btree)");

        // insert X rows
        int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
        for (int i = 0; i < maxRows; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("K", i));
        }

        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive between 200 and 202", 603);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive between 202 and 199", 199 + 200 + 201 + 202);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive >= 200 and intPrimitive <= 202", 603);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive >= 202 and intPrimitive <= 200", null);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive > 9997", 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive >= 9997", 9997 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive < 5", 4 + 3 + 2 + 1);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive <= 5", 5 + 4 + 3 + 2 + 1);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in [200:202]", 603);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in [200:202)", 401);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in (200:202]", 403);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive in (200:202)", 201);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in [3:9997]", 1 + 2 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in [3:9997)", 1 + 2 + 9997 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in (3:9997]", 1 + 2 + 3 + 9998 + 9999);
        runFAFAssertion(epService, "select sum(intPrimitive) as sumi from MyInfraRP where intPrimitive not in (3:9997)", 1 + 2 + 3 + 9997 + 9998 + 9999);

        // test no value returned
        EPOnDemandPreparedQuery query = epService.getEPRuntime().prepareQuery("select * from MyInfraRP where intPrimitive < 0");
        EPOnDemandQueryResult result = query.execute();
        assertEquals(0, result.getArray().length);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraRP", false);
    }

    private void runAssertionFAFKeyPerformance(EPServiceProvider epService, boolean namedWindow) {
        // create window one
        String stmtTextCreateOne = namedWindow ?
                "create window MyInfraOne#keepall as (f1 string, f2 int)" :
                "create table MyInfraOne (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfraOne(f1, f2) select theString, intPrimitive from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraOneIndex on MyInfraOne(f1)");

        // insert X rows
        int maxRows = 100;   //for performance testing change to int maxRows = 100000;
        for (int i = 0; i < maxRows; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("K" + i, i));
        }

        // fire N queries each returning 1 row
        long start = System.currentTimeMillis();
        String queryText = "select * from MyInfraOne where f1='K10'";
        EPOnDemandPreparedQuery query = epService.getEPRuntime().prepareQuery(queryText);
        int loops = 10000;

        for (int i = 0; i < loops; i++) {
            EPOnDemandQueryResult result = query.execute();
            assertEquals(1, result.getArray().length);
            assertEquals("K10", result.getArray()[0].get("f1"));
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 500);

        // test no value returned
        queryText = "select * from MyInfraOne where f1='KX'";
        query = epService.getEPRuntime().prepareQuery(queryText);
        EPOnDemandQueryResult result = query.execute();
        assertEquals(0, result.getArray().length);

        // test query null
        queryText = "select * from MyInfraOne where f1=null";
        query = epService.getEPRuntime().prepareQuery(queryText);
        result = query.execute();
        assertEquals(0, result.getArray().length);

        // insert null and test null
        epService.getEPRuntime().sendEvent(new SupportBean(null, -2));
        result = query.execute();
        assertEquals(0, result.getArray().length);

        // test two values
        epService.getEPRuntime().sendEvent(new SupportBean(null, -1));
        query = epService.getEPRuntime().prepareQuery("select * from MyInfraOne where f1 is null order by f2 asc");
        result = query.execute();
        assertEquals(2, result.getArray().length);
        assertEquals(-2, result.getArray()[0].get("f2"));
        assertEquals(-1, result.getArray()[1].get("f2"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraOne", false);
    }

    private void runFAFQuery(EPOnDemandPreparedQuery query, Integer expectedValue) {
        EPOnDemandQueryResult result = query.execute();
        assertEquals(1, result.getArray().length);
        assertEquals(expectedValue, result.getArray()[0].get("sumi"));
    }

    private void runAssertionFAFInKeywordSingleIndex(EPServiceProvider epService, boolean namedWindow) throws Exception {
        String eplCreate = namedWindow ?
                "create window MyInfraIKW#keepall as MyEvent" :
                "create table MyInfraIKW (id string primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("create index idx on MyInfraIKW(id)");
        epService.getEPAdministrator().createEPL("insert into MyInfraIKW select id from MyEvent");

        int eventCount = 10;
        for (int i = 0; i < eventCount; i++) {
            epService.getEPRuntime().sendEvent(new MyEvent("E" + i));
        }

        InvocationCounter.setCount(0);
        String fafEPL = "select * from MyInfraIKW as mw where justCount(mw) and id in ('notfound')";
        epService.getEPRuntime().executeQuery(fafEPL);
        assertEquals(0, InvocationCounter.getCount());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraIKW", false);
    }

    public static class MyEvent {
        private String id;

        public MyEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class InvocationCounter {
        private static int count;

        public static void setCount(int count) {
            InvocationCounter.count = count;
        }

        public static int getCount() {
            return count;
        }

        public static boolean justCount(Object o) {
            count++;
            return true;
        }
    }
}
