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
import com.espertech.esper.pattern.observer.EventObserver;
import com.espertech.esper.pattern.observer.ObserverEventEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


/**
 * This class represents the state of an eventObserver sub-expression in the evaluation state tree.
 */
public class EvalObserverStateNode extends EvalStateNode implements ObserverEventEvaluator {
    protected final EvalObserverNode evalObserverNode;
    protected EventObserver eventObserver;

    /**
     * Constructor.
     *
     * @param parentNode       is the parent evaluator to call to indicate truth value
     * @param evalObserverNode is the factory node associated to the state
     */
    public EvalObserverStateNode(Evaluator parentNode,
                                 EvalObserverNode evalObserverNode) {
        super(parentNode);

        this.evalObserverNode = evalObserverNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (PatternConsumptionUtil.containsEvent(matchEvent, eventObserver.getBeginState())) {
            quit();
            this.getParentEvaluator().evaluateFalse(this, true);
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalObserverNode;
    }

    @Override
    public PatternAgentInstanceContext getContext() {
        return evalObserverNode.getContext();
    }

    public void observerEvaluateTrue(MatchedEventMap matchEvent, boolean quitted) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternObserverEvaluateTrue(evalObserverNode, matchEvent);
        }
        this.getParentEvaluator().evaluateTrue(matchEvent, this, quitted, null);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternObserverEvaluateTrue();
        }
    }

    public void observerEvaluateFalse(boolean restartable) {
        this.getParentEvaluator().evaluateFalse(this, restartable);
    }

    public void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternObserverStart(evalObserverNode, beginState);
        }
        eventObserver = evalObserverNode.getFactoryNode().getObserverFactory().makeObserver(evalObserverNode.getContext(), beginState, this, null, null, this.getParentEvaluator().isFilterChildNonQuitting());
        eventObserver.startObserve();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternObserverStart();
        }
    }

    public final void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternObserverQuit(evalObserverNode);
        }
        eventObserver.stopObserve();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternObserverQuit();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitObserver(evalObserverNode.getFactoryNode(), this, eventObserver);
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return evalObserverNode.getFactoryNode().isObserverStateNodeNonRestarting();
    }

    public final String toString() {
        return "EvalObserverStateNode eventObserver=" + eventObserver;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalObserverStateNode.class);
}
