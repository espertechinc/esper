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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.epl.pattern.and.EvalAndFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.followedby.EvalFollowedByFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.ExpressionGuardFactory;
import com.espertech.esper.common.internal.epl.pattern.guard.TimerWithinGuardFactory;
import com.espertech.esper.common.internal.epl.pattern.guard.TimerWithinOrMaxCountGuardFactory;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.not.EvalNotFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.TimerAtObserverFactory;
import com.espertech.esper.common.internal.epl.pattern.observer.TimerIntervalObserverFactory;
import com.espertech.esper.common.internal.epl.pattern.observer.TimerScheduleObserverFactory;
import com.espertech.esper.common.internal.epl.pattern.or.EvalOrFactoryNode;

public class PatternFactoryServiceImpl implements PatternFactoryService {
    public final static PatternFactoryServiceImpl INSTANCE = new PatternFactoryServiceImpl();

    private PatternFactoryServiceImpl() {
    }

    public EvalRootFactoryNode root() {
        return new EvalRootFactoryNode();
    }

    public EvalObserverFactoryNode observer() {
        return new EvalObserverFactoryNode();
    }

    public EvalGuardFactoryNode guard() {
        return new EvalGuardFactoryNode();
    }

    public TimerWithinGuardFactory guardTimerWithin() {
        return new TimerWithinGuardFactory();
    }

    public TimerIntervalObserverFactory observerTimerInterval() {
        return new TimerIntervalObserverFactory();
    }

    public EvalAndFactoryNode and() {
        return new EvalAndFactoryNode();
    }

    public EvalOrFactoryNode or() {
        return new EvalOrFactoryNode();
    }

    public EvalFilterFactoryNode filter() {
        return new EvalFilterFactoryNode();
    }

    public EvalEveryFactoryNode every() {
        return new EvalEveryFactoryNode();
    }

    public EvalNotFactoryNode not() {
        return new EvalNotFactoryNode();
    }

    public EvalFollowedByFactoryNode followedby() {
        return new EvalFollowedByFactoryNode();
    }

    public EvalMatchUntilFactoryNode matchUntil() {
        return new EvalMatchUntilFactoryNode();
    }

    public TimerWithinOrMaxCountGuardFactory guardTimerWithinOrMax() {
        return new TimerWithinOrMaxCountGuardFactory();
    }

    public EvalEveryDistinctFactoryNode everyDistinct() {
        return new EvalEveryDistinctFactoryNode();
    }

    public TimerAtObserverFactory observerTimerAt() {
        return new TimerAtObserverFactory();
    }

    public TimerScheduleObserverFactory observerTimerSchedule() {
        return new TimerScheduleObserverFactory();
    }

    public ExpressionGuardFactory guardWhile() {
        return new ExpressionGuardFactory();
    }
}
