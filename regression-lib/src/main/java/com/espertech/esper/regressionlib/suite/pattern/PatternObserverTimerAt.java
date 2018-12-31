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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.patternassert.*;
import com.espertech.esper.runtime.client.DeploymentOptions;
import org.junit.Assert;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternObserverTimerAt {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternTimerAtSimple());
        execs.add(new PatternOp());
        execs.add(new PatternCronParameter());
        execs.add(new PatternAtWeekdays());
        execs.add(new PatternAtWeekdaysPrepared());
        execs.add(new PatternAtWeekdaysVariable());
        execs.add(new PatternExpression());
        execs.add(new PatternPropertyAndSODAAndTimezone());
        execs.add(new PatternEvery15thMonth());
        execs.add(new PatternWMilliseconds());
        return execs;
    }

    private static class PatternWMilliseconds implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runSequenceIsolatedMilliseconds(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, *, *, *, *, *, 200) ]",
                new String[]{
                    "2013-08-23T08:05:00.200",
                    "2013-08-23T08:05:01.200",
                    "2013-08-23T08:05:02.200",
                    "2013-08-23T08:05:03.200"
                });

            runSequenceIsolatedMilliseconds(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, *, *, *, *, *, [200,201,202,300,500]) ]",
                new String[]{
                    "2013-08-23T08:05:00.200",
                    "2013-08-23T08:05:00.201",
                    "2013-08-23T08:05:00.202",
                    "2013-08-23T08:05:00.300",
                    "2013-08-23T08:05:00.500",
                    "2013-08-23T08:05:01.200",
                    "2013-08-23T08:05:01.201",
                });

            runSequenceIsolatedMilliseconds(env, "2013-08-23T08:05:00.373",
                "select * from pattern [ every timer:at(*, *, *, *, *, * / 5, *, 0) ]",
                new String[]{
                    "2013-08-23T08:05:05.000",
                    "2013-08-23T08:05:10.000",
                    "2013-08-23T08:05:15.000",
                    "2013-08-23T08:05:20.000"
                });

            runSequenceIsolatedMilliseconds(env, "2013-08-23T08:05:00.373",
                "select * from pattern [ every timer:at(*, *, *, *, *, * / 5, *, 373) ]",
                new String[]{
                    "2013-08-23T08:05:05.373",
                    "2013-08-23T08:05:10.373",
                    "2013-08-23T08:05:15.373",
                    "2013-08-23T08:05:20.373"
                });

            runSequenceIsolatedMilliseconds(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(10, 9, *, *, *, 2, *, 373, 0) ]",
                new String[]{
                    "2013-08-23T09:10:02.373",
                    "2013-08-24T09:10:02.373",
                    "2013-08-25T09:10:02.373"
                });
        }
    }

    public static class PatternTimerAtSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            sendTimeEvent("2002-05-30T9:00:00.000", env);
            String epl = "@Name('s0') select * from pattern [every timer:at(*,*,*,*,*)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEvent("2002-05-30T9:00:59.999", env);
            Assert.assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.milestone(1);

            sendTimeEvent("2002-05-30T9:01:00.000", env);
            Assert.assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.milestone(2);

            sendTimeEvent("2002-05-30T9:01:59.999", env);
            Assert.assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendTimeEvent("2002-05-30T9:02:00.000", env);
            Assert.assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class PatternEvery15thMonth implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("select * from pattern[every timer:at(*,*,*,*/15,*)]").undeployAll();
        }
    }

    private static class PatternOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2005, 3, 9, 8, 00, 00);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();

            /**
             // Start a 2004-12-9 8:00:00am and send events every 10 minutes
             "A1"    8:10
             "B1"    8:20
             "C1"    8:30
             "B2"    8:40
             "A2"    8:50
             "D1"    9:00
             "E1"    9:10
             "F1"    9:20
             "D2"    9:30
             "B3"    9:40
             "G1"    9:50
             "D3"   10:00
             */

            EventCollection testData = EventCollectionFactory.getEventSetOne(startTime, 1000 * 60 * 10);
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase = null;

            testCase = new EventExpressionCase("timer:at(10, 8, *, *, *)");
            testCase.add("A1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(10, 8, *, *, *, 1)");
            testCase.add("B1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(5, 8, *, *, *)");
            testCase.add("A1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(10, 8, *, *, *, *)");
            testCase.add("A1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(25, 9, *, *, *)");
            testCase.add("D2");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(11, 8, *, *, *)");
            testCase.add("B1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(19, 8, *, *, *, 59)");
            testCase.add("B1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(* / 5, *, *, *, *, *)");
            addAll(testCase);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(*, *, *, *, *, * / 10)");
            addAll(testCase);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(20, 8, *, *, *, 20)");
            testCase.add("C1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(*, *, *, *, *)");
            addAll(testCase);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(*, *, *, *, *, *)");
            addAll(testCase);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(* / 9, *, *, *, *, *)");
            addAll(testCase);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(* / 10, *, *, *, *, *)");
            addAll(testCase);
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("every timer:at(* / 30, *, *, *, *)");
            testCase.add("C1");
            testCase.add("D1");
            testCase.add("D2");
            testCase.add("D3");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(10, 9, *, *, *, 10) or timer:at(30, 9, *, *, *, *)");
            testCase.add("F1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B3') -> timer:at(20, 9, *, *, *, *)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("b=SupportBean_B(id='B3') -> timer:at(45, 9, *, *, *, *)");
            testCase.add("G1", "b", testData.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(59, 8, *, *, *, 59) -> d=SupportBean_D");
            testCase.add("D1", "d", testData.getEvent("D1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(*, 9, *, *, *, 59) -> d=SupportBean_D");
            testCase.add("D2", "d", testData.getEvent("D2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(22, 8, *, *, *) -> b=SupportBean_B(id='B3') -> timer:at(55, *, *, *, *)");
            testCase.add("D3", "b", testData.getEvent("B3"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(40, *, *, *, *, 1) and b=SupportBean_B");
            testCase.add("A2", "b", testData.getEvent("B1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(40, 9, *, *, *, 1) or d=SupportBean_D(id='D3')");
            testCase.add("G1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(22, 8, *, *, *) -> b=SupportBean_B() -> timer:at(55, 8, *, *, *)");
            testCase.add("D1", "b", testData.getEvent("B2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(22, 8, *, *, *, 1) where timer:within(1 second)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(22, 8, *, *, *, 1) where timer:within(31 minutes)");
            testCase.add("C1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(*, 9, *, *, *) and timer:at(55, *, *, *, *)");
            testCase.add("D1");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("timer:at(40, 8, *, *, *, 1) and b=SupportBean_B");
            testCase.add("A2", "b", testData.getEvent("B1"));
            testCaseList.addTest(testCase);

            String text = "select * from pattern [timer:at(10,8,*,*,*,*)]";
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            PatternExpr pattern = Patterns.timerAt(10, 8, null, null, null, null);
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model = SerializableObjectCopier.copyMayFail(model);
            Assert.assertEquals(text, model.toEPL());
            testCase = new EventExpressionCase(model);
            testCase.add("A1");
            testCaseList.addTest(testCase);

            /**
             * As of release 1.6 this no longer updates listeners when the statement is started.
             * The reason is that the dispatch view only gets attached after a pattern started, therefore
             * ZeroDepthEventStream looses the event.
             * There should be no use case requiring this
             *
             testCase = new EventExpressionCase("not timer:at(22, 8, *, *, *, 1)");
             testCase.add(EventCollection.ON_START_EVENT_ID);
             testCaseList.addTest(testCase);
             */

            // Run all tests
            PatternTestHarness util = new PatternTestHarness(testData, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternAtWeekdays implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [every timer:at(0,8,*,*,[1,2,3,4,5])]";

            Calendar cal = GregorianCalendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
            sendTimer(cal.getTimeInMillis(), env);

            env.compileDeploy(expression);
            env.addListener("s0");

            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class PatternAtWeekdaysPrepared implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [every timer:at(?::int,?::int,*,*,[1,2,3,4,5])]";

            Calendar cal = GregorianCalendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
            sendTimer(cal.getTimeInMillis(), env);

            EPCompiled compiled = env.compile(expression);
            env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                prepared.setObject(1, 0);
                prepared.setObject(2, 8);
            }));
            env.addListener("s0");

            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class PatternAtWeekdaysVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [every timer:at(VMIN,VHOUR,*,*,[1,2,3,4,5])]";

            Calendar cal = GregorianCalendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
            sendTimer(cal.getTimeInMillis(), env);

            EPCompiled compiled = env.compile(expression);
            env.deploy(compiled).addListener("s0");
            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class PatternExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select * from pattern [every timer:at(7+1-8,4+4,*,*,[1,2,3,4,5])]";

            Calendar cal = GregorianCalendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008
            sendTimer(cal.getTimeInMillis(), env);

            env.compileDeploy(expression).addListener("s0");
            tryAssertion(env);

            env.undeployAll();
        }
    }

    private static class PatternPropertyAndSODAAndTimezone implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            sendTimeEvent("2008-08-3T06:00:00.000", env);
            String expression = "@name('s0') select * from pattern [a=SupportBean -> every timer:at(2*a.intPrimitive,*,*,*,*)]";
            env.compileDeploy(expression);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 20));

            sendTimeEvent("2008-08-3T06:39:59.000", env);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendTimeEvent("2008-08-3T06:40:00.000", env);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            env.undeployAll();

            // test SODA
            String epl = "select * from pattern [every timer:at(*/VFREQ,VMIN:VMAX,1 last,*,[8,2:VMAX,*/VREQ])]";
            EPStatementObjectModel model = env.eplToModel(epl);
            Assert.assertEquals(epl, model.toEPL());

            // test timezone
            if (TimeZone.getDefault().getRawOffset() == -5 * 60 * 60 * 1000) {    // asserting only in EST timezone, see schedule util tests
                sendTimeEvent("2008-01-4T06:50:00.000", env);
                env.compileDeploy("@name('s0') select * from pattern [timer:at(0, 5, 4, 1, *, 0, 'PST')]").addListener("s0");

                sendTimeEvent("2008-01-4T07:59:59.999", env);
                assertFalse(env.listener("s0").getAndClearIsInvoked());

                sendTimeEvent("2008-01-4T08:00:00.000", env);
                assertTrue(env.listener("s0").getAndClearIsInvoked());
            }
            env.compileDeploy("select * from pattern [timer:at(0, 5, 4, 8, *, 0, 'xxx')]");
            env.compileDeploy("select * from pattern [timer:at(0, 5, 4, 8, *, 0, *)]");

            env.undeployAll();
        }
    }

    public static class PatternCronParameter implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            //
            // LAST
            //
            // Last day of the month, at 5pm
            runSequenceIsolated(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(0, 17, last, *, *) ]",
                new String[]{
                    "2013-08-31T17:00:00.000",
                    "2013-09-30T17:00:00.000",
                    "2013-10-31T17:00:00.000",
                    "2013-11-30T17:00:00.000",
                    "2013-12-31T17:00:00.000",
                    "2014-01-31T17:00:00.000",
                    "2014-02-28T17:00:00.000",
                    "2014-03-31T17:00:00.000",
                    "2014-04-30T17:00:00.000",
                    "2014-05-31T17:00:00.000",
                    "2014-06-30T17:00:00.000",
                });

            // Last day of the month, at the earliest
            runSequenceIsolated(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, last, *, *) ]",
                new String[]{
                    "2013-08-31T00:00:00.000",
                    "2013-09-30T00:00:00.000",
                    "2013-10-31T00:00:00.000",
                    "2013-11-30T00:00:00.000",
                    "2013-12-31T00:00:00.000",
                    "2014-01-31T00:00:00.000",
                    "2014-02-28T00:00:00.000",
                    "2014-03-31T00:00:00.000",
                    "2014-04-30T00:00:00.000",
                    "2014-05-31T00:00:00.000",
                    "2014-06-30T00:00:00.000",
                });

            // Last Sunday of the month, at 5pm
            runSequenceIsolated(env, "2013-08-20T08:00:00.000",
                "select * from pattern [ every timer:at(0, 17, *, *, 0 last, *) ]",
                new String[]{
                    "2013-08-25T17:00:00.000",
                    "2013-09-29T17:00:00.000",
                    "2013-10-27T17:00:00.000",
                    "2013-11-24T17:00:00.000",
                    "2013-12-29T17:00:00.000",
                    "2014-01-26T17:00:00.000",
                    "2014-02-23T17:00:00.000",
                    "2014-03-30T17:00:00.000",
                    "2014-04-27T17:00:00.000",
                    "2014-05-25T17:00:00.000",
                    "2014-06-29T17:00:00.000",
                });

            // Last Friday of the month, any time
            // 0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4= Thursday, 5=Friday, 6=Saturday
            runSequenceIsolated(env, "2013-08-20T08:00:00.000",
                "select * from pattern [ every timer:at(*, *, *, *, 5 last, *) ]",
                new String[]{
                    "2013-08-30T00:00:00.000",
                    "2013-09-27T00:00:00.000",
                    "2013-10-25T00:00:00.000",
                    "2013-11-29T00:00:00.000",
                    "2013-12-27T00:00:00.000",
                    "2014-01-31T00:00:00.000",
                    "2014-02-28T00:00:00.000",
                    "2014-03-28T00:00:00.000",
                });

            // Last day of week (Saturday)
            runSequenceIsolated(env, "2013-08-01T08:00:00.000",
                "select * from pattern [ every timer:at(*, *, *, *, last, *) ]",
                new String[]{
                    "2013-08-03T00:00:00.000",
                    "2013-08-10T00:00:00.000",
                    "2013-08-17T00:00:00.000",
                    "2013-08-24T00:00:00.000",
                    "2013-08-31T00:00:00.000",
                    "2013-09-07T00:00:00.000",
                });

            // Last day of month in August
            // For Java: January=0, February=1, March=2, April=3, May=4, June=5,
            //            July=6, August=7, September=8, November=9, October=10, December=11
            // For Esper: January=1, February=2, March=3, April=4, May=5, June=6,
            //            July=7, August=8, September=9, November=10, October=11, December=12
            runSequenceIsolated(env, "2013-01-01T08:00:00.000",
                "select * from pattern [ every timer:at(*, *, last, 8, *, *) ]",
                new String[]{
                    "2013-08-31T00:00:00.000",
                    "2014-08-31T00:00:00.000",
                    "2015-08-31T00:00:00.000",
                    "2016-08-31T00:00:00.000",
                });

            // Last day of month in Feb. (test leap year)
            runSequenceIsolated(env, "2007-01-01T08:00:00.000",
                "select * from pattern [ every timer:at(*, *, last, 2, *, *) ]",
                new String[]{
                    "2007-02-28T00:00:00.000",
                    "2008-02-29T00:00:00.000",
                    "2009-02-28T00:00:00.000",
                    "2010-02-28T00:00:00.000",
                    "2011-02-28T00:00:00.000",
                    "2012-02-29T00:00:00.000",
                    "2013-02-28T00:00:00.000",
                });

            // Observer for last Friday of each June (month 6)
            runSequenceIsolated(env, "2007-01-01T08:00:00.000",
                "select * from pattern [ every timer:at(*, *, *, 6, 5 last, *) ]",
                new String[]{
                    "2007-06-29T00:00:00.000",
                    "2008-06-27T00:00:00.000",
                    "2009-06-26T00:00:00.000",
                    "2010-06-25T00:00:00.000",
                    "2011-06-24T00:00:00.000",
                    "2012-06-29T00:00:00.000",
                    "2013-06-28T00:00:00.000",
                });

            //
            // LASTWEEKDAY
            //

            // Last weekday (last day that is not a weekend day)
            runSequenceIsolated(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(0, 17, lastweekday, *, *) ]",
                new String[]{
                    "2013-08-30T17:00:00.000",
                    "2013-09-30T17:00:00.000",
                    "2013-10-31T17:00:00.000",
                    "2013-11-29T17:00:00.000",
                    "2013-12-31T17:00:00.000",
                    "2014-01-31T17:00:00.000",
                    "2014-02-28T17:00:00.000",
                    "2014-03-31T17:00:00.000",
                    "2014-04-30T17:00:00.000",
                    "2014-05-30T17:00:00.000",
                    "2014-06-30T17:00:00.000",
                });

            // Last weekday, any time
            runSequenceIsolated(env, "2013-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, lastweekday, *, *, *) ]",
                new String[]{
                    "2013-08-30T00:00:00.000",
                    "2013-09-30T00:00:00.000",
                    "2013-10-31T00:00:00.000",
                    "2013-11-29T00:00:00.000",
                    "2013-12-31T00:00:00.000",
                    "2014-01-31T00:00:00.000",
                });

            // Observer for last weekday of September, for 2007 it's Friday September 28th
            runSequenceIsolated(env, "2007-08-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, lastweekday, 9, *, *) ]",
                new String[]{
                    "2007-09-28T00:00:00.000",
                    "2008-09-30T00:00:00.000",
                    "2009-09-30T00:00:00.000",
                    "2010-09-30T00:00:00.000",
                    "2011-09-30T00:00:00.000",
                    "2012-09-28T00:00:00.000",
                });

            // Observer for last weekday of February
            runSequenceIsolated(env, "2007-01-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, lastweekday, 2, *, *) ]",
                new String[]{
                    "2007-02-28T00:00:00.000",
                    "2008-02-29T00:00:00.000",
                    "2009-02-27T00:00:00.000",
                    "2010-02-26T00:00:00.000",
                    "2011-02-28T00:00:00.000",
                    "2012-02-29T00:00:00.000",
                });

            //
            // WEEKDAY
            //
            runSequenceIsolated(env, "2007-01-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, 1 weekday, 9, *, *) ]",
                new String[]{
                    "2007-09-03T00:00:00.000",
                    "2008-09-01T00:00:00.000",
                    "2009-09-01T00:00:00.000",
                    "2010-09-01T00:00:00.000",
                    "2011-09-01T00:00:00.000",
                    "2012-09-03T00:00:00.000",
                    "2013-09-02T00:00:00.000",
                });

            runSequenceIsolated(env, "2007-01-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, 30 weekday, 9, *, *) ]",
                new String[]{
                    "2007-09-28T00:00:00.000",
                    "2008-09-30T00:00:00.000",
                    "2009-09-30T00:00:00.000",
                    "2010-09-30T00:00:00.000",
                    "2011-09-30T00:00:00.000",
                    "2012-09-28T00:00:00.000",
                    "2013-09-30T00:00:00.000",
                });

            // nearest weekday for current month on the 10th
            runSequenceIsolated(env, "2013-01-23T08:05:00.000",
                "select * from pattern [ every timer:at(*, *, 10 weekday, *, *, *) ]",
                new String[]{
                    "2013-02-11T00:00:00.000",
                    "2013-03-11T00:00:00.000",
                    "2013-04-10T00:00:00.000",
                    "2013-05-10T00:00:00.000",
                    "2013-06-10T00:00:00.000",
                    "2013-07-10T00:00:00.000",
                    "2013-08-09T00:00:00.000",
                });
        }
    }

    private static void runSequenceIsolated(RegressionEnvironment env, String startTime, String epl, String[] times) {
        sendTime(env, startTime);

        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        runSequence(env, times);

        env.undeployAll();
    }

    private static void runSequence(RegressionEnvironment env, String[] times) {
        for (String next : times) {
            // send right-before time
            long nextLong = DateTime.parseDefaultMSec(next);
            env.advanceTime(nextLong - 1001);
            assertFalse("unexpected callback at " + next, env.listener("s0").isInvoked());

            // send right-after time
            env.advanceTime(nextLong + 1000);
            assertTrue("missing callback at " + next, env.listener("s0").getAndClearIsInvoked());
        }
    }

    private static void runSequenceIsolatedMilliseconds(RegressionEnvironment env, String startTime, String epl, String[] times) {
        sendTime(env, startTime);

        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        runSequenceMilliseconds(env, times);

        env.undeployAll();
    }

    private static void runSequenceMilliseconds(RegressionEnvironment env, String[] times) {
        for (String next : times) {
            // send right-before time
            long nextLong = DateTime.parseDefaultMSec(next);
            env.advanceTime(nextLong - 1);
            // Comment-me-in: System.out.println("Advance to " + DateTime.print(nextLong - 1));
            assertFalse("unexpected callback at " + next, env.listener("s0").isInvoked());

            // send right-after time
            env.advanceTime(nextLong);
            // Comment-me-in: System.out.println("Advance to " + DateTime.print(nextLong));
            assertTrue("missing callback at " + next, env.listener("s0").getAndClearIsInvoked());
        }
    }

    private static void sendTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void tryAssertion(RegressionEnvironment env) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2008, 7, 3, 10, 0, 0);      // start on a Sunday at 6am, August 3 2008

        List<String> invocations = new ArrayList<String>();
        for (int i = 0; i < 24 * 60 * 7; i++) { // run for 1 week
            cal.add(Calendar.MINUTE, 1);
            sendTimer(cal.getTimeInMillis(), env);

            if (env.listener("s0").getAndClearIsInvoked()) {
                // System.out.println("invoked at calendar " + cal.getTime().toString());
                invocations.add(cal.getTime().toString());
            }
        }
        String[] expectedResult = new String[5];
        cal.set(2008, 7, 4, 8, 0, 0); //"Mon Aug 04 08:00:00 EDT 2008"
        expectedResult[0] = cal.getTime().toString();
        cal.set(2008, 7, 5, 8, 0, 0); //"Tue Aug 05 08:00:00 EDT 2008"
        expectedResult[1] = cal.getTime().toString();
        cal.set(2008, 7, 6, 8, 0, 0); //"Wed Aug 06 08:00:00 EDT 2008"
        expectedResult[2] = cal.getTime().toString();
        cal.set(2008, 7, 7, 8, 0, 0); //"Thu Aug 07 08:00:00 EDT 2008"
        expectedResult[3] = cal.getTime().toString();
        cal.set(2008, 7, 8, 8, 0, 0); //"Fri Aug 08 08:00:00 EDT 2008"
        expectedResult[4] = cal.getTime().toString();
        EPAssertionUtil.assertEqualsExactOrder(expectedResult, invocations.toArray());
    }

    private static void sendTimeEvent(String time, RegressionEnvironment env) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }

    private static void addAll(EventExpressionCase desc) {
        desc.add("A1");
        desc.add("B1");
        desc.add("C1");
        desc.add("B2");
        desc.add("A2");
        desc.add("D1");
        desc.add("E1");
        desc.add("F1");
        desc.add("D2");
        desc.add("B3");
        desc.add("G1");
        desc.add("D3");
    }
}