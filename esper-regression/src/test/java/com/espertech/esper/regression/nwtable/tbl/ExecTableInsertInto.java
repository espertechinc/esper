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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ExecTableInsertInto implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionInsertIntoSelfAccess(epService);
        runAssertionNamedWindowMergeInsertIntoTable(epService);
        runAssertionSplitStream(epService);
        runAssertionInsertIntoFromNamedWindow(epService);
        runAssertionInsertInto(epService);
        runAssertionInsertIntoWildcard(epService);
    }

    private void runAssertionInsertIntoSelfAccess(EPServiceProvider epService) {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableIISA(pkey string primary key)");
        epService.getEPAdministrator().createEPL("insert into MyTableIISA select theString as pkey from SupportBean where MyTableIISA[theString] is null");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey".split(","), new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey".split(","), new Object[][]{{"E1"}, {"E2"}});
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNamedWindowMergeInsertIntoTable(EPServiceProvider epService) {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableNWM(pkey string)");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean as sb merge MyWindow when not matched " +
                "then insert into MyTableNWM select sb.theString as pkey");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), "pkey".split(","), new Object[][]{{"E1"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSplitStream(EPServiceProvider epService) {
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(
                "create table MyTableOne(pkey string primary key, col int)");
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(
                "create table MyTableTwo(pkey string primary key, col int)");

        String eplSplit = "on SupportBean \n" +
                "  insert into MyTableOne select theString as pkey, intPrimitive as col where intPrimitive > 0\n" +
                "  insert into MyTableTwo select theString as pkey, intPrimitive as col where intPrimitive < 0\n" +
                "  insert into OtherStream select theString as pkey, intPrimitive as col where intPrimitive = 0\n";
        epService.getEPAdministrator().createEPL(eplSplit);

        SupportUpdateListener otherStreamListener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from OtherStream").addListener(otherStreamListener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertSplitStream(stmtCreateOne, stmtCreateTwo, new Object[][]{{"E1", 1}}, new Object[0][]);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", -2));
        assertSplitStream(stmtCreateOne, stmtCreateTwo, new Object[][]{{"E1", 1}}, new Object[][]{{"E2", -2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", -3));
        assertSplitStream(stmtCreateOne, stmtCreateTwo, new Object[][]{{"E1", 1}}, new Object[][]{{"E2", -2}, {"E3", -3}});
        assertFalse(otherStreamListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        assertSplitStream(stmtCreateOne, stmtCreateTwo, new Object[][]{{"E1", 1}}, new Object[][]{{"E2", -2}, {"E3", -3}});
        EPAssertionUtil.assertProps(otherStreamListener.assertOneGetNewAndReset(), "pkey,col".split(","), new Object[]{"E4", 0});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertSplitStream(EPStatement stmtCreateOne, EPStatement stmtCreateTwo, Object[][] tableOneRows, Object[][] tableTwoRows) {
        String[] fields = "pkey,col".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreateOne.iterator(), fields, tableOneRows);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreateTwo.iterator(), fields, tableTwoRows);
    }

    private void runAssertionInsertIntoFromNamedWindow(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindow#unique(theString) as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableIIF(pkey0 string primary key, pkey1 int primary key)");
        epService.getEPAdministrator().createEPL("on SupportBean_S1 insert into MyTableIIF select theString as pkey0, intPrimitive as pkey1 from MyWindow");
        String[] fields = "pkey0,pkey1".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10}});

        epService.getEPRuntime().executeQuery("delete from MyTableIIF");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});
    }

    private void runAssertionInsertInto(EPServiceProvider epService) {
        runInsertIntoKeyed(epService);

        runInsertIntoUnKeyed(epService);
    }

    private void runInsertIntoUnKeyed(EPServiceProvider epService) {
        String[] fields = "theString".split(",");
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableIIU(theString string)");
        epService.getEPAdministrator().createEPL("@name('tbl-insert') insert into MyTableIIU select theString from SupportBean");

        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[0][]);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}});

        try {
            epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "java.lang.RuntimeException: Unexpected exception in statement 'tbl-insert': Unique index violation, table 'MyTableIIU' is a declared to hold a single un-keyed row");
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runInsertIntoKeyed(EPServiceProvider epService) {
        String[] fields = "pkey,thesum".split(",");
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create table MyTableIIK(" +
                "pkey string primary key," +
                "thesum sum(int))");
        epService.getEPAdministrator().createEPL("insert into MyTableIIK select theString as pkey from SupportBean");
        epService.getEPAdministrator().createEPL("into table MyTableIIK select sum(id) as thesum from SupportBean_S0 group by p00");
        epService.getEPAdministrator().createEPL("on SupportBean_S1 insert into MyTableIIK select p10 as pkey");
        epService.getEPAdministrator().createEPL("on SupportBean_S2 merge MyTableIIK where p20 = pkey when not matched then insert into MyTableIIK select p20 as pkey");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", null}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E1"));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10}, {"E2", null}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

        // assert on-insert and on-merge
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(0, "E4"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "E4"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtCreate.iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}, {"E3", 3}, {"E4", 4}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_MyTableIIK__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_MyTableIIK__public", false);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInsertIntoWildcard(EPServiceProvider epService) {
        tryAssertionWildcard(epService, true, null);
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionWildcard(epService, false, rep);
        }
    }

    private void tryAssertionWildcard(EPServiceProvider epService, boolean bean, EventRepresentationChoice rep) {
        if (bean) {
            epService.getEPAdministrator().createEPL("create schema MySchema as " + MyP0P1Event.class.getName());
        } else {
            epService.getEPAdministrator().createEPL("create " + rep.getOutputTypeCreateSchemaName() + " schema MySchema (p0 string, p1 string)");
        }

        EPStatement stmtTheTable = epService.getEPAdministrator().createEPL("create table TheTable (p0 string, p1 string)");
        epService.getEPAdministrator().createEPL("insert into TheTable select * from MySchema");

        if (bean) {
            epService.getEPRuntime().sendEvent(new MyP0P1Event("a", "b"));
        } else if (rep.isMapEvent()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("p0", "a");
            map.put("p1", "b");
            epService.getEPRuntime().sendEvent(map, "MySchema");
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"a", "b"}, "MySchema");
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MySchema"));
            theEvent.put("p0", "a");
            theEvent.put("p1", "b");
            epService.getEPRuntime().sendEventAvro(theEvent, "MySchema");
        }
        EPAssertionUtil.assertProps(stmtTheTable.iterator().next(), "p0,p1".split(","), new Object[]{"a", "b"});
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MySchema", false);

        epService.getEPAdministrator().destroyAllStatements();
    }

    public static class MyP0P1Event {
        private final String p0;
        private final String p1;

        private MyP0P1Event(String p0, String p1) {
            this.p0 = p0;
            this.p1 = p1;
        }

        public String getP0() {
            return p0;
        }

        public String getP1() {
            return p1;
        }
    }
}
