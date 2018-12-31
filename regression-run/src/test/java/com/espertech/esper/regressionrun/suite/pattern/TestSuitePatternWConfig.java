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
package com.espertech.esper.regressionrun.suite.pattern;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.suite.pattern.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TestSuitePatternWConfig extends TestCase {

    public void testMax2Noprevent() {
        RegressionSession session = RegressionRunner.session();
        configure(2, false, session.getConfiguration());
        RegressionRunner.run(session, new PatternOperatorFollowedByMax2Noprevent());
        session.destroy();
    }

    public void testMax2Prevent() {
        RegressionSession session = RegressionRunner.session();
        configure(2, true, session.getConfiguration());
        RegressionRunner.run(session, new PatternOperatorFollowedByMax2Prevent());
        session.destroy();
    }

    public void testMax4Prevent() {
        RegressionSession session = RegressionRunner.session();
        configure(4, true, session.getConfiguration());
        RegressionRunner.run(session, new PatternOperatorFollowedByMax4Prevent());
        session.destroy();
    }

    public void testPatternMicrosecondResolution() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
        RegressionRunner.run(session, new PatternMicrosecondResolution(true));
        session.destroy();
    }

    public void testPatternMicrosecondResolutionCrontab() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
        RegressionRunner.run(session, new PatternMicrosecondResolutionCrontab());
        session.destroy();
    }

    public void testPatternObserverTimerScheduleTimeZoneEST() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getRuntime().getExpression().setTimeZone(TimeZone.getTimeZone("GMT-4:00"));
        RegressionRunner.run(session, new PatternObserverTimerScheduleTimeZoneEST());
        session.destroy();
    }

    private void configure(long max, boolean preventStart, Configuration configuration) {
        configuration.getRuntime().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
        configuration.getRuntime().getPatterns().setMaxSubexpressions(max);
        configuration.getRuntime().getPatterns().setMaxSubexpressionPreventStart(preventStart);

        for (Class clazz : new Class[]{SupportBean_A.class, SupportBean_B.class, SupportBean.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }
    }
}
