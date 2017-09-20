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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecContextPartitionedAggregate implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        configuration.addPlugInSingleRowFunction("toArray", this.getClass().getName(), "toArray");
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAccessOnly(epService);
        runAssertionSegmentedSubqueryWithAggregation(epService);
        runAssertionRowPerGroupStream(epService);
        runAssertionRowPerGroupBatchContextProp(epService);
        runAssertionRowPerGroupWithAccess(epService);
        runAssertionRowForAll(epService);
        runAssertionRowPerGroupUnidirectionalJoin(epService);
    }

    private void runAssertionAccessOnly(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String eplContext = "@Name('CTX') create context SegmentedByString partition by theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplContext);

        String[] fieldsGrouped = "theString,intPrimitive,col1".split(",");
        String eplGroupedAccess = "@Name('S2') context SegmentedByString select theString,intPrimitive,window(longPrimitive) as col1 from SupportBean#keepall sb group by intPrimitive";
        epService.getEPAdministrator().createEPL(eplGroupedAccess);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("S2").addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("G1", 1, 10L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G1", 1, new Object[]{10L}});

        epService.getEPRuntime().sendEvent(makeEvent("G1", 2, 100L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G1", 2, new Object[]{100L}});

        epService.getEPRuntime().sendEvent(makeEvent("G2", 1, 200L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G2", 1, new Object[]{200L}});

        epService.getEPRuntime().sendEvent(makeEvent("G1", 1, 11L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsGrouped, new Object[]{"G1", 1, new Object[]{10L, 11L}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSegmentedSubqueryWithAggregation(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fields = new String[]{"theString", "intPrimitive", "val0"};
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select theString, intPrimitive, (select count(*) from SupportBean_S0#keepall as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "s1"));
        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, 0L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRowPerGroupStream(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fieldsOne = "intPrimitive,count(*)".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString select intPrimitive, count(*) from SupportBean group by intPrimitive");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{200, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{11, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{200, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L});

        stmtOne.destroy();

        // add "string" : a context property
        String[] fieldsTwo = "theString,intPrimitive,count(*)".split(",");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('B') context SegmentedByString select theString, intPrimitive, count(*) from SupportBean group by intPrimitive");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G1", 10, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 200, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G1", 10, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G1", 11, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 200, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 10, 1L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRowPerGroupBatchContextProp(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fieldsOne = "intPrimitive,count(*)".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString select intPrimitive, count(*) from SupportBean#length_batch(2) group by intPrimitive order by intPrimitive asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsOne, new Object[]{10, 1L});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[1], fieldsOne, new Object[]{11, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{200, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsOne, new Object[]{10, 2L});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[1], fieldsOne, new Object[]{11, 0L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsOne, new Object[]{10, 2L});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[1], fieldsOne, new Object[]{200, 0L});

        stmtOne.destroy();

        // add "string" : add context property
        String[] fieldsTwo = "theString,intPrimitive,count(*)".split(",");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('B') context SegmentedByString select theString, intPrimitive, count(*) from SupportBean#length_batch(2) group by intPrimitive order by theString, intPrimitive asc");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsTwo, new Object[]{"G1", 10, 1L});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[1], fieldsTwo, new Object[]{"G1", 11, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"G2", 200, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsTwo, new Object[]{"G1", 10, 2L});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[1], fieldsTwo, new Object[]{"G1", 11, 0L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 10));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsTwo, new Object[]{"G2", 10, 2L});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[1], fieldsTwo, new Object[]{"G2", 200, 0L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRowPerGroupWithAccess(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fieldsOne = "intPrimitive,col1,col2,col3".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select intPrimitive, count(*) as col1, toArray(window(*).selectFrom(v=>v.longPrimitive)) as col2, first().longPrimitive as col3 " +
                "from SupportBean#keepall as sb " +
                "group by intPrimitive order by intPrimitive asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("G1", 10, 200L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L, new Object[]{200L}, 200L});

        epService.getEPRuntime().sendEvent(makeEvent("G1", 10, 300L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L, new Object[]{200L, 300L}, 200L});

        epService.getEPRuntime().sendEvent(makeEvent("G2", 10, 1000L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 1L, new Object[]{1000L}, 1000L});

        epService.getEPRuntime().sendEvent(makeEvent("G2", 10, 1010L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L, new Object[]{1000L, 1010L}, 1000L});

        stmtOne.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRowForAll(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        // test aggregation-only (no access)
        String[] fieldsOne = "col1".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select sum(intPrimitive) as col1 " +
                "from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{3});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{2});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{7});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{3});

        epService.getEPRuntime().sendEvent(new SupportBean("G3", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{-1});

        stmtOne.destroy();

        // test mixed with access
        String[] fieldsTwo = "col1,col2".split(",");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select sum(intPrimitive) as col1, toArray(window(*).selectFrom(v=>v.intPrimitive)) as col2 " +
                "from SupportBean#keepall");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 8));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{8, new Object[]{8}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{5, new Object[]{5}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{9, new Object[]{8, 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{7, new Object[]{5, 2}});

        stmtTwo.destroy();

        // test only access
        String[] fieldsThree = "col1".split(",");
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select toArray(window(*).selectFrom(v=>v.intPrimitive)) as col1 " +
                "from SupportBean#keepall");
        stmtThree.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 8));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{8}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{5}});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{8, 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsThree, new Object[]{new Object[]{5, 2}});

        stmtThree.destroy();

        // test subscriber
        EPStatement stmtFour = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select count(*) as col1 " +
                "from SupportBean");
        SupportSubscriber subs = new SupportSubscriber();
        stmtFour.setSubscriber(subs);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        assertEquals(1L, subs.assertOneGetNewAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 1));
        assertEquals(2L, subs.assertOneGetNewAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 2));
        assertEquals(1L, subs.assertOneGetNewAndReset());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRowPerGroupUnidirectionalJoin(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@Name('context') create context SegmentedByString partition by theString from SupportBean");

        String[] fieldsOne = "intPrimitive,col1".split(",");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context SegmentedByString " +
                "select intPrimitive, count(*) as col1 " +
                "from SupportBean unidirectional, SupportBean_S0#keepall " +
                "group by intPrimitive order by intPrimitive asc");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 3L});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{20, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5));

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{20, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{10, 5L});

        stmtOne.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static Object toArray(Collection input) {
        return input.toArray();
    }
}
