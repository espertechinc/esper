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

public interface PatternFactoryService {
    EvalRootFactoryNode root();

    EvalObserverFactoryNode observer();

    EvalGuardFactoryNode guard();

    EvalAndFactoryNode and();

    EvalOrFactoryNode or();

    EvalFilterFactoryNode filter();

    EvalEveryFactoryNode every();

    EvalNotFactoryNode not();

    EvalFollowedByFactoryNode followedby();

    EvalMatchUntilFactoryNode matchUntil();

    EvalEveryDistinctFactoryNode everyDistinct();

    TimerIntervalObserverFactory observerTimerInterval();

    TimerAtObserverFactory observerTimerAt();

    TimerScheduleObserverFactory observerTimerSchedule();

    TimerWithinGuardFactory guardTimerWithin();

    TimerWithinOrMaxCountGuardFactory guardTimerWithinOrMax();

    ExpressionGuardFactory guardWhile();
}
