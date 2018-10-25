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
package com.espertech.esper.regressionrun.suite.view;

import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.suite.view.ViewGroup;
import com.espertech.esper.regressionlib.suite.view.ViewTimeBatchWSystemTime;
import com.espertech.esper.regressionlib.suite.view.ViewTimeWin;
import com.espertech.esper.regressionlib.suite.view.ViewTimeWinWSystemTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.regressionlib.support.schedule.SupportDateTimeUtil.timePlusMonth;

public class TestSuiteViewWConfig extends TestCase {
    public void testViewGroupMicroseconds() {
        RegressionSession session = RegressionRunner.session();
        configureMicroseconds(session);
        RegressionRunner.run(session, new ViewGroup.ViewGroupReclaimWithFlipTime(5000000));
        session.destroy();
    }

    public void testViewTimeMicrosecondsWinFlipTime() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(0, "1", 1000000));
        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(0, "10 milliseconds", 10000));
        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(0, "10 microseconds", 10));
        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(0, "1 seconds 10 microseconds", 1000010));
        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(123456789, "10", 123456789 + 10 * 1000000));
        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(0, "1 months 10 microseconds", timePlusMonth(0, 1) * 1000 + 10));

        long currentTime = DateTime.parseDefaultMSec("2002-05-1T08:00:01.999");
        execs.add(new ViewTimeWin.ViewTimeWindowFlipTimer(currentTime * 1000 + 33, "3 months 100 microseconds", timePlusMonth(currentTime, 3) * 1000 + 33 + 100));

        RegressionSession session = RegressionRunner.session();
        configureMicroseconds(session);
        RegressionRunner.run(session, execs);
        session.destroy();
    }

    public void testViewTimeBatchWSystemTime() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportMarketDataBean.class);
        session.getConfiguration().getRuntime().getThreading().setInternalTimerEnabled(true);
        RegressionRunner.run(session, new ViewTimeBatchWSystemTime());
        session.destroy();
    }

    public void testViewTimeWinWSystemTime() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportMarketDataBean.class);
        session.getConfiguration().getRuntime().getThreading().setInternalTimerEnabled(true);
        RegressionRunner.run(session, new ViewTimeWinWSystemTime());
        session.destroy();
    }

    private void configureMicroseconds(RegressionSession session) {
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
    }
}
