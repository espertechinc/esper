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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

/**
 * Interface for factories for making observer instances.
 */
public interface ObserverFactory {
    /**
     * Make an observer instance.
     *
     * @param context                  - services that may be required by observer implementation
     * @param beginState               - start state for observer
     * @param observerEventEvaluator   - receiver for events observed
     * @param observerState            - state node for observer
     * @param isFilterChildNonQuitting true for non-quitting filter
     * @return observer instance
     */
    public EventObserver makeObserver(PatternAgentInstanceContext context,
                                      MatchedEventMap beginState,
                                      ObserverEventEvaluator observerEventEvaluator,
                                      Object observerState,
                                      boolean isFilterChildNonQuitting);

    boolean isNonRestarting();
}
