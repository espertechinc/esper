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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class is always the root node in the evaluation state tree representing any activated event expression.
 * It hold the handle to a further state node with subnodes making up a whole evaluation state tree.
 */
public class EvalRootStateNode extends EvalStateNode implements Evaluator, PatternStopCallback, EvalRootState {
    protected EvalNode rootSingleChildNode;
    protected EvalStateNode topStateNode;
    private PatternMatchCallback callback;

    /**
     * Constructor.
     *
     * @param rootSingleChildNode is the root nodes single child node
     */
    public EvalRootStateNode(EvalNode rootSingleChildNode) {
        super(null);
        this.rootSingleChildNode = rootSingleChildNode;
    }

    @Override
    public EvalNode getFactoryNode() {
        return rootSingleChildNode;
    }

    /**
     * Hands the callback to use to indicate matching events.
     *
     * @param callback is invoked when the event expressions turns true.
     */
    public final void setCallback(PatternMatchCallback callback) {
        this.callback = callback;
    }

    public void startRecoverable(boolean startRecoverable, MatchedEventMap beginState) {
        start(beginState);
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternRootStart(beginState);
        }
        topStateNode = rootSingleChildNode.newState(this, null, 0L);
        topStateNode.start(beginState);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternRootStart();
        }
    }

    public final void stop() {
        quit();
    }

    public void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternRootQuit();
        }
        if (topStateNode != null) {
            topStateNode.quit();
            handleQuitEvent();
        }
        topStateNode = null;
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternRootQuit();
        }
    }

    public void handleQuitEvent() {
        // no action
    }

    public void handleChildQuitEvent() {
        // no action
    }

    public void handleEvaluateFalseEvent() {
        // no action
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternRootEvaluateTrue(matchEvent);
        }

        if (isQuitted) {
            topStateNode = null;
            handleChildQuitEvent();
        }

        callback.matchFound(matchEvent.getMatchingEventsAsMap(), optionalTriggeringEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternRootEvaluateTrue(topStateNode == null);
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternRootEvalFalse();
        }
        if (topStateNode != null) {
            topStateNode.quit();
            topStateNode = null;
            handleEvaluateFalseEvent();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternRootEvalFalse();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitRoot(this);
        if (topStateNode != null) {
            topStateNode.accept(visitor);
        }
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    public final String toString() {
        return "EvalRootStateNode topStateNode=" + topStateNode;
    }

    public EvalStateNode getTopStateNode() {
        return topStateNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (topStateNode != null) {
            topStateNode.removeMatch(matchEvent);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(EvalRootStateNode.class);
}
