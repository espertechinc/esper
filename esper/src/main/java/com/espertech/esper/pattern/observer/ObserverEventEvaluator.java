/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.pattern.observer;

import com.espertech.esper.pattern.*;

/**
 * For use by {@link EventObserver} instances to place an event for processing/evaluation.
 */
public interface ObserverEventEvaluator
{
    /**
     * Indicate an event for evaluation (sub-expression the observer represents has turned true).
     * @param matchEvent is the matched events so far
     * @param quitted whether the observer quit, usually "true" for most observers
     */
    public void observerEvaluateTrue(MatchedEventMap matchEvent, boolean quitted);

    /**
     * Indicate that the observer turned permanently false.
     * @param restartable
     */
    public void observerEvaluateFalse(boolean restartable);

    public PatternAgentInstanceContext getContext();
}
