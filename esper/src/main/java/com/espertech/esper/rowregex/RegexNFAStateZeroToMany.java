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
import com.espertech.esper.epl.expression.core.ExprNode;

/**
 * The '*' state in the regex NFA states.
 */
public class RegexNFAStateZeroToMany extends RegexNFAStateBase implements RegexNFAState {
    private ExprEvaluator exprNode;
    private boolean exprRequiresMultimatchState;

    /**
     * Ctor.
     *
     * @param nodeNum                     node num
     * @param variableName                variable name
     * @param streamNum                   stream number
     * @param multiple                    true for multiple matches
     * @param isGreedy                    true for greedy
     * @param exprNode                    filter expression
     * @param exprRequiresMultimatchState indicator for multi-match state required
     */
    public RegexNFAStateZeroToMany(String nodeNum, String variableName, int streamNum, boolean multiple, boolean isGreedy, ExprNode exprNode, boolean exprRequiresMultimatchState) {
        super(nodeNum, variableName, streamNum, multiple, isGreedy);
        this.exprNode = exprNode == null ? null : exprNode.getExprEvaluator();
        this.exprRequiresMultimatchState = exprRequiresMultimatchState;
        this.addState(this);
    }

    public boolean matches(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (exprNode == null) {
            return true;
        }
        Boolean result = (Boolean) exprNode.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (result != null) {
            return result;
        }
        return false;
    }

    public String toString() {
        if (exprNode == null) {
            return "ZeroMany-Unfiltered";
        }
        return "ZeroMany-Filtered";
    }

    public boolean isExprRequiresMultimatchState() {
        return exprRequiresMultimatchState;
    }
}
