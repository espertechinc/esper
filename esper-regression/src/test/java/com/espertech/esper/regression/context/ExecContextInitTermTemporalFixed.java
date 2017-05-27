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

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionSelectorSegmented;
import com.espertech.esper.client.context.InvalidContextPartitionSelector;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.context.SupportSelectorById;
import com.espertech.esper.supportregression.context.SupportSelectorFilteredInitTerm;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.AgentInstanceAssertionUtil;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecContextInitTermTemporalFixed implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configuration.addDatabaseReference("MyDB", configDB);
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionContextPartitionSelection(epService);
        runAssertionFilterStartedFilterEndedCorrelatedOutputSnapshot(epService);
        runAssertionFilterStartedPatternEndedCorrelated(epService);
        runAssertionStartAfterEndAfter(epService);
        runAssertionFilterStartedFilterEndedOutputSnapshot(epService);
        runAssertionPatternStartedPatternEnded(epService);
        runAssertionContextCreateDestroy(epService);
        runAssertionDBHistorical(epService);
        runAssertionPrevPriorAndAggregation(epService);
        runAssertionJoin(epService);
        runAssertionPatternWithTime(epService);
        runAssertionSubselect(epService);
        runAssertionNWSameContextOnExpr(epService);
        runAssertionNWFireAndForget(epService);
        runAssertionStartTurnedOff(epService);
        runAssertionStartTurnedOn(epService);
    }

    private void runAssertionContextPartitionSelection(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3".split(",");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("create context MyCtx as start SupportBean_S0 s0 end SupportBean_S1(id=s0.id)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("context MyCtx select context.id as c0, context.s0.p00 as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#keepall group by theString");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 101));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        Object[][] expected = new Object[][]{{0, "S0_1", "E1", 6}, {0, "S0_1", "E2", 10}, {0, "S0_1", "E3", 201}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), stmt.safeIterator(), fields, expected);

        // test iterator targeted by context partition id
        SupportSelectorById selectorById = new SupportSelectorById(Collections.singleton(0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(selectorById), stmt.safeIterator(selectorById), fields, expected);

        // test iterator targeted by property on triggering event
        SupportSelectorFilteredInitTerm filtered = new SupportSelectorFilteredInitTerm("S0_1");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(filtered), stmt.safeIterator(filtered), fields, expected);
        filtered = new SupportSelectorFilteredInitTerm("S0_2");
        assertFalse(stmt.iterator(filtered).hasNext());

        // test always-false filter - compare context partition info
        filtered = new SupportSelectorFilteredInitTerm(null);
        assertFalse(stmt.iterator(filtered).hasNext());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{1000L}, filtered.getContextsStartTimes());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"S0_1"}, filtered.getP00PropertyValues());

        try {
            stmt.iterator(new ContextPartitionSelectorSegmented() {
                public List<Object[]> getPartitionKeys() {
                    return null;
                }
            });
            fail();
        } catch (InvalidContextPartitionSelector ex) {
            assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById] interfaces but received com."));
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterStartedFilterEndedCorrelatedOutputSnapshot(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as " +
                "start SupportBean_S0 as s0 " +
                "end SupportBean_S1(p10 = s0.p00) as s1");

        String[] fields = "c1,c2,c3".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.s0.id as c1, context.s1.id as c2, sum(intPrimitive) as c3 " +
                "from SupportBean#keepall output snapshot when terminated");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "G1"));    // starts it
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "GX"));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "G1"));  // terminate
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100, 200, 5});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "G2"));    // starts new one
        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "G3"));    // ignored

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(201, "G2"));  // terminate
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{101, 201, 15});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterStartedPatternEndedCorrelated(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as " +
                "start SupportBean_S0 as s0 " +
                "end pattern [SupportBean_S1(p10 = s0.p00)]");

        String[] fields = "c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "G1"));    // starts it
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "GX"));  // false terminate
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "G1"));  // actual terminate
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "G2"));    // starts second

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 6});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, null));    // false terminate
        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "GY"));    // false terminate

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 7));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G2", 13});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(300, "G2"));  // actual terminate
        epService.getEPRuntime().sendEvent(new SupportBean("E8", 8));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "G3"));    // starts third
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "G3"));    // terminate third

        epService.getEPRuntime().sendEvent(new SupportBean("E9", 9));
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStartAfterEndAfter(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as start after 5 sec end after 10 sec");

        String[] fields = "c1,c2,c3".split(",");
        String[] fieldsShort = "c3".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.startTime as c1, context.endTime as c2, sum(intPrimitive) as c3 " +
                "from SupportBean#keepall");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listener.getAndClearIsInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:05.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{DateTime.parseDefaultMSec("2002-05-1T08:00:05.000"), DateTime.parseDefaultMSec("2002-05-1T08:00:15.000"), 2});

        sendTimeEvent(epService, "2002-05-1T08:00:14.999");

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsShort, new Object[]{5});

        sendTimeEvent(epService, "2002-05-1T08:00:15.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:20.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{DateTime.parseDefaultMSec("2002-05-1T08:00:20.000"), DateTime.parseDefaultMSec("2002-05-1T08:00:30.000"), 5});

        sendTimeEvent(epService, "2002-05-1T08:00:30.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        assertFalse(listener.isInvoked());

        // try variable
        epService.getEPAdministrator().createEPL("create variable int var_start = 10");
        epService.getEPAdministrator().createEPL("create variable int var_end = 20");
        epService.getEPAdministrator().createEPL("create context FrequentlyContext as start after var_start sec end after var_end sec");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFilterStartedFilterEndedOutputSnapshot(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as start SupportBean_S0 as s0 end SupportBean_S1 as s1");

        String[] fields = "c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall output snapshot when terminated");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "S0_1"));    // starts it
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        // terminate
        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "S1_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 5});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(201, "S1_2"));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(102, "S0_2"));    // starts it
        epService.getEPRuntime().sendEvent(new SupportBean_S1(201, "S1_3"));    // ends it
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_2", null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(103, "S0_3"));    // starts it
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 6));           // some more data
        epService.getEPRuntime().sendEvent(new SupportBean_S0(104, "S0_4"));    // ignored
        epService.getEPRuntime().sendEvent(new SupportBean_S1(201, "S1_3"));    // ends it
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_3", 6});

        statement.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternStartedPatternEnded(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EveryNowAndThen as " +
                "start pattern [s0=SupportBean_S0 -> timer:interval(1 sec)] " +
                "end pattern [s1=SupportBean_S1 -> timer:interval(1 sec)]");

        String[] fields = "c1,c2".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "S0_1"));    // starts it
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listener.getAndClearIsInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:01.000"); // 1 second passes

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 4});

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 9});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(101, "S0_2"));    // ignored
        sendTimeEvent(epService, "2002-05-1T08:00:03.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 15});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "S1_1"));    // ignored

        epService.getEPRuntime().sendEvent(new SupportBean("E7", 7));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 22});

        sendTimeEvent(epService, "2002-05-1T08:00:04.000"); // terminates

        epService.getEPRuntime().sendEvent(new SupportBean("E8", 8));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "S1_2"));    // ignored
        sendTimeEvent(epService, "2002-05-1T08:00:10.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E9", 9));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(103, "S0_3"));    // new instance
        sendTimeEvent(epService, "2002-05-1T08:00:11.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E10", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S0_3", 10});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextCreateDestroy(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context EverySecond as start (*, *, *, *, *, *) end (*, *, *, *, *, *)");

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context EverySecond select * from SupportBean");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listener.getAndClearIsInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:00.999");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(listener.getAndClearIsInvoked());

        sendTimeEvent(epService, "2002-05-1T08:00:01.000");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.getAndClearIsInvoked());

        long start = DateTime.parseDefaultMSec("2002-05-1T08:00:01.999");
        for (int i = 0; i < 10; i++) {
            sendTimeEvent(epService, start);

            sendEventAndAssert(epService, listener, false);

            start += 1;
            sendTimeEvent(epService, start);

            sendEventAndAssert(epService, listener, true);

            start += 999;
            sendTimeEvent(epService, start);

            sendEventAndAssert(epService, listener, true);

            start += 1;
            sendTimeEvent(epService, start);

            sendEventAndAssert(epService, listener, false);

            start += 999;
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDBHistorical(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        String[] fields = "s1.mychar".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        String stmtText = "context NineToFive select * from SupportBean_S0 as s0, sql:MyDB ['select * from mytesttable where ${id} = mytesttable.mybigint'] as s1";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"Y"});

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"X"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPrevPriorAndAggregation(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        String[] fields = "col1,col2,col3,col4,col5".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NineToFive " +
                "select prev(theString) as col1, prevwindow(sb) as col2, prevtail(theString) as col3, prior(1, theString) as col4, sum(intPrimitive) as col5 " +
                "from SupportBean#keepall as sb");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        SupportBean event1 = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(event1);
        Object[][] expected = new Object[][]{{null, new SupportBean[]{event1}, "E1", null, 1}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);

        SupportBean event2 = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(event2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", new SupportBean[]{event2, event1}, "E1", "E1", 3});

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);

        // now started
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");

        SupportBean event3 = new SupportBean("E3", 9);
        epService.getEPRuntime().sendEvent(event3);
        expected = new Object[][]{{null, new SupportBean[]{event3}, "E3", null, 9}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, expected);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), statement.safeIterator(), fields, expected);
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1, 0, 3, 1);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoin(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        String[] fields = "col1,col2,col3,col4".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context NineToFive " +
                "select sb.theString as col1, sb.intPrimitive as col2, s0.id as col3, s0.p00 as col4 " +
                "from SupportBean#keepall as sb full outer join SupportBean_S0#keepall as s0 on p00 = theString");
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 1, "E1"});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 5, 1, "E1"});

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");

        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 4, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 4, 2, "E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternWithTime(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement statement = epService.getEPAdministrator().createEPL("context NineToFive select * from pattern[every timer:interval(10 sec)]");
        statement.addListener(listener);
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());   // from the context

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());   // context + pattern
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-1T09:00:10.000");
        assertTrue(listener.isInvoked());

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");
        listener.reset();   // it is not well defined whether the listener does get fired or not
        assertEquals(1, spi.getSchedulingService().getScheduleHandleCount());   // from the context

        // now started
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");
        assertEquals(2, spi.getSchedulingService().getScheduleHandleCount());   // context + pattern
        assertFalse(listener.isInvoked());

        sendTimeEvent(epService, "2002-05-2T09:00:10.000");
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubselect(EPServiceProvider epService) {
        FilterServiceSPI filterSPI = (FilterServiceSPI) ((EPServiceProviderSPI) epService).getFilterService();

        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        String[] fields = "theString,col".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL("context NineToFive select theString, (select p00 from SupportBean_S0#lastevent) as col from SupportBean");
        statement.addListener(listener);
        assertEquals(0, filterSPI.getFilterCountApprox());   // from the context

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        assertEquals(2, filterSPI.getFilterCountApprox());   // from the context

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "S01"));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "S01"});

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");
        assertEquals(0, filterSPI.getFilterCountApprox());   // from the context

        epService.getEPRuntime().sendEvent(new SupportBean("Ex", 0));
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");
        assertEquals(2, filterSPI.getFilterCountApprox());   // from the context
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(12, "S02"));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", "S02"});
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 1, 1, 0, 0);

        // now gone
        sendTimeEvent(epService, "2002-05-2T17:00:00.000");
        assertEquals(0, filterSPI.getFilterCountApprox());   // from the context

        epService.getEPRuntime().sendEvent(new SupportBean("Ey", 0));
        assertFalse(listener.isInvoked());
        AgentInstanceAssertionUtil.assertInstanceCounts(statement.getStatementContext(), 0, 0, 0, 0);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNWSameContextOnExpr(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("makeBean", this.getClass().getName(), "singleRowPluginMakeBean");
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        // no started yet
        String[] fields = "theString,intPrimitive".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("context NineToFive create window MyWindow#keepall as SupportBean");
        stmt.addListener(listener);

        epService.getEPAdministrator().createEPL("context NineToFive insert into MyWindow select * from SupportBean");

        epService.getEPAdministrator().createEPL("context NineToFive " +
                "on SupportBean_S0 s0 merge MyWindow mw where mw.theString = s0.p00 " +
                "when matched then update set intPrimitive = s0.id " +
                "when not matched then insert select makeBean(id, p00)");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.isInvoked());

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "E1"));
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E1", 3});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 1});
        listener.reset();

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");

        // no longer updated
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertFalse(listener.isInvoked());

        // now started again but empty
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNWFireAndForget(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        epService.getEPAdministrator().createEPL("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");

        // no started yet
        epService.getEPAdministrator().createEPL("context NineToFive create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("context NineToFive insert into MyWindow select * from SupportBean");

        // not queryable
        tryInvalidNWQuery(epService);

        // now started
        sendTimeEvent(epService, "2002-05-1T09:00:00.000");
        tryNWQuery(epService, 0);

        // now not empty
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(1, epService.getEPRuntime().executeQuery("select * from MyWindow").getArray().length);

        // now gone
        sendTimeEvent(epService, "2002-05-1T17:00:00.000");

        // no longer queryable
        tryInvalidNWQuery(epService);
        epService.getEPRuntime().sendEvent(new SupportBean());

        // now started again but empty
        sendTimeEvent(epService, "2002-05-2T09:00:00.000");
        tryNWQuery(epService, 0);

        // fill some data
        epService.getEPRuntime().sendEvent(new SupportBean());
        epService.getEPRuntime().sendEvent(new SupportBean());
        sendTimeEvent(epService, "2002-05-2T09:10:00.000");
        tryNWQuery(epService, 2);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryInvalidNWQuery(EPServiceProvider epService) {
        try {
            epService.getEPRuntime().executeQuery("select * from MyWindow");
        } catch (EPException ex) {
            String expected = "Error executing statement: Named window 'MyWindow' is associated to context 'NineToFive' that is not available for querying without context partition selector, use the executeQuery(epl, selector) method instead [select * from MyWindow]";
            assertEquals(expected, ex.getMessage());
        }
    }

    private void tryNWQuery(EPServiceProvider epService, int numRows) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyWindow");
        assertEquals(numRows, result.getArray().length);
    }

    private void runAssertionStartTurnedOff(EPServiceProvider epService) {
        sendTimeEvent(epService, "2002-05-1T08:00:00.000");
        String contextEPL = "@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
        EPStatement stmtContext = epService.getEPAdministrator().createEPL("@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");
        assertContextEventType(stmtContext.getEventType());
        SupportUpdateListener contextListener = new SupportUpdateListener();
        stmtContext.addListener(contextListener);
        stmtContext.setSubscriber(new MiniSubscriber());

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context NineToFive " +
                "select * from SupportBean");
        stmtOne.addListener(new SupportUpdateListener());

        sendTimeAndAssert(epService, "2002-05-1T08:59:30.000", false, 1);
        sendTimeAndAssert(epService, "2002-05-1T08:59:59.999", false, 1);
        sendTimeAndAssert(epService, "2002-05-1T09:00:00.000", true, 1);

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('B') context NineToFive select * from SupportBean");
        stmtTwo.addListener(new SupportUpdateListener());

        sendTimeAndAssert(epService, "2002-05-1T16:59:59.000", true, 2);
        sendTimeAndAssert(epService, "2002-05-1T17:00:00.000", false, 2);

        EPStatement stmtThree = epService.getEPAdministrator().createEPL("@Name('C') context NineToFive select * from SupportBean");
        stmtThree.addListener(new SupportUpdateListener());

        sendTimeAndAssert(epService, "2002-05-2T08:59:59.999", false, 3);
        sendTimeAndAssert(epService, "2002-05-2T09:00:00.000", true, 3);
        sendTimeAndAssert(epService, "2002-05-2T16:59:59.000", true, 3);
        sendTimeAndAssert(epService, "2002-05-2T17:00:00.000", false, 3);

        assertFalse(contextListener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();

        // test SODA
        sendTimeEvent(epService, "2002-05-3T16:59:59.000");
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(contextEPL);
        assertEquals(contextEPL, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(contextEPL, stmt.getText());

        // test built-in properties
        EPStatement stmtLast = epService.getEPAdministrator().createEPL("@Name('A') context NineToFive " +
                "select context.name as c1, context.startTime as c2, context.endTime as c3, theString as c4 from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtLast.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("NineToFive", theEvent.get("c1"));
        assertEquals("2002-05-03T16:59:59.000", DateTime.print(theEvent.get("c2")));
        assertEquals("2002-05-03T17:00:00.000", DateTime.print(theEvent.get("c3")));
        assertEquals("E1", theEvent.get("c4"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStartTurnedOn(EPServiceProvider epService) {

        ContextManagementService ctxMgmtService = ((EPServiceProviderSPI) epService).getContextManagementService();
        assertEquals(0, ctxMgmtService.getContextCount());

        sendTimeEvent(epService, "2002-05-1T09:15:00.000");
        EPStatement stmtContext = epService.getEPAdministrator().createEPL("@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)");
        assertEquals(1, ctxMgmtService.getContextCount());

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("@Name('A') context NineToFive select * from SupportBean");
        stmtOne.addListener(new SupportUpdateListener());

        sendTimeAndAssert(epService, "2002-05-1T09:16:00.000", true, 1);
        sendTimeAndAssert(epService, "2002-05-1T16:59:59.000", true, 1);
        sendTimeAndAssert(epService, "2002-05-1T17:00:00.000", false, 1);

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('B') context NineToFive select * from SupportBean");
        stmtTwo.addListener(new SupportUpdateListener());

        sendTimeAndAssert(epService, "2002-05-2T08:59:59.999", false, 2);
        sendTimeAndAssert(epService, "2002-05-2T09:15:00.000", true, 2);
        sendTimeAndAssert(epService, "2002-05-2T16:59:59.000", true, 2);
        sendTimeAndAssert(epService, "2002-05-2T17:00:00.000", false, 2);

        // destroy context before stmts
        stmtContext.destroy();
        assertEquals(1, ctxMgmtService.getContextCount());

        stmtTwo.destroy();
        stmtOne.destroy();

        // context gone too
        assertEquals(0, ctxMgmtService.getContextCount());
    }

    private void assertContextEventType(EventType eventType) {
        assertEquals(0, eventType.getPropertyNames().length);
        assertEquals("anonymous_EventType_Context_NineToFive", eventType.getName());
    }

    private void sendTimeAndAssert(EPServiceProvider epService, String time, boolean isInvoked, int countStatements) {
        sendTimeEvent(epService, time);
        epService.getEPRuntime().sendEvent(new SupportBean());

        String[] statements = epService.getEPAdministrator().getStatementNames();
        assertEquals(countStatements + 1, statements.length);

        for (String statement : statements) {
            EPStatement stmt = epService.getEPAdministrator().getStatement(statement);
            if (stmt.getName().equals("context")) {
                continue;
            }
            SupportUpdateListener listener = (SupportUpdateListener) stmt.getUpdateListeners().next();
            assertEquals("Failed for statement " + stmt.getName(), isInvoked, listener.getAndClearIsInvoked());
        }
    }

    private void sendTimeEvent(EPServiceProvider epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }

    private void sendTimeEvent(EPServiceProvider epService, long time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(time));
    }

    private void sendEventAndAssert(EPServiceProvider epService, SupportUpdateListener listener, boolean expected) {
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(expected, listener.isInvoked());
        listener.reset();
    }

    public static SupportBean singleRowPluginMakeBean(int id, String p00) {
        return new SupportBean(p00, id);
    }

    public static class MiniSubscriber {
        public static void update() {
            // no action
        }
    }
}
