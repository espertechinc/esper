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

import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.junit.Assert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternObserverTimerSchedule {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternTimerScheduleSimple());
        execs.add(new PatternObserverTimerScheduleMultiform());
        execs.add(new PatternTimerScheduleLimitedWDateAndPeriod());
        execs.add(new PatternTimerScheduleJustDate());
        execs.add(new PatternTimerScheduleJustPeriod());
        execs.add(new PatternTimerScheduleDateWithPeriod());
        execs.add(new PatternTimerScheduleUnlimitedRecurringPeriod());
        return execs;
    }

    public static class PatternTimerScheduleSimple implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            sendTimeEvent(env, "2002-05-01T9:00:00.000");
            String epl = "@Name('s0') select * from pattern [every timer:schedule(period:1 day, repetitions: 3)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEvent(env, "2002-05-02T8:59:59.999");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendTimeEvent(env, "2002-05-02T9:00:00.000");
            env.assertListenerInvoked("s0");

            env.milestone(2);

            sendTimeEvent(env, "2002-05-03T8:59:59.999");
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            sendTimeEvent(env, "2002-05-03T9:00:00.000");
            env.assertListenerInvoked("s0");

            env.milestone(4);

            sendTimeEvent(env, "2002-05-04T8:59:59.999");
            env.assertListenerNotInvoked("s0");

            env.milestone(5);

            sendTimeEvent(env, "2002-05-04T9:00:00.000");
            env.assertListenerInvoked("s0");

            env.milestone(6);

            env.milestone(7);

            sendTimeEvent(env, "2002-05-30T9:00:00.000");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class PatternObserverTimerScheduleMultiform implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // just-date: "<date>" : non-recurring, typically a future start time, no period
            runAssertionJustFutureDate(env, milestone);
            runAssertionJustPastDate(env, milestone);

            // just-period: "P<...>" : non-recurring
            runAssertionJustPeriod(env, milestone);

            // partial-form-2: "<date>/P<period>": non-recurring, no start date (starts from current date), with period
            tryAssertionDateWithPeriod(env, milestone);

            // partial-form-1: "R<?>/P<period>": recurring, no start date (starts from current date), with period
            runAssertionRecurringLimitedWithPeriod(env, milestone);
            runAssertionRecurringUnlimitedWithPeriod(env, milestone);
            runAssertionRecurringAnchoring(env, milestone);

            // full form: "R<?>/<date>/P<period>" : recurring, start time, with period
            runAssertionFullFormLimitedFutureDated(env, milestone);
            runAssertionFullFormLimitedPastDated(env, milestone);
            runAssertionFullFormUnlimitedFutureDated(env, milestone);
            runAssertionFullFormUnlimitedPastDated(env, milestone);
            runAssertionFullFormUnlimitedPastDatedAnchoring(env, milestone);

            // equivalent formulations
            runAssertionEquivalent(env);

            // invalid tests
            runAssertionInvalid(env);

            // followed-by
            runAssertionFollowedBy(env, milestone);
            runAssertionFollowedByDynamicallyComputed(env, milestone);

            // named parameters
            runAssertionNameParameters(env);

            /**
             * For Testing, could also use this:
             */
            /*
            env.advanceTimeStage(DateTime.parseDefaultMSecWZone("2001-10-01T05:51:00.000GMT-0:00")));
            runtime.getDeploymentService().createEPL("select * from pattern[timer:schedule('2008-03-01T13:00:00Z/P1Y2M10DT2H30M')]").addListener(listener);

            long next = runtime.getRuntime().getNextScheduledTime();
            System.out.println(DateTime.print(next));
            */
        }
    }

    public static class PatternTimerScheduleLimitedWDateAndPeriod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimeEventWithZone(env, "2012-10-01T05:51:00.000GMT-0:00");

            // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
            String epl = "@Name('s0') select * from pattern[every timer:schedule(iso: 'R3/2012-10-01T05:52:00Z/PT2S')]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEventWithZone(env, "2012-10-01T5:51:59.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendTimeEventWithZone(env, "2012-10-01T5:52:00.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.milestone(2);

            sendTimeEventWithZone(env, "2012-10-01T5:52:01.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            sendTimeEventWithZone(env, "2012-10-01T5:52:02.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.milestone(4);

            sendTimeEventWithZone(env, "2012-10-01T5:52:03.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(5);

            sendTimeEventWithZone(env, "2012-10-01T5:52:04.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.milestone(6);

            sendTimeEventWithZone(env, "2012-10-01T5:53:00.000GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class PatternTimerScheduleJustDate implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            sendTimeEventWithZone(env, "2012-10-01T5:51:00.000GMT-0:00");

            // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
            String epl = "@Name('s0') select * from pattern[every timer:schedule(date: '2012-10-02T00:00:00Z')]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEventWithZone(env, "2012-10-01T23:59:59.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendTimeEventWithZone(env, "2012-10-02T0:0:00.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.milestone(2);

            sendTimeEventWithZone(env, "2012-10-10T0:0:00.000GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class PatternTimerScheduleJustPeriod implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            sendTimeEventWithZone(env, "2012-10-01T5:51:00.000GMT-0:00");

            // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
            String epl = "@Name('s0') select * from pattern[every timer:schedule(period: 1 minute)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEventWithZone(env, "2012-10-01T5:51:59.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendTimeEventWithZone(env, "2012-10-02T5:52:00.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    public static class PatternTimerScheduleDateWithPeriod implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            sendTimeEventWithZone(env, "2012-10-01T5:51:00.000GMT-0:00");

            // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
            String epl = "@Name('s0') select * from pattern[every timer:schedule(period: 1 day, date: '2012-10-02T00:00:00Z')]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEventWithZone(env, "2012-10-02T23:59:59.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendTimeEventWithZone(env, "2012-10-03T0:0:00.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    public static class PatternTimerScheduleUnlimitedRecurringPeriod implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            sendTimeEventWithZone(env, "2012-10-01T5:51:00.000GMT-0:00");

            String epl = "@Name('s0') select * from pattern[every timer:schedule(repetitions:-1, period: 1 sec)]";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendTimeEventWithZone(env, "2012-10-01T5:51:0.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            sendTimeEventWithZone(env, "2012-10-01T5:51:1.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.milestone(2);

            sendTimeEventWithZone(env, "2012-10-01T5:51:1.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            sendTimeEventWithZone(env, "2012-10-01T5:51:2.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.milestone(4);

            sendTimeEventWithZone(env, "2012-10-01T5:51:2.999GMT-0:00");
            env.assertListenerNotInvoked("s0");

            env.milestone(5);

            sendTimeEventWithZone(env, "2012-10-01T5:51:3.000GMT-0:00");
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static void runAssertionFollowedByDynamicallyComputed(RegressionEnvironment env, AtomicInteger milestone) {

        sendCurrentTime(env, "2012-10-01T05:51:07.000GMT-0:00");

        String epl = "@name('s0') select * from pattern[every sb=SupportBean -> timer:schedule(iso: computeISO8601String(sb))]";
        env.compileDeploy(epl).addListener("s0");

        SupportBean b1 = makeSendEvent(env, "E1", 5);

        sendCurrentTime(env, "2012-10-01T05:51:9.999GMT-0:00");
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.milestoneInc(milestone);

        sendCurrentTime(env, "2012-10-01T05:51:10.000GMT-0:00");
        Assert.assertEquals(b1, env.listener("s0").assertOneGetNewAndReset().get("sb"));

        env.undeployAll();
    }

    private static void runAssertionFollowedBy(RegressionEnvironment env, AtomicInteger milestone) {
        sendCurrentTime(env, "2012-10-01T05:51:07.000GMT-0:00");

        String epl = "@name('s0') select * from pattern[every sb=SupportBean -> timer:schedule(iso: 'R/1980-01-01T00:00:00Z/PT15S')]";
        env.compileDeploy(epl).addListener("s0");

        SupportBean b1 = makeSendEvent(env, "E1");

        sendCurrentTime(env, "2012-10-01T05:51:14.999GMT-0:00");
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.milestoneInc(milestone);

        sendCurrentTime(env, "2012-10-01T05:51:15.000GMT-0:00");
        Assert.assertEquals(b1, env.listener("s0").assertOneGetNewAndReset().get("sb"));

        sendCurrentTime(env, "2012-10-01T05:51:16.000GMT-0:00");
        SupportBean b2 = makeSendEvent(env, "E2");

        env.milestoneInc(milestone);

        sendCurrentTime(env, "2012-10-01T05:51:18.000GMT-0:00");
        SupportBean b3 = makeSendEvent(env, "E3");

        sendCurrentTime(env, "2012-10-01T05:51:30.000GMT-0:00");
        env.assertPropsPerRowLastNew("s0", "sb".split(","), new Object[][]{{b2}, {b3}});

        env.undeployAll();
    }

    private static void runAssertionInvalid(RegressionEnvironment env) {

        // the ISO 8601 parse tests reside with the parser
        env.tryInvalidCompile("select * from pattern[every timer:schedule(iso: 'x')]",
            "Invalid parameter for pattern observer 'timer:schedule(iso:\"x\")': Failed to parse 'x': Exception parsing date 'x', the date is not a supported ISO 8601 date");

        // named parameter tests: absence, typing, etc.
        env.tryInvalidCompile("select * from pattern[timer:schedule()]",
            "Invalid parameter for pattern observer 'timer:schedule()': No parameters provided");
        env.tryInvalidCompile("select * from pattern[timer:schedule(x:1)]",
            "Invalid parameter for pattern observer 'timer:schedule(x:1)': Unexpected named parameter 'x', expecting any of the following: [iso, repetitions, date, period]");
        env.tryInvalidCompile("select * from pattern[timer:schedule(period:1)]",
            "Invalid parameter for pattern observer 'timer:schedule(period:1)': Failed to validate named parameter 'period', expected a single expression returning a TimePeriod-typed value");
        env.tryInvalidCompile("select * from pattern[timer:schedule(repetitions:'a', period:1 seconds)]",
            "Invalid parameter for pattern observer 'timer:schedule(repetitions:\"a\",period:1 seconds)': Failed to validate named parameter 'repetitions', expected a single expression returning any of the following types: int,long");
        env.tryInvalidCompile("select * from pattern[timer:schedule(date:1 seconds)]",
            "Invalid parameter for pattern observer 'timer:schedule(date:1 seconds)': Failed to validate named parameter 'date', expected a single expression returning any of the following types: string,Calendar,Date,long");
        env.tryInvalidCompile("select * from pattern[timer:schedule(repetitions:1)]",
            "Invalid parameter for pattern observer 'timer:schedule(repetitions:1)': Either the date or period parameter is required");
        env.tryInvalidCompile("select * from pattern[timer:schedule(iso: 'R/1980-01-01T00:00:00Z/PT15S', repetitions:1)]",
            "Invalid parameter for pattern observer 'timer:schedule(iso:\"R/1980-01-01T00:00:00Z/PT15S\",repetitions:1)': The 'iso' parameter is exclusive of other parameters");
    }

    private static void runAssertionEquivalent(RegressionEnvironment env) {

        String first = "@name('s0') select * from pattern[every timer:schedule(iso: 'R2/2008-03-01T13:00:00Z/P1Y2M10DT2H30M')]";
        tryAssertionEquivalent(env, first);

        String second = "@name('s0') select * from pattern[every " +
            "(timer:schedule(iso: '2008-03-01T13:00:00Z') or" +
            " timer:schedule(iso: '2009-05-11T15:30:00Z'))]";
        tryAssertionEquivalent(env, second);

        String third = "@name('s0') select * from pattern[every " +
            "(timer:schedule(iso: '2008-03-01T13:00:00Z') or" +
            " timer:schedule(iso: '2008-03-01T13:00:00Z/P1Y2M10DT2H30M'))]";
        tryAssertionEquivalent(env, third);
    }

    private static void tryAssertionEquivalent(RegressionEnvironment env, String epl) {
        sendCurrentTime(env, "2001-10-01T05:51:00.000GMT-0:00");

        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2008-03-01T13:00:00.000GMT-0:00");
        assertReceivedAtTime(env, "2009-05-11T15:30:00.000GMT-0:00");
        assertSendNoMoreCallback(env, "2012-10-01T05:52:04.000GMT-0:00");

        env.undeployAll();
    }

    private static void tryAssertionDateWithPeriod(RegressionEnvironment env, AtomicInteger milestone) {
        tryAssertionDateWithPeriod(env, "iso: '2012-10-01T05:52:00Z/PT2S'", milestone);
        tryAssertionDateWithPeriod(env, "date: '2012-10-01T05:52:00Z', period: 2 seconds", milestone);
    }

    private static void tryAssertionDateWithPeriod(RegressionEnvironment env, String parameters, AtomicInteger milestone) {
        sendCurrentTime(env, "2012-10-01T05:51:00.000GMT-0:00");

        // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
        String epl = "@name('s0') select * from pattern[timer:schedule(" + parameters + ")]";
        env.compileDeploy(epl).addListener("s0");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:52:02.000GMT-0:00");
        assertSendNoMoreCallback(env, "2012-10-01T05:52:04.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionFullFormLimitedFutureDated(RegressionEnvironment env, AtomicInteger milestone) {
        tryAssertionFullFormLimitedFutureDated(env, true, "iso: 'R3/2012-10-01T05:52:00Z/PT2S'", milestone);
        tryAssertionFullFormLimitedFutureDated(env, false, "iso: 'R3/2012-10-01T05:52:00Z/PT2S'", milestone);
        tryAssertionFullFormLimitedFutureDated(env, false, "repetitions: 3L, date:'2012-10-01T05:52:00Z', period: 2 seconds", milestone);
    }

    private static void tryAssertionFullFormLimitedFutureDated(RegressionEnvironment env, boolean audit, String parameters, AtomicInteger milestone) {

        sendCurrentTime(env, "2012-10-01T05:51:00.000GMT-0:00");

        // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
        String epl = (audit ? "@Audit " : "") + "@name('s0') select * from pattern[every timer:schedule(" + parameters + ")]";
        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:52:02.000GMT-0:00");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:52:04.000GMT-0:00");
        assertSendNoMoreCallback(env, "2012-10-01T05:52:06.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionJustFutureDate(RegressionEnvironment env, AtomicInteger milestone) {
        tryAssertionJustFutureDate(env, true, "iso: '2012-10-01T05:52:00Z'", milestone);
        tryAssertionJustFutureDate(env, false, "iso: '2012-10-01T05:52:00Z'", milestone);
        tryAssertionJustFutureDate(env, false, "date: '2012-10-01T05:52:00Z'", milestone);
    }

    private static void tryAssertionJustFutureDate(RegressionEnvironment env, boolean hasEvery, String parameters, AtomicInteger milestone) {
        sendCurrentTime(env, "2012-10-01T05:51:00.000GMT-0:00");

        // Fire once at "2012-10-01T05:52:00Z" (UTC)
        String epl = "@name('s0') select * from pattern[" + (hasEvery ? "every " : "") + "timer:schedule(" + parameters + ")]";
        env.compileDeploy(epl).addListener("s0");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        assertSendNoMoreCallback(env, "2012-10-01T05:53:00.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionJustPastDate(RegressionEnvironment env, AtomicInteger milestone) {
        tryAssertionJustPastDate(env, true, milestone);
        tryAssertionJustPastDate(env, false, milestone);
    }

    private static void tryAssertionJustPastDate(RegressionEnvironment env, boolean hasEvery, AtomicInteger milestone) {
        sendCurrentTime(env, "2012-10-01T05:51:00.000GMT-0:00");

        // Fire once at "2012-10-01T05:52:00Z" (UTC)
        String epl = "@name('s0') select * from pattern[" + (hasEvery ? "every " : "") + "timer:schedule(iso: '2010-10-01T05:52:00Z')]";
        env.compileDeploy(epl).addListener("s0");

        env.milestoneInc(milestone);

        assertSendNoMoreCallback(env, "2012-10-01T05:53:00.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionJustPeriod(RegressionEnvironment env, AtomicInteger milestone) {
        tryAssertionJustPeriod(env, "iso:'P1DT2H'", milestone);
        tryAssertionJustPeriod(env, "period: 1 day 2 hours", milestone);
    }

    private static void tryAssertionJustPeriod(RegressionEnvironment env, String parameters, AtomicInteger milestone) {
        sendCurrentTime(env, "2012-10-01T05:51:00.000GMT-0:00");

        // Fire once after 1 day and 2 hours
        String epl = "@name('s0') select * from pattern[timer:schedule(" + parameters + ")]";
        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2012-10-02T07:51:00.000GMT-0:00");

        env.milestoneInc(milestone);

        assertSendNoMoreCallback(env, "2012-10-03T09:51:00.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionRecurringLimitedWithPeriod(RegressionEnvironment env, AtomicInteger milestone) {
        tryAssertionRecurringLimitedWithPeriod(env, "iso:'R3/PT2S'", milestone);
        tryAssertionRecurringLimitedWithPeriod(env, "repetitions:3L, period: 2 seconds", milestone);
    }

    private static void tryAssertionRecurringLimitedWithPeriod(RegressionEnvironment env, String parameters, AtomicInteger milestone) {

        // Fire 3 times after 2 seconds from current time
        sendCurrentTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(" + parameters + ")]";
        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2012-10-01T05:52:02.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:52:04.000GMT-0:00");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:52:06.000GMT-0:00");
        assertSendNoMoreCallback(env, "2012-10-01T05:52:08.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionRecurringUnlimitedWithPeriod(RegressionEnvironment env, AtomicInteger milestone) {

        // Fire 3 times after 2 seconds from current time
        sendCurrentTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(iso:'R/PT1M10S')]";

        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2012-10-01T05:53:10.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:54:20.000GMT-0:00");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:55:30.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:56:40.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionFullFormUnlimitedPastDated(RegressionEnvironment env, AtomicInteger milestone) {
        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(iso:'R/1980-01-01T00:00:00Z/PT1S')]";

        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2012-10-01T05:52:01.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:52:02.000GMT-0:00");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:52:03.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionNameParameters(RegressionEnvironment env) {

        tryAssertionNameParameters(env, "repetitions:-1L, date:'1980-01-01T00:00:00Z', period: 1 seconds");
        tryAssertionNameParameters(env, "repetitions:-1, date:getThe1980Calendar(), period: 1 seconds");
        tryAssertionNameParameters(env, "repetitions:-1, date:getThe1980Date(), period: getTheSeconds() seconds");
        tryAssertionNameParameters(env, "repetitions:-1, date:getThe1980Long(), period: 1 seconds");
        tryAssertionNameParameters(env, "repetitions:-1, date:getThe1980LocalDateTime(), period: 1 seconds");
        tryAssertionNameParameters(env, "repetitions:-1, date:getThe1980ZonedDateTime(), period: 1 seconds");
    }

    private static void tryAssertionNameParameters(RegressionEnvironment env, String parameters) {
        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(" + parameters + ")]";
        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2012-10-01T05:52:01.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:52:02.000GMT-0:00");
        assertReceivedAtTime(env, "2012-10-01T05:52:03.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionFullFormUnlimitedPastDatedAnchoring(RegressionEnvironment env, AtomicInteger milestone) {

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(env, "2012-01-01T00:0:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(iso:'R/1980-01-01T00:00:00Z/PT10S')]";
        env.compileDeploy(epl).addListener("s0");

        env.milestoneInc(milestone);

        sendCurrentTime(env, "2012-01-01T00:0:15.000GMT-0:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        sendCurrentTime(env, "2012-01-01T00:0:20.000GMT-0:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-01-01T00:0:30.000GMT-0:00");

        sendCurrentTime(env, "2012-01-01T00:0:55.000GMT-0:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        assertReceivedAtTime(env, "2012-01-01T00:1:00.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionRecurringAnchoring(RegressionEnvironment env, AtomicInteger milestone) {
        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(env, "2012-01-01T00:0:00.000GMT-0:00");

        String epl = "@name('s0') select * from pattern[every timer:schedule(iso: 'R/PT10S')]";
        env.compileDeploy(epl).addListener("s0");

        sendCurrentTime(env, "2012-01-01T00:0:15.000GMT-0:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        env.milestoneInc(milestone);

        sendCurrentTime(env, "2012-01-01T00:0:20.000GMT-0:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        assertReceivedAtTime(env, "2012-01-01T00:0:30.000GMT-0:00");

        env.milestoneInc(milestone);

        sendCurrentTime(env, "2012-01-01T00:0:55.000GMT-0:00");
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        assertReceivedAtTime(env, "2012-01-01T00:1:00.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionFullFormLimitedPastDated(RegressionEnvironment env, AtomicInteger milestone) {

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(iso: 'R8/2012-10-01T05:51:00Z/PT10S')]";
        env.compileDeploy(epl).addListener("s0");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2012-10-01T05:52:10.000GMT-0:00");
        assertSendNoMoreCallback(env, "2012-10-01T05:52:20.000GMT-0:00");

        env.undeployAll();
    }

    private static void runAssertionFullFormUnlimitedFutureDated(RegressionEnvironment env, AtomicInteger milestone) {
        // Repeat unlimited number of times, reference-dated to future date, period of 1 day
        sendCurrentTime(env, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "@name('s0') select * from pattern[every timer:schedule(iso: 'R/2013-01-01T02:00:05Z/P1D')]";

        env.compileDeploy(epl).addListener("s0");

        assertReceivedAtTime(env, "2013-01-01T02:00:05.000GMT-0:00");

        env.milestoneInc(milestone);

        assertReceivedAtTime(env, "2013-01-02T02:00:05.000GMT-0:00");
        assertReceivedAtTime(env, "2013-01-03T02:00:05.000GMT-0:00");
        assertReceivedAtTime(env, "2013-01-04T02:00:05.000GMT-0:00");

        env.undeployAll();
    }

    private static void assertSendNoMoreCallback(RegressionEnvironment env, String time) {
        sendCurrentTime(env, time);
        assertFalse(env.listener("s0").getIsInvokedAndReset());
        sendCurrentTime(env, "2999-01-01T00:0:00.000GMT-0:00");
        assertFalse(env.listener("s0").getIsInvokedAndReset());
    }

    private static void assertReceivedAtTime(RegressionEnvironment env, String time) {
        long msec = DateTime.parseDefaultMSecWZone(time);

        env.advanceTime(msec - 1);
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.advanceTime(msec);
        assertTrue("expected but not received at " + time, env.listener("s0").getIsInvokedAndReset());
    }

    private static void sendTimeEvent(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSecWZone(time));
    }

    private static SupportBean makeSendEvent(RegressionEnvironment env, String theString) {
        return makeSendEvent(env, theString, 0);
    }

    private static SupportBean makeSendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        env.eventService().sendEventBean(b, b.getClass().getSimpleName());
        return b;
    }

    public static String computeISO8601String(SupportBean bean) {
        return "R/1980-01-01T00:00:00Z/PT" + bean.getIntPrimitive() + "S";
    }

    public static Calendar getThe1980Calendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-0:00"));
        cal.setTimeInMillis(DateTime.parseDefaultMSecWZone("1980-01-01T00:0:0.000GMT-0:00"));
        return cal;
    }

    public static Date getThe1980Date() {
        return getThe1980Calendar().getTime();
    }

    public static long getThe1980Long() {
        return getThe1980Calendar().getTimeInMillis();
    }

    public static int getTheSeconds() {
        return 1;
    }

    public static LocalDateTime getThe1980LocalDateTime() {
        long millis = getThe1980Long();
        Instant instant = Instant.ofEpochMilli(millis);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static ZonedDateTime getThe1980ZonedDateTime() {
        return ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
    }

    private static void sendTimeEventWithZone(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSecWZone(time));
    }
}


