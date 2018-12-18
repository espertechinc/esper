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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextPartitionSelectorSegmented;
import com.espertech.esper.common.client.context.InvalidContextPartitionSelector;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.context.AgentInstanceAssertionUtil;
import com.espertech.esper.regressionlib.support.context.SupportContextMgmtHelper;
import com.espertech.esper.regressionlib.support.context.SupportSelectorById;
import com.espertech.esper.regressionlib.support.context.SupportSelectorFilteredInitTerm;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.regressionlib.support.util.SupportScheduleHelper;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import junit.framework.TestCase;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ContextInitTermTemporalFixed {

    private static final Logger log = LoggerFactory.getLogger(ContextInitTermTemporalFixed.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextStartEndContextPartitionSelection());
        execs.add(new ContextStartEndFilterStartedFilterEndedCorrelatedOutputSnapshot());
        execs.add(new ContextStartEndFilterStartedPatternEndedCorrelated());
        execs.add(new ContextStartEndStartAfterEndAfter());
        execs.add(new ContextStartEndFilterStartedFilterEndedOutputSnapshot());
        execs.add(new ContextStartEndPatternStartedPatternEnded());
        execs.add(new ContextStartEndContextCreateDestroy());
        execs.add(new ContextStartEndPrevPriorAndAggregation());
        execs.add(new ContextStartEndJoin());
        execs.add(new ContextStartEndPatternWithTime());
        execs.add(new ContextStartEndSubselect());
        execs.add(new ContextStartEndSubselectCorrelated());
        execs.add(new ContextStartEndNWSameContextOnExpr());
        execs.add(new ContextStartEndNWFireAndForget());
        execs.add(new ContextStartEndStartTurnedOff());
        execs.add(new ContextStartEndStartTurnedOn());
        execs.add(new ContextStart9End5AggUngrouped());
        execs.add(new ContextStart9End5AggGrouped());
        execs.add(new ContextStartEndDBHistorical());
        execs.add(new ContextStartEndMultiCrontab());
        return execs;
    }

    private static class ContextStartEndContextPartitionSelection implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            env.advanceTime(0);
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create context MyCtx as start SupportBean_S0 s0 end SupportBean_S1(id=s0.id)", path);
            env.compileDeploy("@name('s0') context MyCtx select context.id as c0, context.s0.p00 as c1, theString as c2, sum(intPrimitive) as c3 from SupportBean#keepall group by theString", path);

            env.advanceTime(1000);
            env.sendEventBean(new SupportBean_S0(1, "S0_1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 10));
            env.sendEventBean(new SupportBean("E1", 2));
            env.sendEventBean(new SupportBean("E3", 100));
            env.sendEventBean(new SupportBean("E3", 101));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 3));
            Object[][] expected = new Object[][]{{0, "S0_1", "E1", 6}, {0, "S0_1", "E2", 10}, {0, "S0_1", "E3", 201}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, expected);

            // test iterator targeted by context partition id
            SupportSelectorById selectorById = new SupportSelectorById(Collections.singleton(0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selectorById), env.statement("s0").safeIterator(selectorById), fields, expected);

            // test iterator targeted by property on triggering event
            SupportSelectorFilteredInitTerm filtered = new SupportSelectorFilteredInitTerm("S0_1");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(filtered), env.statement("s0").safeIterator(filtered), fields, expected);
            filtered = new SupportSelectorFilteredInitTerm("S0_2");
            TestCase.assertFalse(env.statement("s0").iterator(filtered).hasNext());

            // test always-false filter - compare context partition info
            filtered = new SupportSelectorFilteredInitTerm(null);
            TestCase.assertFalse(env.statement("s0").iterator(filtered).hasNext());
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{1000L}, filtered.getContextsStartTimes());
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"S0_1"}, filtered.getP00PropertyValues());

            try {
                env.statement("s0").iterator(new ContextPartitionSelectorSegmented() {
                    public List<Object[]> getPartitionKeys() {
                        return null;
                    }
                });
                fail();
            } catch (InvalidContextPartitionSelector ex) {
                TestCase.assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById] interfaces but received com."));
            }

            env.undeployAll();
        }
    }

    private static class ContextStartEndFilterStartedFilterEndedCorrelatedOutputSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context EveryNowAndThen as " +
                "start SupportBean_S0 as s0 " +
                "end SupportBean_S1(p10 = s0.p00) as s1", path);

            String[] fields = "c1,c2,c3".split(",");
            env.compileDeploy("@name('s0') context EveryNowAndThen select context.s0.id as c1, context.s1.id as c2, sum(intPrimitive) as c3 " +
                "from SupportBean#keepall output snapshot when terminated", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_S0(100, "G1"));    // starts it
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E3", 3));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(200, "GX"));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(200, "G1"));  // terminate
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 200, 5});

            env.sendEventBean(new SupportBean_S0(101, "G2"));    // starts new one

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(102, "G3"));    // ignored

            env.milestone(3);

            env.sendEventBean(new SupportBean("E4", 4));
            env.sendEventBean(new SupportBean("E5", 5));
            env.sendEventBean(new SupportBean("E6", 6));

            env.milestone(4);

            env.sendEventBean(new SupportBean_S1(201, "G2"));  // terminate
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{101, 201, 15});

            env.undeployAll();
        }
    }

    private static class ContextStartEndFilterStartedPatternEndedCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context EveryNowAndThen as " +
                "start SupportBean_S0 as s0 " +
                "end pattern [SupportBean_S1(p10 = s0.p00)]", path);

            String[] fields = "c1,c2".split(",");
            env.compileDeploy("@name('s0') context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_S0(100, "G1"));    // starts it
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 2});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(200, "GX"));  // false terminate
            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 5});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S1(200, "G1"));  // actual terminate
            env.sendEventBean(new SupportBean("E4", 4));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean_S0(101, "G2"));    // starts second

            env.milestone(3);

            env.sendEventBean(new SupportBean("E6", 6));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 6});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S1(101, null));    // false terminate
            env.sendEventBean(new SupportBean_S1(101, "GY"));    // false terminate

            env.sendEventBean(new SupportBean("E7", 7));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G2", 13});

            env.milestone(5);

            env.sendEventBean(new SupportBean_S1(300, "G2"));  // actual terminate
            env.sendEventBean(new SupportBean("E8", 8));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(102, "G3"));    // starts third
            env.sendEventBean(new SupportBean_S1(0, "G3"));    // terminate third

            env.milestone(6);

            env.sendEventBean(new SupportBean("E9", 9));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ContextStartEndStartAfterEndAfter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create context EveryNowAndThen as start after 5 sec end after 10 sec", path);

            String[] fields = "c1,c2,c3".split(",");
            String[] fieldsShort = "c3".split(",");

            env.compileDeploy("@name('s0') context EveryNowAndThen select context.startTime as c1, context.endTime as c2, sum(intPrimitive) as c3 " +
                "from SupportBean#keepall", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.milestone(0);

            sendTimeEvent(env, "2002-05-1T08:00:05.000");

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{DateTime.parseDefaultMSec("2002-05-1T08:00:05.000"), DateTime.parseDefaultMSec("2002-05-1T08:00:15.000"), 2});

            env.milestone(1);

            sendTimeEvent(env, "2002-05-1T08:00:14.999");

            env.milestone(2);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsShort, new Object[]{5});

            env.milestone(3);

            sendTimeEvent(env, "2002-05-1T08:00:15.000");

            env.milestone(4);

            env.sendEventBean(new SupportBean("E4", 4));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            sendTimeEvent(env, "2002-05-1T08:00:20.000");

            env.milestone(5);

            env.sendEventBean(new SupportBean("E5", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{DateTime.parseDefaultMSec("2002-05-1T08:00:20.000"), DateTime.parseDefaultMSec("2002-05-1T08:00:30.000"), 5});

            sendTimeEvent(env, "2002-05-1T08:00:30.000");

            env.milestone(6);

            env.sendEventBean(new SupportBean("E6", 6));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            // try variable
            path.clear();
            env.compileDeploy("create variable int var_start = 10", path);
            env.compileDeploy("create variable int var_end = 20", path);
            env.compileDeploy("create context FrequentlyContext as start after var_start sec end after var_end sec", path);

            env.undeployAll();
        }
    }

    private static class ContextStartEndFilterStartedFilterEndedOutputSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context EveryNowAndThen as start SupportBean_S0 as s0 end SupportBean_S1 as s1", path);

            String[] fields = "c1,c2".split(",");
            env.compileDeploy("@name('s0') context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall output snapshot when terminated", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_S0(100, "S0_1"));    // starts it
            env.sendEventBean(new SupportBean("E2", 2));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 3));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            // terminate
            env.sendEventBean(new SupportBean_S1(200, "S1_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 5});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(201, "S1_2"));

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 4));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean_S0(102, "S0_2"));    // starts it
            env.sendEventBean(new SupportBean_S1(201, "S1_3"));    // ends it
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_2", null});

            env.sendEventBean(new SupportBean_S0(103, "S0_3"));    // starts it
            env.sendEventBean(new SupportBean("E5", 6));           // some more data

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(104, "S0_4"));    // ignored
            env.sendEventBean(new SupportBean_S1(201, "S1_3"));    // ends it
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_3", 6});

            env.undeployModuleContaining("s0");
            env.undeployAll();
        }
    }

    private static class ContextStartEndPatternStartedPatternEnded implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create context EveryNowAndThen as " +
                "start pattern [s0=SupportBean_S0 -> timer:interval(1 sec)] " +
                "end pattern [s1=SupportBean_S1 -> timer:interval(1 sec)]", path);

            String[] fields = "c1,c2".split(",");
            env.compileDeploy("@name('s0') context EveryNowAndThen select context.s0.p00 as c1, sum(intPrimitive) as c2 " +
                "from SupportBean#keepall", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(100, "S0_1"));    // starts it
            env.sendEventBean(new SupportBean("E2", 2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 3));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimeEvent(env, "2002-05-1T08:00:01.000"); // 1 second passes

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 4});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E5", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 9});

            env.sendEventBean(new SupportBean_S0(101, "S0_2"));    // ignored
            sendTimeEvent(env, "2002-05-1T08:00:03.000");

            env.milestone(3);

            env.sendEventBean(new SupportBean("E6", 6));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 15});

            env.sendEventBean(new SupportBean_S1(101, "S1_1"));    // ignored

            env.sendEventBean(new SupportBean("E7", 7));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_1", 22});

            env.milestone(4);

            sendTimeEvent(env, "2002-05-1T08:00:04.000"); // terminates

            env.sendEventBean(new SupportBean("E8", 8));

            env.milestone(5);

            env.sendEventBean(new SupportBean_S1(102, "S1_2"));    // ignored
            sendTimeEvent(env, "2002-05-1T08:00:10.000");
            env.sendEventBean(new SupportBean("E9", 9));
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean_S0(103, "S0_3"));    // new instance
            sendTimeEvent(env, "2002-05-1T08:00:11.000");

            env.milestone(6);

            env.sendEventBean(new SupportBean("E10", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_3", 10});

            env.undeployAll();
        }
    }

    private static class ContextStartEndContextCreateDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context EverySecond as start (*, *, *, *, *, *) end (*, *, *, *, *, *)", path);
            env.compileDeploy("@name('s0') context EverySecond select * from SupportBean", path);
            env.addListener("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean());
            TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.milestone(1);

            sendTimeEvent(env, "2002-05-1T08:00:00.999");
            env.sendEventBean(new SupportBean());
            TestCase.assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.milestone(2);

            sendTimeEvent(env, "2002-05-1T08:00:01.000");
            env.sendEventBean(new SupportBean());
            TestCase.assertFalse(env.listener("s0").getAndClearIsInvoked());

            long start = DateTime.parseDefaultMSec("2002-05-1T08:00:01.999");
            for (int i = 0; i < 10; i++) {
                sendTimeEvent(env, start);

                sendEventAndAssert(env, false);

                start += 1;
                sendTimeEvent(env, start);

                sendEventAndAssert(env, true);

                start += 999;
                sendTimeEvent(env, start);

                sendEventAndAssert(env, true);

                start += 1;
                sendTimeEvent(env, start);

                sendEventAndAssert(env, false);

                start += 999;
            }

            env.undeployAll();
        }
    }

    private static class ContextStartEndDBHistorical implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            String[] fields = "s1.mychar".split(",");
            String stmtText = "@name('s0') context NineToFive select * from SupportBean_S0 as s0, sql:MyDB ['select * from mytesttable where ${id} = mytesttable.mybigint'] as s1";
            env.compileDeploy(stmtText, path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Y"});

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");

            env.sendEventBean(new SupportBean_S0(2));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            // now started
            sendTimeEvent(env, "2002-05-2T09:00:00.000");

            env.sendEventBean(new SupportBean_S0(3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"X"});

            env.undeployAll();
        }
    }

    private static class ContextStartEndPrevPriorAndAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            String[] fields = "col1,col2,col3,col4,col5".split(",");
            env.compileDeploy("@name('s0') context NineToFive " +
                "select prev(theString) as col1, prevwindow(sb) as col2, prevtail(theString) as col3, prior(1, theString) as col4, sum(intPrimitive) as col5 " +
                "from SupportBean#keepall as sb", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean());
            TestCase.assertFalse(env.listener("s0").isInvoked());

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            SupportBean event1 = new SupportBean("E1", 1);
            env.sendEventBean(event1);
            Object[][] expected = new Object[][]{{null, new SupportBean[]{event1}, "E1", null, 1}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, expected);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, expected);

            env.milestone(0);

            SupportBean event2 = new SupportBean("E2", 2);
            env.sendEventBean(event2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", new SupportBean[]{event2, event1}, "E1", "E1", 3});

            env.milestone(1);

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, null);

            env.sendEventBean(new SupportBean());
            TestCase.assertFalse(env.listener("s0").isInvoked());
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 0, null, 0, 0);

            env.milestone(2);

            // now started
            sendTimeEvent(env, "2002-05-2T09:00:00.000");

            env.milestone(3);

            SupportBean event3 = new SupportBean("E3", 9);
            env.sendEventBean(event3);
            expected = new Object[][]{{null, new SupportBean[]{event3}, "E3", null, 9}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, expected);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, expected);
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 1, null, 1, 1);

            env.undeployAll();
        }
    }

    private static class ContextStartEndJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            String[] fields = "col1,col2,col3,col4".split(",");
            env.compileDeploy("@name('s0') context NineToFive " +
                "select sb.theString as col1, sb.intPrimitive as col2, s0.id as col3, s0.p00 as col4 " +
                "from SupportBean#keepall as sb full outer join SupportBean_S0#keepall as s0 on p00 = theString", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, 1, "E1"});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E1", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 5, 1, "E1"});

            env.milestone(3);

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");

            env.milestone(4);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_S0(1, "E1"));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            // now started
            sendTimeEvent(env, "2002-05-2T09:00:00.000");

            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            env.sendEventBean(new SupportBean("E1", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 4, null, null});

            env.milestone(6);

            env.sendEventBean(new SupportBean_S0(2, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 4, 2, "E1"});

            env.undeployAll();
        }
    }

    private static class ContextStartEndPatternWithTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            env.compileDeploy("@name('s0') context NineToFive select * from pattern[every timer:interval(10 sec)]", path);
            env.addListener("s0");
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));   // from the context

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            Assert.assertEquals(2, SupportScheduleHelper.scheduleCountOverall(env));   // context + pattern
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendTimeEvent(env, "2002-05-1T09:00:10.000");
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.milestone(1);

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");
            env.listener("s0").reset();   // it is not well defined whether the listener does get fired or not
            Assert.assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));   // from the context

            env.milestone(2);

            // now started
            sendTimeEvent(env, "2002-05-2T09:00:00.000");
            Assert.assertEquals(2, SupportScheduleHelper.scheduleCountOverall(env));   // context + pattern
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendTimeEvent(env, "2002-05-2T09:00:10.000");
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ContextStartEndSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            String[] fields = "theString,col".split(",");

            env.compileDeploy("@name('s0') context NineToFive select theString, (select p00 from SupportBean_S0#lastevent) as col from SupportBean", path);
            env.addListener("s0");
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));   // from the context

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));   // from the context

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(11, "S01"));

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "S01"});

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));   // from the context

            env.milestone(3);

            env.sendEventBean(new SupportBean("Ex", 0));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            // now started
            sendTimeEvent(env, "2002-05-2T09:00:00.000");
            Assert.assertEquals(2, SupportFilterHelper.getFilterCountApprox(env));   // from the context
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", null});

            env.sendEventBean(new SupportBean_S0(12, "S02"));

            env.milestone(5);

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", "S02"});
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 1, 1, null, null);

            env.milestone(6);

            // now gone
            sendTimeEvent(env, "2002-05-2T17:00:00.000");
            Assert.assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));   // from the context

            env.milestone(7);

            env.sendEventBean(new SupportBean("Ey", 0));
            TestCase.assertFalse(env.listener("s0").isInvoked());
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 0, 0, null, null);

            env.undeployAll();
        }
    }

    public static class ContextStartEndSubselectCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T8:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            String[] fields = "theString,col".split(",");
            env.compileDeploy("@name('s0') context NineToFive select theString, " +
                "(select id from SupportBean_S0#keepall as s0 where s0.p00 = sb.theString) as col from SupportBean as sb", path);
            env.addListener("s0");

            env.milestone(0);

            // now started
            sendTimeEvent(env, "2002-05-1T9:00:00.000");

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(11, "S01"));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", null});

            env.milestone(3);

            env.sendEventBean(new SupportBean("S01", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S01", 11});

            env.milestone(4);

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");

            env.milestone(5);

            env.sendEventBean(new SupportBean("Ex", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            // now started
            sendTimeEvent(env, "2002-05-2T9:00:00.000");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(7);

            env.sendEventBean(new SupportBean("S01", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S01", null});

            env.milestone(8);

            env.sendEventBean(new SupportBean_S0(12, "S02"));
            env.sendEventBean(new SupportBean("S02", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S02", 12});

            env.milestone(9);

            // now gone
            sendTimeEvent(env, "2002-05-2T17:00:00.000");

            env.milestone(10);

            env.sendEventBean(new SupportBean("Ey", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ContextStartEndNWSameContextOnExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            // no started yet
            String[] fields = "theString,intPrimitive".split(",");
            env.compileDeploy("@name('s0') context NineToFive create window MyWindow#keepall as SupportBean", path);
            env.addListener("s0");

            env.compileDeploy("context NineToFive insert into MyWindow select * from SupportBean", path);

            env.compileDeploy("context NineToFive " +
                "on SupportBean_S0 s0 merge MyWindow mw where mw.theString = s0.p00 " +
                "when matched then update set intPrimitive = s0.id " +
                "when not matched then insert select makeBean(id, p00)", path);

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(3, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"E1", 3});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E1", 1});
            env.listener("s0").reset();

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");

            env.milestone(3);

            // no longer updated
            env.sendEventBean(new SupportBean_S0(1, "E1"));
            TestCase.assertFalse(env.listener("s0").isInvoked());

            env.milestone(4);

            // now started again but empty
            sendTimeEvent(env, "2002-05-2T09:00:00.000");

            env.milestone(5);

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.undeployAll();
        }
    }

    private static class ContextStartEndNWFireAndForget implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);

            // no started yet
            env.compileDeploy("context NineToFive create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("context NineToFive insert into MyWindow select * from SupportBean", path);

            tryNWQuery(env, path, 0);

            // now started
            sendTimeEvent(env, "2002-05-1T09:00:00.000");
            tryNWQuery(env, path, 0);

            env.milestone(0);

            // now not empty
            env.sendEventBean(new SupportBean());
            tryNWQuery(env, path, 1);

            env.milestone(1);

            // now gone
            sendTimeEvent(env, "2002-05-1T17:00:00.000");

            env.milestone(2);

            tryNWQuery(env, path, 0);
            env.sendEventBean(new SupportBean());

            env.milestone(3);

            // now started again but empty
            sendTimeEvent(env, "2002-05-2T09:00:00.000");
            tryNWQuery(env, path, 0);

            env.milestone(4);

            // fill some data
            env.sendEventBean(new SupportBean());
            env.sendEventBean(new SupportBean());
            sendTimeEvent(env, "2002-05-2T09:10:00.000");
            tryNWQuery(env, path, 2);

            env.undeployAll();
        }

        private static void tryNWQuery(RegressionEnvironment env, RegressionPath path, int numRows) {
            EPCompiled compiled = env.compileFAF("select * from MyWindow", path);
            EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled);
            Assert.assertEquals(numRows, result.getArray().length);
        }
    }

    private static class ContextStartEndStartTurnedOff implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T08:00:00.000");
            RegressionPath path = new RegressionPath();
            String contextEPL = "@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
            env.compileDeploy(contextEPL, path);
            assertContextEventType(env.statement("context").getEventType());
            env.addListener("context");
            env.statement("context").setSubscriber(new MiniSubscriber());

            env.compileDeploy("@Name('A') context NineToFive select * from SupportBean", path);
            env.addListener("A");

            sendTimeAndAssert(env, "2002-05-1T08:59:30.000", false, "A");

            env.milestone(0);

            sendTimeAndAssert(env, "2002-05-1T08:59:59.999", false, "A");
            sendTimeAndAssert(env, "2002-05-1T09:00:00.000", true, "A");

            env.compileDeploy("@Name('B') context NineToFive select * from SupportBean", path);
            env.addListener("B");

            sendTimeAndAssert(env, "2002-05-1T16:59:59.000", true, "A,B");

            env.milestone(1);

            sendTimeAndAssert(env, "2002-05-1T17:00:00.000", false, "A,B");

            env.milestone(2);

            env.compileDeploy("@Name('C') context NineToFive select * from SupportBean", path);
            env.addListener("C");

            sendTimeAndAssert(env, "2002-05-2T08:59:59.999", false, "A,B,C");
            sendTimeAndAssert(env, "2002-05-2T09:00:00.000", true, "A,B,C");

            env.milestone(3);

            sendTimeAndAssert(env, "2002-05-2T16:59:59.000", true, "A,B,C");
            sendTimeAndAssert(env, "2002-05-2T17:00:00.000", false, "A,B,C");

            TestCase.assertFalse(env.listener("context").isInvoked());

            env.undeployAll();
            path.clear();

            // test SODA
            sendTimeEvent(env, "2002-05-3T16:59:59.000");
            env.eplToModelCompileDeploy(contextEPL, path);

            // test built-in properties
            env.compileDeploy("@Name('A') context NineToFive " +
                "select context.name as c1, context.startTime as c2, context.endTime as c3, theString as c4 from SupportBean", path);
            env.addListener("A");

            env.sendEventBean(new SupportBean("E1", 10));
            EventBean theEvent = env.listener("A").assertOneGetNewAndReset();
            Assert.assertEquals("NineToFive", theEvent.get("c1"));
            Assert.assertEquals("2002-05-03T16:59:59.000", DateTime.print(theEvent.get("c2")));
            Assert.assertEquals("2002-05-03T17:00:00.000", DateTime.print(theEvent.get("c3")));
            Assert.assertEquals("E1", theEvent.get("c4"));

            env.undeployAll();
        }
    }

    private static class ContextStartEndStartTurnedOn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Assert.assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
            RegressionPath path = new RegressionPath();

            sendTimeEvent(env, "2002-05-1T09:15:00.000");
            env.compileDeploy("@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);
            Assert.assertEquals(1, SupportContextMgmtHelper.getContextCount(env));

            env.milestone(0);

            env.compileDeploy("@Name('A') context NineToFive select * from SupportBean", path).addListener("A");

            sendTimeAndAssert(env, "2002-05-1T09:16:00.000", true, "A");

            env.milestone(1);

            sendTimeAndAssert(env, "2002-05-1T16:59:59.000", true, "A");
            sendTimeAndAssert(env, "2002-05-1T17:00:00.000", false, "A");

            env.compileDeploy("@Name('B') context NineToFive select * from SupportBean", path).addListener("B");

            sendTimeAndAssert(env, "2002-05-2T08:59:59.999", false, "A,B");
            sendTimeAndAssert(env, "2002-05-2T09:15:00.000", true, "A,B");

            env.milestone(2);

            sendTimeAndAssert(env, "2002-05-2T16:59:59.000", true, "A,B");

            env.milestone(3);

            sendTimeAndAssert(env, "2002-05-2T17:00:00.000", false, "A,B");

            Assert.assertEquals(1, SupportContextMgmtHelper.getContextCount(env));
            env.undeployModuleContaining("A");

            env.milestone(4);

            Assert.assertEquals(1, SupportContextMgmtHelper.getContextCount(env));
            env.undeployModuleContaining("B");

            env.milestone(5);

            Assert.assertEquals(1, SupportContextMgmtHelper.getContextCount(env));
            env.undeployModuleContaining("context");

            env.milestone(6);

            Assert.assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
        }
    }

    public static class ContextStart9End5AggUngrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T8:00:00.000");
            RegressionPath path = new RegressionPath();

            String eplContext = "@Name('CTX') create context CtxNineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
            env.compileDeploy(eplContext, path);

            String[] fields = "c1,c2".split(",");
            String epl = "@Name('S1') context CtxNineToFive select theString as c1, sum(intPrimitive) as c2 from SupportBean";
            env.compileDeploy(epl, path).addListener("S1");

            env.sendEventBean(new SupportBean("G1", 1));
            assertFalse(env.listener("S1").getAndClearIsInvoked());

            env.milestone(0);

            sendTimeEvent(env, "2002-05-1T9:00:00.000");

            env.sendEventBean(new SupportBean("G2", 2));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G2", 2});

            env.sendEventBean(new SupportBean("G3", 3));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G3", 5});

            env.milestone(1);

            sendTimeEvent(env, "2002-05-1T17:00:00.000");

            env.sendEventBean(new SupportBean("G4", 4));
            assertFalse(env.listener("S1").getAndClearIsInvoked());

            env.milestone(2);

            sendTimeEvent(env, "2002-05-2T8:00:00.000");

            sendTimeEvent(env, "2002-05-2T9:00:00.000");
            env.sendEventBean(new SupportBean("G5", 20));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"G5", 20});

            env.undeployAll();
        }
    }

    public static class ContextStart9End5AggGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEvent(env, "2002-05-1T7:00:00.000");
            RegressionPath path = new RegressionPath();

            String eplTwo = "create context NestedContext as start (0, 8, *, *, *) end (0, 9, *, *, *)";
            env.compileDeploy(eplTwo, path);

            env.milestone(0);

            String[] fields = "c1,c2".split(",");
            env.compileDeploy("@Name('s0') context NestedContext select " +
                "theString as c1, count(*) as c2 from SupportBean group by theString", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendTimeEvent(env, "2002-05-1T8:00:00.000"); // start context

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

            env.milestone(3);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L});

            env.milestone(4);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L});

            env.milestone(5);

            sendTimeEvent(env, "2002-05-1T9:00:00.000"); // terminate

            env.milestone(5);

            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            sendTimeEvent(env, "2002-05-2T8:00:00.000"); // start context

            env.milestone(7);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1L});

            env.undeployAll();
        }
    }

    public static class ContextStartEndMultiCrontab implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionMultiCrontab(env,
                "(0, 8, *, *, *, *), (0, 10, *, *, *, *)",
                "(0, 9, *, *, *, *), (0, 12, *, *, *, *)",
                new TimeRangePair[] {
                    new TimeRangePair("2002-05-30T09:30:00.000", null, false),
                    new TimeRangePair("2002-05-30T010:00:00.000", "2002-05-30T011:59:59.999", true),
                    new TimeRangePair("2002-05-30T012:00:00.000", "2002-05-31T07:59:59.999", false),
                    new TimeRangePair("2002-05-31T08:00:00.000", "2002-05-31T08:59:59.999", true),
                    new TimeRangePair("2002-05-31T09:00:00.000", "2002-05-31T09:59:59.999", false),
                    new TimeRangePair("2002-05-31T010:00:00.000", "2002-05-31T010:10:00.000", true)
                });

            runAssertionMultiCrontab(env,
                "(0, 8, *, *, *, *)",
                "(0, 12, *, *, [1, 2, 3, 4, 5], *), (0, 20, *, *, [0, 6], *)",
                new TimeRangePair[] {
                    new TimeRangePair("2018-12-06T09:30:00.000", null, true), // Thurs. Dec 6, 2018
                    new TimeRangePair("2018-12-06T10:00:00.000", "2018-12-06T11:59:59.999", true),
                    new TimeRangePair("2018-12-06T12:00:00.000", "2018-12-07T07:59:59.999", false),
                    new TimeRangePair("2018-12-07T08:00:00.000", "2018-12-07T11:59:59.999", true),
                    new TimeRangePair("2018-12-07T12:00:00.000", "2018-12-08T07:59:59.999", false),
                    new TimeRangePair("2018-12-08T08:00:00.000", "2018-12-08T19:59:59.999", true),
                    new TimeRangePair("2018-12-08T20:00:00.000", "2018-12-09T07:59:59.999", false),
                    new TimeRangePair("2018-12-09T08:00:00.000", "2018-12-09T19:59:59.999", true),
                    new TimeRangePair("2018-12-09T20:00:00.000", "2018-12-10T07:59:59.999", false),
                    new TimeRangePair("2018-12-10T08:00:00.000", "2018-12-10T11:59:59.999", true),
                    new TimeRangePair("2018-12-10T12:00:00.000", "2018-12-10T13:00:00.000", false)
                });

            runAssertionMultiCrontab(env,
                "(0, 8, *, *, 1, *), (0, 9, *, *, 2, *)",
                "(0, 10, *, *, *)",
                new TimeRangePair[] {
                    new TimeRangePair("2018-12-03T09:30:00.000", null, true), // Mon. Dec 3, 2018
                    new TimeRangePair("2018-12-03T09:30:00.000", "2018-12-03T09:59:59.999", true),
                    new TimeRangePair("2018-12-03T10:00:00.000", "2018-12-04T08:59:59.999", false),
                    new TimeRangePair("2018-12-04T09:00:00.000", "2018-12-04T09:59:59.999", true),
                    new TimeRangePair("2018-12-04T10:00:00.000", "2018-12-10T07:59:59.999", false),
                    new TimeRangePair("2018-12-10T09:00:00.000", "2018-12-10T09:59:59.999", true),
                });

            String epl = "create context Ctx as start (0, 8, *, *, 1, *), (0, 9, *, *, 2, *) end (0, 12, *, *, [1,2,3,4,5], *), (0, 20, *, *, [0,6], *)";
            env.eplToModelCompileDeploy(epl);
            env.undeployAll();
        }
    }

    private static void runAssertionMultiCrontab(RegressionEnvironment env, String startList, String endList, TimeRangePair[] pairs) {
        String epl = "create context Ctx " +
            "start " + startList +
            "end " + endList + ";\n" +
            "@name('s0') context Ctx select * from SupportBean";

        sendTimeEvent(env, pairs[0].getStart());
        assertNull(pairs[0].getEnd());
        env.compileDeploy(epl).addListener("s0");
        sendEventAndAssert(env, pairs[0].isExpected());

        for (int i = 1; i < pairs.length; i++) {
            long start = DateTime.parseDefaultMSec(pairs[i].getStart());
            long end = DateTime.parseDefaultMSec(pairs[i].getEnd());
            long current = start;

            while (current < end) {
                // Comment-me-in: log.info("Sending " + DateTime.print(current));
                sendTimeEvent(env, current);
                sendEventAndAssert(env, pairs[i].isExpected());
                current += 5 * 60 * 1000; // advance in 5-minute intervals
            }

            // Comment-me-in: log.info("Sending " + DateTime.print(end));
            sendTimeEvent(env, end);
            sendEventAndAssert(env, pairs[i].isExpected());
        }

        env.undeployAll();
    }

    private static void assertContextEventType(EventType eventType) {
        Assert.assertEquals(0, eventType.getPropertyNames().length);
        Assert.assertEquals("stmt0_ctxout_NineToFive_1", eventType.getName());
    }

    private static void sendTimeAndAssert(RegressionEnvironment env, String time, boolean isInvoked, String statementNames) {
        sendTimeEvent(env, time);
        env.sendEventBean(new SupportBean());

        for (String statement : statementNames.split(",")) {
            SupportListener listener = env.listener(statement);
            Assert.assertEquals("Failed for statement " + statement, isInvoked, listener.getAndClearIsInvoked());
        }
    }

    private static void sendTimeEvent(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendTimeEvent(RegressionEnvironment env, long time) {
        env.advanceTime(time);
    }

    private static void sendEventAndAssert(RegressionEnvironment env, boolean expected) {
        env.sendEventBean(new SupportBean());
        Assert.assertEquals(expected, env.listener("s0").isInvoked());
        env.listener("s0").reset();
    }

    public static SupportBean singleRowPluginMakeBean(int id, String p00) {
        return new SupportBean(p00, id);
    }

    public static class MiniSubscriber {
        public static void update() {
            // no action
        }
    }

    private static class TimeRangePair {
        private final String start;
        private final String end;
        private final boolean expected;

        public TimeRangePair(String start, String end, boolean expected) {
            this.start = start;
            this.end = end;
            this.expected = expected;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public boolean isExpected() {
            return expected;
        }
    }
}
