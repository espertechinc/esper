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
package com.espertech.esper.pattern.guard;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportSchedulingServiceImpl;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.supportunit.guard.SupportQuitable;
import com.espertech.esper.supportunit.pattern.SupportPatternContextFactory;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import junit.framework.TestCase;

public class TestTimerWithinGuard extends TestCase {
    private TimerWithinGuard guard;
    private SchedulingService scheduleService;
    private SupportQuitable quitable;

    public void setUp() {
        StatementContext stmtContext = SupportStatementContextFactory.makeContext(new SchedulingServiceImpl(new TimeSourceServiceImpl()));
        scheduleService = stmtContext.getSchedulingService();
        PatternAgentInstanceContext agentInstanceContext = SupportPatternContextFactory.makePatternAgentInstanceContext(scheduleService);

        quitable = new SupportQuitable(agentInstanceContext);

        guard = new TimerWithinGuard(1000, quitable);
    }

    public void testInspect() {
        assertTrue(guard.inspect(null));
    }

    /**
     * Make sure the timer calls guardQuit after the set time period
     */
    public void testStartAndTrigger() {
        scheduleService.setTime(0);

        guard.startGuard();

        assertEquals(0, quitable.getAndResetQuitCounter());

        scheduleService.setTime(1000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);

        assertEquals(1, quitable.getAndResetQuitCounter());
    }

    public void testStartAndStop() {
        scheduleService.setTime(0);

        guard.startGuard();

        guard.stopGuard();

        scheduleService.setTime(1001);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);

        assertEquals(0, quitable.getAndResetQuitCounter());
    }

    public void testInvalid() {
        try {
            guard.startGuard();
            guard.startGuard();
            fail();
        } catch (IllegalStateException ex) {
            // Expected exception
        }
    }
}
