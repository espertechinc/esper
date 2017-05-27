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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportSimpleBeanOne;
import com.espertech.esper.supportregression.bean.SupportSimpleBeanTwo;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexAssertionEventSend;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;

public class ExecSubselectIndex implements RegressionExecution, IndexBackingTableInfo {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionIndexChoicesOverdefinedWhere(epService);
        runAssertionUniqueIndexCorrelated(epService);
    }

    private void runAssertionIndexChoicesOverdefinedWhere(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        // test no where clause with unique
        IndexAssertionEventSend assertNoWhere = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "c0,c1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 1, 2, 3));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EX", 10, 11, 12));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EX", "E1"});
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 1, 2, 3));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EY", 10, 11, 12));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EY", null});
            }
        };
        tryAssertion(epService, listener, false, "s2,i2", "", BACKING_UNINDEXED, assertNoWhere);

        // test no where clause with unique on multiple props, exact specification of where-clause
        IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "c0,c1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 1, 3, 10));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 1, 2, 0));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E3", 1, 3, 9));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EX", 1, 3, 9));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EX", "E3"});
            }
        };
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1", BACKING_MULTI_DUPS, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.d2 = ssb1.d1", BACKING_SINGLE_DUPS, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", BACKING_COMPOSITE, assertSendEvents);
        tryAssertion(epService, listener, false, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_DUPS, assertSendEvents);
        tryAssertion(epService, listener, false, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.s2 between 'E3' and 'E4'", BACKING_MULTI_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, false, "l2", "where ssb2.l2 = ssb1.l1", BACKING_SINGLE_UNIQUE, assertSendEvents);
        tryAssertion(epService, listener, true, "l2", "where ssb2.l2 = ssb1.l1", BACKING_SINGLE_DUPS, assertSendEvents);
        tryAssertion(epService, listener, false, "l2", "where ssb2.l2 = ssb1.l1 and ssb1.i1 between 1 and 20", BACKING_SINGLE_UNIQUE, assertSendEvents);
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener, boolean disableImplicitUniqueIdx, String uniqueFields, String whereClause, String backingTable, IndexAssertionEventSend assertion) {
        SupportQueryPlanIndexHook.reset();
        String eplUnique = INDEX_CALLBACK_HOOK + "select s1 as c0, " +
                "(select s2 from SSB2#unique(" + uniqueFields + ") as ssb2 " + whereClause + ") as c1 " +
                "from SSB1 as ssb1";
        if (disableImplicitUniqueIdx) {
            eplUnique = "@Hint('DISABLE_UNIQUE_IMPLICIT_IDX')" + eplUnique;
        }
        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        stmtUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, null, backingTable);

        assertion.run();

        stmtUnique.destroy();
    }

    private void runAssertionUniqueIndexCorrelated(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        String[] fields = "c0,c1".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();

        // test std:unique
        SupportQueryPlanIndexHook.reset();
        String eplUnique = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select intPrimitive from SupportBean#unique(theString) where theString = s0.p00) as c1 " +
                "from S0 as s0";
        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        stmtUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, null, BACKING_SINGLE_UNIQUE);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 4});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 3});

        stmtUnique.destroy();

        // test std:firstunique
        SupportQueryPlanIndexHook.reset();
        String eplFirstUnique = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select intPrimitive from SupportBean#firstunique(theString) where theString = s0.p00) as c1 " +
                "from S0 as s0";
        EPStatement stmtFirstUnique = epService.getEPAdministrator().createEPL(eplFirstUnique);
        stmtFirstUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, null, BACKING_SINGLE_UNIQUE);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 1});

        stmtFirstUnique.destroy();

        // test intersection std:firstunique
        SupportQueryPlanIndexHook.reset();
        String eplIntersection = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select intPrimitive from SupportBean#time(1)#unique(theString) where theString = s0.p00) as c1 " +
                "from S0 as s0";
        EPStatement stmtIntersection = epService.getEPAdministrator().createEPL(eplIntersection);
        stmtIntersection.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, null, BACKING_SINGLE_UNIQUE);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 4});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 3});

        stmtIntersection.destroy();

        // test grouped unique
        SupportQueryPlanIndexHook.reset();
        String eplGrouped = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select longPrimitive from SupportBean#groupwin(theString)#unique(intPrimitive) where theString = s0.p00 and intPrimitive = s0.id) as c1 " +
                "from S0 as s0";
        EPStatement stmtGrouped = epService.getEPAdministrator().createEPL(eplGrouped);
        stmtGrouped.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, null, BACKING_MULTI_UNIQUE);

        epService.getEPRuntime().sendEvent(makeBean("E1", 1, 100));
        epService.getEPRuntime().sendEvent(makeBean("E1", 2, 101));
        epService.getEPRuntime().sendEvent(makeBean("E1", 1, 102));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 102L});

        stmtGrouped.destroy();
    }

    private SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }
}
