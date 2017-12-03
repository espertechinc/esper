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
package com.espertech.esper.supportunit.guard;

import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.pattern.observer.ObserverEventEvaluator;

import java.util.LinkedList;
import java.util.List;

public class SupportObserverEvaluator implements ObserverEventEvaluator {
    private List<MatchedEventMap> matchEvents = new LinkedList<MatchedEventMap>();
    private int evaluateFalseCounter;
    private PatternAgentInstanceContext context;

    public SupportObserverEvaluator(PatternAgentInstanceContext context) {
        this.context = context;
    }

    public void observerEvaluateTrue(MatchedEventMap matchEvent, boolean quitted) {
        matchEvents.add(matchEvent);
    }

    public void observerEvaluateFalse(boolean restartable) {
        evaluateFalseCounter++;
    }

    public List<MatchedEventMap> getAndClearMatchEvents() {
        List<MatchedEventMap> original = matchEvents;
        matchEvents = new LinkedList<MatchedEventMap>();
        return original;
    }

    public List<MatchedEventMap> getMatchEvents() {
        return matchEvents;
    }

    public int getAndResetEvaluateFalseCounter() {
        int value = evaluateFalseCounter;
        evaluateFalseCounter = 0;
        return value;
    }

    public PatternAgentInstanceContext getContext() {
        return context;
    }
}
