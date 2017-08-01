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
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

/**
 * NFA state for a single match that applies a filter.
 */
public class RegexNFAStateFilter extends RegexNFAStateBase implements RegexNFAState {
    private final ExprNode exprNode;
    private final ExprEvaluator expression;
    private final boolean exprRequiresMultimatchState;

    /**
     * Ctor.
     *
     * @param nodeNum                     node num
     * @param variableName                variable name
     * @param streamNum                   stream number
     * @param multiple                    true for multiple matches
     * @param expression                  filter expression
     * @param exprRequiresMultimatchState indicator for multi-match state required
     */
    public RegexNFAStateFilter(String nodeNum, String variableName, int streamNum, boolean multiple, Pair<ExprNode, ExprEvaluator> expression, boolean exprRequiresMultimatchState) {
        super(nodeNum, variableName, streamNum, multiple, null);
        this.exprNode = expression.getFirst();
        this.expression = expression.getSecond();
        this.exprRequiresMultimatchState = exprRequiresMultimatchState;
    }

    public boolean matches(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprBool(exprNode, eventsPerStream);
        }
        Boolean result = (Boolean) expression.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (result != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprBool(result);
            }
            return result;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprBool(false);
        }
        return false;
    }

    public String toString() {
        return "FilterEvent";
    }

    public boolean isExprRequiresMultimatchState() {
        return exprRequiresMultimatchState;
    }
}
