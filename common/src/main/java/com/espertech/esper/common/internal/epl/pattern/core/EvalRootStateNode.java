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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class is always the root node in the evaluation state tree representing any activated event expression.
 * It hold the handle to a further state node with subnodes making up a whole evaluation state tree.
 */
public class EvalRootStateNode extends EvalStateNode implements Evaluator, EvalRootState {
    protected final EvalRootNode rootNode;
    protected EvalNode rootSingleChildNode;
    protected EvalStateNode topStateNode;
    private PatternMatchCallback callback;

    /**
     * Constructor.
     *
     * @param rootNode            root node
     * @param rootSingleChildNode is the root nodes single child node
     */
    public EvalRootStateNode(EvalRootNode rootNode, EvalNode rootSingleChildNode) {
        super(null);
        this.rootNode = rootNode;
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

    public void quit() {
        rootNode.agentInstanceContext.getInstrumentationProvider().qPatternRootQuit();
        if (topStateNode != null) {
            topStateNode.quit();
        }
        topStateNode = null;
        rootNode.agentInstanceContext.getInstrumentationProvider().aPatternRootQuit();
    }

    public final void start(MatchedEventMap beginState) {
        rootNode.agentInstanceContext.getInstrumentationProvider().qPatternRootStart(beginState);
        topStateNode = rootSingleChildNode.newState(this);
        topStateNode.start(beginState);
        rootNode.agentInstanceContext.getInstrumentationProvider().aPatternRootStart();
    }

    public final void stop() {
        quit();
    }

    public void startRecoverable(boolean startRecoverable, MatchedEventMap beginState) {
        start(beginState);
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = rootNode.getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternRootEvaluateTrue(matchEvent);

        if (isQuitted) {
            topStateNode = null;
        }

        callback.matchFound(matchEvent.getMatchingEventsAsMap(), optionalTriggeringEvent);
        agentInstanceContext.getInstrumentationProvider().aPatternRootEvaluateTrue(isQuitted);
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        AgentInstanceContext agentInstanceContext = rootNode.getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternRootEvalFalse();

        if (topStateNode != null) {
            topStateNode.quit();
            topStateNode = null;
        }

        agentInstanceContext.getInstrumentationProvider().aPatternRootEvalFalse();
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
