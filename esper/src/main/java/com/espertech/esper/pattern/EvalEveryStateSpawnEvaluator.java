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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the state of an 'every' operator in the evaluation state tree.
 * EVERY nodes work as a factory for new state subnodes. When a child node of an EVERY
 * node calls the evaluateTrue method on the EVERY node, the EVERY node will call newState on its child
 * node BEFORE it calls evaluateTrue on its parent node. It keeps a reference to the new child in
 * its list. (BEFORE because the root node could call quit on child nodes for stopping all
 * listeners).
 */
public final class EvalEveryStateSpawnEvaluator implements Evaluator {
    private boolean isEvaluatedTrue;

    private final String statementName;

    public EvalEveryStateSpawnEvaluator(String statementName) {
        this.statementName = statementName;
    }

    public final boolean isEvaluatedTrue() {
        return isEvaluatedTrue;
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        log.warn("Event/request processing: Uncontrolled pattern matching of \"every\" operator - infinite loop when using EVERY operator on expression(s) containing a not operator, for statement '" + statementName + "'");
        isEvaluatedTrue = true;
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        log.warn("Event/request processing: Uncontrolled pattern matching of \"every\" operator - infinite loop when using EVERY operator on expression(s) containing a not operator, for statement '" + statementName + "'");
        isEvaluatedTrue = true;
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalEveryStateSpawnEvaluator.class);
}

