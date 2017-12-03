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

import com.espertech.esper.core.support.SupportSchedulingServiceImpl;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.schedule.ScheduleSpec;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.supportunit.guard.SupportObserverEvaluator;
import com.espertech.esper.supportunit.pattern.SupportPatternContextFactory;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import com.espertech.esper.type.ScheduleUnit;
import junit.framework.TestCase;

public class TestTimerCronObserver extends TestCase {
    private TimerAtObserver observer;
    private SchedulingServiceImpl scheduleService;
    private SupportObserverEvaluator evaluator;
    private MatchedEventMap beginState;

    public void setUp() {
        beginState = new MatchedEventMapImpl(new MatchedEventMapMeta(new String[0], false));

        scheduleService = new SchedulingServiceImpl(new TimeSourceServiceImpl());
        PatternAgentInstanceContext agentContext = SupportPatternContextFactory.makePatternAgentInstanceContext(scheduleService);

        ScheduleSpec scheduleSpec = new ScheduleSpec();
        scheduleSpec.addValue(ScheduleUnit.SECONDS, 1);

        evaluator = new SupportObserverEvaluator(agentContext);

        observer = new TimerAtObserver(scheduleSpec, beginState, evaluator);
    }

    public void testStartAndObserve() {
        scheduleService.setTime(0);
        observer.startObserve();
        scheduleService.setTime(1000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));

        // Test start again
        observer.startObserve();
        scheduleService.setTime(60999);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(0, evaluator.getMatchEvents().size());

        scheduleService.setTime(61000); // 1 minute plus 1 second
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
        scheduleService.setTime(61000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));

        observer.stopObserve();
        observer.startObserve();

        scheduleService.setTime(150000);
        SupportSchedulingServiceImpl.evaluateSchedule(scheduleService);
        assertEquals(beginState, evaluator.getAndClearMatchEvents().get(0));
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
