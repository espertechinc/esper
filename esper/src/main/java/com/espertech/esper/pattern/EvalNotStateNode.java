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
 * This class contains the state of an 'not' operator in the evaluation state tree.
 * The not operator inverts the truth of the subexpression under it. It defaults to being true rather than
 * being false at startup. True at startup means it will generate an event on newState such that parent expressions
 * may turn true. It turns permenantly false when it receives an event from a subexpression and the subexpression
 * quitted. It indicates the false state via an evaluateFalse call on its parent evaluator.
 */
public class EvalNotStateNode extends EvalStateNode implements Evaluator {
    protected final EvalNotNode evalNotNode;
    protected EvalStateNode childNode;

    /**
     * Constructor.
     *
     * @param parentNode  is the parent evaluator to call to indicate truth value
     * @param evalNotNode is the factory node associated to the state
     */
    public EvalNotStateNode(Evaluator parentNode,
                            EvalNotNode evalNotNode) {
        super(parentNode);

        this.evalNotNode = evalNotNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        // The not-operator does not pass along the matches
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalNotNode;
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternNotStart(evalNotNode, beginState);
        }
        childNode = evalNotNode.getChildNode().newState(this, null, 0L);
        childNode.start(beginState);

        // The not node acts by inverting the truth
        // By default the child nodes are false. This not node acts inverts the truth and pretends the child is true,
        // raising an event up.
        this.getParentEvaluator().evaluateTrue(beginState, this, false, null);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternNotStart();
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternNotEvalFalse(evalNotNode);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternNotEvalFalse();
        }
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternNotEvaluateTrue(evalNotNode, matchEvent);
        }
        // Only is the subexpression stopped listening can we tell the parent evaluator that this
        // turned permanently false.
        if (isQuitted) {
            childNode = null;
            this.getParentEvaluator().evaluateFalse(this, true);
        } else {
            // If the subexpression did not quit, we stay in the "true" state
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternNotEvaluateTrue(isQuitted);
        }
    }

    public final void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternNotQuit(evalNotNode);
        }
        if (childNode != null) {
            childNode.quit();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternNotQuit();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitNot(evalNotNode.getFactoryNode(), this);
        if (childNode != null) {
            childNode.accept(visitor);
        }
    }

    public boolean isNotOperator() {
        return true;
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
        return "EvalNotStateNode child=" + childNode;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalNotStateNode.class);
}
