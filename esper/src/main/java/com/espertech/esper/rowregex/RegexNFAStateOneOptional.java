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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

/**
 * The '?' state in the regex NFA states.
 */
public class RegexNFAStateOneOptional extends RegexNFAStateBase implements RegexNFAState {
    private ExprEvaluator exprEvaluator;
    private boolean exprRequiresMultimatchState;

    /**
     * Ctor.
     *
     * @param nodeNum                     node num
     * @param variableName                variable name
     * @param streamNum                   stream number
     * @param multiple                    true for multiple matches
     * @param isGreedy                    true for greedy
     * @param exprEvaluator                    filter expression
     * @param exprRequiresMultimatchState indicator for multi-match state required
     */
    public RegexNFAStateOneOptional(String nodeNum, String variableName, int streamNum, boolean multiple, boolean isGreedy, ExprEvaluator exprEvaluator, boolean exprRequiresMultimatchState) {
        super(nodeNum, variableName, streamNum, multiple, isGreedy);
        this.exprEvaluator = exprEvaluator;
        this.exprRequiresMultimatchState = exprRequiresMultimatchState;
    }

    public boolean matches(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (exprEvaluator == null) {
            return true;
        }

        Boolean result = (Boolean) exprEvaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (result != null) {
            return result;
        }
        return false;
    }

    public String toString() {
        if (exprEvaluator == null) {
            return "OptionalFilterEvent";
        }
        return "OptionalFilterEvent-Filtered";
    }

    public boolean isExprRequiresMultimatchState() {
        return exprRequiresMultimatchState;
    }
}
