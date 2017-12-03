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

import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.supportunit.pattern.SupportMatchedEventConvertor;
import com.espertech.esper.supportunit.pattern.SupportPatternContextFactory;
import com.espertech.esper.view.TestViewSupport;
import junit.framework.TestCase;

public class TestTimerObserverFactory extends TestCase {
    private PatternAgentInstanceContext patternContext;

    public void setUp() {
        patternContext = SupportPatternContextFactory.makePatternAgentInstanceContext();
    }

    public void testIntervalWait() throws Exception {
        TimerIntervalObserverFactory factory = new TimerIntervalObserverFactory();
        factory.setObserverParameters(TestViewSupport.toExprListBean(new Object[]{1}), new SupportMatchedEventConvertor(), null);
        EventObserver eventObserver = factory.makeObserver(patternContext, null, new SupportObserverEventEvaluator(patternContext), null, null, false);

        assertTrue(eventObserver instanceof TimerIntervalObserver);
    }

    private static class SupportObserverEventEvaluator implements ObserverEventEvaluator {
        private final PatternAgentInstanceContext patternContext;

        private SupportObserverEventEvaluator(PatternAgentInstanceContext patternContext) {
            this.patternContext = patternContext;
        }

        public void observerEvaluateTrue(MatchedEventMap matchEvent, boolean quitted) {
        }

        public void observerEvaluateFalse(boolean restartable) {
        }

        public PatternAgentInstanceContext getContext() {
            return patternContext;
        }
    }
}
