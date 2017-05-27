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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecPatternCronParameter implements RegressionExecution, SupportBeanConstants {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        //
        // LAST
        //
        // Last day of the month, at 5pm
        runSequenceIsolated(epService, "2013-08-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2013-08-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2013-08-20T08:00:00.000",
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
        runSequenceIsolated(epService, "2013-08-20T08:00:00.000",
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
        runSequenceIsolated(epService, "2013-08-01T08:00:00.000",
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
        runSequenceIsolated(epService, "2013-01-01T08:00:00.000",
                "select * from pattern [ every timer:at(*, *, last, 8, *, *) ]",
                new String[]{
                    "2013-08-31T00:00:00.000",
                    "2014-08-31T00:00:00.000",
                    "2015-08-31T00:00:00.000",
                    "2016-08-31T00:00:00.000",
                });

        // Last day of month in Feb. (test leap year)
        runSequenceIsolated(epService, "2007-01-01T08:00:00.000",
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
        runSequenceIsolated(epService, "2007-01-01T08:00:00.000",
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
        runSequenceIsolated(epService, "2013-08-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2013-08-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2007-08-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2007-01-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2007-01-23T08:05:00.000",
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

        runSequenceIsolated(epService, "2007-01-23T08:05:00.000",
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
        runSequenceIsolated(epService, "2013-01-23T08:05:00.000",
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

    private void runSequenceIsolated(EPServiceProvider epService, String startTime, String epl, String[] times) {
        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("i1");
        sendTime(isolated, startTime);
        SupportUpdateListener listener = new SupportUpdateListener();
        isolated.getEPAdministrator().createEPL(epl, "S0", null).addListener(listener);
        runSequence(isolated, times, listener);
        epService.getEPAdministrator().destroyAllStatements();
        isolated.destroy();
    }

    private void runSequence(EPServiceProviderIsolated epService, String[] times, SupportUpdateListener listener) {
        for (String next : times) {
            // send right-before time
            long nextLong = DateTime.parseDefaultMSec(next);
            epService.getEPRuntime().sendEvent(new CurrentTimeEvent(nextLong - 1001));
            assertFalse("unexpected callback at " + next, listener.isInvoked());

            // send right-after time
            epService.getEPRuntime().sendEvent(new CurrentTimeEvent(nextLong + 1000));
            assertTrue("missing callback at " + next, listener.getAndClearIsInvoked());
        }
    }

    private void sendTime(EPServiceProviderIsolated epService, String time) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(time)));
    }
}



