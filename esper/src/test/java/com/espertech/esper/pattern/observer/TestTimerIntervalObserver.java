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
package com.espertech.esper.pattern.observer;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportSchedulingServiceImpl;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.pattern.*;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.supportunit.guard.SupportObserverEvaluator;
import com.espertech.esper.supportunit.pattern.SupportPatternContextFactory;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import junit.framework.TestCase;

public class TestTimerIntervalObserver extends TestCase {
    private PatternContext context;
    private PatternAgentInstanceContext agentContext;

    private TimerIntervalObserver observer;
    private SchedulingServiceImpl scheduleService;
    private SupportObserverEvaluator evaluator;
    private MatchedEventMap beginState;

    public void setUp() {

        beginState = new MatchedEventMapImpl(new MatchedEventMapMeta(new String[0], false));

        scheduleService = new SchedulingServiceImpl(new TimeSourceServiceImpl());
        StatementContext stmtContext = SupportStatementContextFactory.makeContext(scheduleService);
        context = new PatternContext(stmtContext, 1, new MatchedEventMapMeta(new String[0], false), false);
        agentContext = SupportPatternContextFactory.makePatternAgentInstanceContext(scheduleService);

        evaluator = new SupportObserverEvaluator(agentContext);

        observer = new TimerIntervalObserver(1000, beginState, evaluator);
    }

    public void testStartAndObserve() {
        scheduleService.setTime(0);
        observer.startObserve();
        scheduleService.setTime(1000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));

        // Test start again
        observer.startObserve();
        scheduleService.setTime(1999);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(0, evaluator.getMatchEvents().size());

        scheduleService.setTime(2000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));
    }

    public void testStartAndStop() {
        // Start then stop
        scheduleService.setTime(0);
        observer.startObserve();
        observer.stopObserve();
        scheduleService.setTime(1000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(0, evaluator.getAndClearMatchEvents().size());

        // Test start again
        observer.startObserve();
        scheduleService.setTime(2500);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));

        observer.stopObserve();
        observer.startObserve();

        scheduleService.setTime(3500);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));
    }

    public void testImmediateTrigger() {
        // Should fireStatementStopped right away, wait time set to zero
        observer = new TimerIntervalObserver(0, beginState, evaluator);

        scheduleService.setTime(0);
        observer.startObserve();
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));
        scheduleService.setTime(10000000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(0, evaluator.getAndClearMatchEvents().size());
    }

    public void testInvalid() {
        try {
            observer.startObserve();
            observer.startObserve();
            fail();
        } catch (IllegalStateException ex) {
            // Expected exception
        }
    }

}
