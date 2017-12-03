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

import java.util.Set;

/**
 * Superclass of all state nodes in an evaluation node tree representing an event expressions.
 * Follows the Composite pattern. Subclasses are expected to keep their own collection containing child nodes
 * as needed.
 */
public abstract class EvalStateNode {
    private Evaluator parentEvaluator;

    /**
     * Starts the event expression or an instance of it.
     * Child classes are expected to initialize and start any event listeners
     * or schedule any time-based callbacks as needed.
     *
     * @param beginState begin state
     */
    public abstract void start(MatchedEventMap beginState);

    /**
     * Stops the event expression or an instance of it. Child classes are expected to free resources
     * and stop any event listeners or remove any time-based callbacks.
     */
    public abstract void quit();

    /**
     * Accept a visitor. Child classes are expected to invoke the visit method on the visitor instance
     * passed in.
     *
     * @param visitor on which the visit method is invoked by each node
     */
    public abstract void accept(EvalStateNodeVisitor visitor);

    /**
     * Returns the factory node for the state node.
     *
     * @return factory node
     */
    public abstract EvalNode getFactoryNode();

    public abstract boolean isNotOperator();

    public abstract boolean isFilterStateNode();

    public abstract boolean isObserverStateNodeNonRestarting();

    /**
     * Remove matches that overlap with the provided events.
     *
     * @param matchEvent set of events to check for
     */
    public abstract void removeMatch(Set<EventBean> matchEvent);

    /**
     * Constructor.
     *
     * @param parentNode is the evaluator for this node on which to indicate a change in truth value
     */
    public EvalStateNode(Evaluator parentNode) {
        this.parentEvaluator = parentNode;
    }

    /**
     * Returns the parent evaluator.
     *
     * @return parent evaluator instance
     */
    public final Evaluator getParentEvaluator() {
        return parentEvaluator;
    }

    /**
     * Sets the parent evaluator.
     *
     * @param parentEvaluator for this node
     */
    public final void setParentEvaluator(Evaluator parentEvaluator) {
        this.parentEvaluator = parentEvaluator;
    }
}
