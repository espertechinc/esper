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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndA;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndB;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ExprDTDocSamples implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        env.compileDeploy("select timeTaken.format() as timeTakenStr from RFIDEvent");
        env.compileDeploy("select timeTaken.get('month') as timeTakenMonth from RFIDEvent");
        env.compileDeploy("select timeTaken.getMonthOfYear() as timeTakenMonth from RFIDEvent");
        env.compileDeploy("select timeTaken.minus(2 minutes) as timeTakenMinus2Min from RFIDEvent");
        env.compileDeploy("select timeTaken.minus(2*60*1000) as timeTakenMinus2Min from RFIDEvent");
        env.compileDeploy("select timeTaken.plus(2 minutes) as timeTakenMinus2Min from RFIDEvent");
        env.compileDeploy("select timeTaken.plus(2*60*1000) as timeTakenMinus2Min from RFIDEvent");
        env.compileDeploy("select timeTaken.roundCeiling('min') as timeTakenRounded from RFIDEvent");
        env.compileDeploy("select timeTaken.roundFloor('min') as timeTakenRounded from RFIDEvent");
        env.compileDeploy("select timeTaken.set('month', 3) as timeTakenMonth from RFIDEvent");
        env.compileDeploy("select timeTaken.withDate(2002, 4, 30) as timeTakenDated from RFIDEvent");
        env.compileDeploy("select timeTaken.withMax('sec') as timeTakenMaxSec from RFIDEvent");
        env.compileDeploy("select timeTaken.toCalendar() as timeTakenCal from RFIDEvent");
        env.compileDeploy("select timeTaken.toDate() as timeTakenDate from RFIDEvent");
        env.compileDeploy("select timeTaken.toMillisec() as timeTakenLong from RFIDEvent");

        // test pattern use
        AtomicInteger milestone = new AtomicInteger();
        tryRun(env, "a.longdateStart.after(b)", "2002-05-30T09:00:00.000", "2002-05-30T08:59:59.999", true, milestone);
        tryRun(env, "a.after(b.longdateStart)", "2002-05-30T09:00:00.000", "2002-05-30T08:59:59.999", true, milestone);
        tryRun(env, "a.after(b)", "2002-05-30T09:00:00.000", "2002-05-30T08:59:59.999", true, milestone);
        tryRun(env, "a.after(b)", "2002-05-30T08:59:59.999", "2002-05-30T09:00:00.000", false, milestone);
    }

    private void tryRun(RegressionEnvironment env, String condition, String tsa, String tsb, boolean isInvoked, AtomicInteger milestone) {
        String epl = "@name('s0') select * from pattern [a=A -> b=B] as abc where " + condition;
        env.compileDeploy(epl).addListener("s0").milestoneInc(milestone);

        env.sendEventBean(SupportTimeStartEndA.make("E1", tsa, 0), "A");
        env.sendEventBean(SupportTimeStartEndB.make("E2", tsb, 0), "B");
        assertEquals(isInvoked, env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    public static class MyEvent {

        public String get() {
            return "abc";
        }
    }
}
