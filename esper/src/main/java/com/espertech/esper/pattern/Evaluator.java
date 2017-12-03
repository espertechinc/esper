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
package com.espertech.esper.pattern;


import com.espertech.esper.client.EventBean;
import com.espertech.esper.filterspec.MatchedEventMap;

/**
 * Interface for nodes in an expression evaluation state tree that are being informed by a child that the
 * event expression fragments (subtrees) which the child represents has turned true (evaluateTrue method)
 * or false (evaluateFalse).
 */
public interface Evaluator {
    /**
     * Indicate a change in truth value to true.
     *  @param matchEvent is the container for events that caused the change in truth value
     * @param fromNode   is the node that indicates the change
     * @param isQuitted  is an indication of whether the node continues listenening or stops listening
     * @param optionalTriggeringEvent in case the truth value changed to true in direct response to an event arriving, provides that event
     */
    public void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent);

    /**
     * Indicate a change in truth value to false.
     *
     * @param fromNode    is the node that indicates the change
     * @param restartable whether the evaluator can be restarted
     */
    public void evaluateFalse(EvalStateNode fromNode, boolean restartable);

    public boolean isFilterChildNonQuitting();
}
