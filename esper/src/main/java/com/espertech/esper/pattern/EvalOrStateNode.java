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

import java.util.Arrays;
import java.util.Set;

/**
 * This class represents the state of a "or" operator in the evaluation state tree.
 */
public class EvalOrStateNode extends EvalStateNode implements Evaluator {
    protected final EvalOrNode evalOrNode;
    protected final EvalStateNode[] childNodes;

    /**
     * Constructor.
     *
     * @param parentNode is the parent evaluator to call to indicate truth value
     * @param evalOrNode is the factory node associated to the state
     */
    public EvalOrStateNode(Evaluator parentNode,
                           EvalOrNode evalOrNode) {
        super(parentNode);

        this.childNodes = new EvalStateNode[evalOrNode.getChildNodes().length];
        this.evalOrNode = evalOrNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        for (EvalStateNode node : childNodes) {
            if (node != null) {
                node.removeMatch(matchEvent);
            }
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalOrNode;
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternOrStart(evalOrNode, beginState);
        }
        // In an "or" expression we need to create states for all child expressions/listeners,
        // since all are going to be started
        int count = 0;
        for (EvalNode node : evalOrNode.getChildNodes()) {
            EvalStateNode childState = node.newState(this, null, 0L);
            childNodes[count++] = childState;
        }

        // In an "or" expression we start all child listeners
        EvalStateNode[] childNodeCopy = new EvalStateNode[childNodes.length];
        System.arraycopy(childNodes, 0, childNodeCopy, 0, childNodes.length);
        for (EvalStateNode child : childNodeCopy) {
            child.start(beginState);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternOrStart();
        }
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternOrEvaluateTrue(evalOrNode, matchEvent);
        }
        // If one of the children quits, the whole or expression turns true and all subexpressions must quit
        if (isQuitted) {
            for (int i = 0; i < childNodes.length; i++) {
                if (childNodes[i] == fromNode) {
                    childNodes[i] = null;
                }
            }
            quitInternal();     // Quit the remaining listeners
        }

        this.getParentEvaluator().evaluateTrue(matchEvent, this, isQuitted, optionalTriggeringEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternOrEvaluateTrue(isQuitted);
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternOrEvalFalse(evalOrNode);
        }
        for (int i = 0; i < childNodes.length; i++) {
            if (childNodes[i] == fromNode) {
                childNodes[i] = null;
            }
        }

        boolean allEmpty = true;
        for (int i = 0; i < childNodes.length; i++) {
            if (childNodes[i] != null) {
                allEmpty = false;
                break;
            }
        }

        if (allEmpty) {
            this.getParentEvaluator().evaluateFalse(this, true);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternOrEvalFalse();
        }
    }

    public final void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternOrQuit(evalOrNode);
        }
        quitInternal();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternOrQuit();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitOr(evalOrNode.getFactoryNode(), this);
        for (EvalStateNode node : childNodes) {
            if (node != null) {
                node.accept(visitor);
            }
        }
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    public final String toString() {
        return "EvalOrStateNode";
    }

    private void quitInternal() {
        for (EvalStateNode child : childNodes) {
            if (child != null) {
                child.quit();
            }
        }
        Arrays.fill(childNodes, null);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalOrStateNode.class);
}
