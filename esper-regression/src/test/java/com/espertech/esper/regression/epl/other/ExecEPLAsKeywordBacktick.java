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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.bookexample.BookDesc;
import com.espertech.esper.supportregression.bean.bookexample.OrderBean;
import com.espertech.esper.supportregression.bean.bookexample.OrderBeanFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEPLAsKeywordBacktick implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);
        epService.getEPAdministrator().getConfiguration().addEventType(OrderBean.class);

        runFromClause(epService);
        runOnTrigger(epService);
        runUpdateIStream(epService);
        runFAFUpdateDelete(epService);
        runOnMergeAndUpdateAndSelect(epService);
        runSubselect(epService);
        runOnSelectProperty(epService);
    }

    private void runOnSelectProperty(EPServiceProvider epService) {
        String stmtText = "on OrderBean insert into ABC select * " +
                "insert into DEF select `order`.reviewId from [books][reviews] `order`";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.destroy();
    }

    private void runSubselect(EPServiceProvider epService) {
        String epl = "select (select `order`.p00 from SupportBean_S0#lastevent as `order`) as c0 from SupportBean_S1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        assertEquals("A", listener.assertOneGetNewAndReset().get("c0"));

        stmt.destroy();
    }

    private void runOnMergeAndUpdateAndSelect(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowMerge#keepall as (p0 string, p1 string)");
        epService.getEPRuntime().executeQuery("insert into MyWindowMerge select 'a' as p0, 'b' as p1");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 merge MyWindowMerge as `order` when matched then update set `order`.p1 = `order`.p0");
        epService.getEPAdministrator().createEPL("on SupportBean_S1 update MyWindowMerge as `order` set p0 = 'x'");

        assertFAF(epService, "MyWindowMerge", "a", "b");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFAF(epService, "MyWindowMerge", "a", "a");

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "x"));
        assertFAF(epService, "MyWindowMerge", "x", "a");

        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean select `order`.p0 as c0 from MyWindowMerge as `order`");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals("x", listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runFAFUpdateDelete(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create window MyWindowFAF#keepall as (p0 string, p1 string)");
        epService.getEPRuntime().executeQuery("insert into MyWindowFAF select 'a' as p0, 'b' as p1");
        assertFAF(epService, "MyWindowFAF", "a", "b");

        epService.getEPRuntime().executeQuery("update MyWindowFAF as `order` set `order`.p0 = `order`.p1");
        assertFAF(epService, "MyWindowFAF", "b", "b");

        epService.getEPRuntime().executeQuery("delete from MyWindowFAF as `order` where `order`.p0 = 'b'");
        assertEquals(0, epService.getEPRuntime().executeQuery("select * from MyWindowFAF").getArray().length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertFAF(EPServiceProvider epService, String windowName, String p0, String p1) {
        EPAssertionUtil.assertProps(epService.getEPRuntime().executeQuery("select * from " + windowName).getArray()[0], "p0,p1".split(","), new Object[] {p0, p1});
    }

    private void runUpdateIStream(EPServiceProvider epService) {
        EPStatement stmtUpd = epService.getEPAdministrator().createEPL("update istream SupportBean_S0 as `order` set p00=`order`.p01");

        String epl = "select * from SupportBean_S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a", "x"));
        assertEquals("x", listener.assertOneGetNewAndReset().get("p00"));

        stmtUpd.destroy();
        stmt.destroy();
    }

    private void runOnTrigger(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table MyTable(k1 string primary key, v1 string)");
        epService.getEPRuntime().executeQuery("insert into MyTable select 'x' as k1, 'y' as v1");
        epService.getEPRuntime().executeQuery("insert into MyTable select 'a' as k1, 'b' as v1");

        String epl = "on SupportBean_S0 as `order` select v1 from MyTable where `order`.p00 = k1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a"));
        assertEquals("b", listener.assertOneGetNewAndReset().get("v1"));

        stmt.destroy();
    }

    private void runFromClause(EPServiceProvider epService) throws Exception {
        String epl = "select * from SupportBean_S0#lastevent as `order`, SupportBean_S1#lastevent as `select`";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean_S0 s0 = new SupportBean_S0(1, "S0_1");
        SupportBean_S1 s1 = new SupportBean_S1(10, "S1_1");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "order,select,order.p00,select.p10".split(","), new Object[] {s0, s1, "S0_1", "S1_1"});

        stmt.destroy();
    }
}
