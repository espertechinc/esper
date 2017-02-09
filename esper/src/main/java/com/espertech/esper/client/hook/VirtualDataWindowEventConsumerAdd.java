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
package com.espertech.esper.client.hook;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;

/**
 * Event indicating a named-window consuming statement is being added.
 */
public class VirtualDataWindowEventConsumerAdd extends VirtualDataWindowEventConsumerBase {

    private final ExprNode[] filterExpressions;
    private final ExprEvaluatorContext exprEvaluatorContext;

    /**
     * Ctor.
     *
     * @param namedWindowName      the named window name
     * @param consumerObject       an object that identifies the consumer, the same instance or the add and for the remove event
     * @param statementName        statement name
     * @param agentInstanceId      agent instance id
     * @param filterExpressions    filter expressions
     * @param exprEvaluatorContext for expression evaluation
     */
    public VirtualDataWindowEventConsumerAdd(String namedWindowName, Object consumerObject, String statementName, int agentInstanceId, ExprNode[] filterExpressions, ExprEvaluatorContext exprEvaluatorContext) {
        super(namedWindowName, consumerObject, statementName, agentInstanceId);
        this.filterExpressions = filterExpressions;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    /**
     * Provides the filter expressions.
     * <p>
     * Evaluate filter expressions, if any, as follows:
     * <code>
     * Boolean pass = filterExpressions[...].getExprEvaluator().evaluate(new EventBean[] {vdwEvent}, true, addEvent.getExprEvaluatorContext());
     * </code>
     * </p>
     * <p>
     * Filter expressions must be evaluated using the same ExprEvaluatorContext instance as provided by this event.
     * </p>
     *
     * @return filter expression list
     */
    public ExprNode[] getFilterExpressions() {
        return filterExpressions;
    }

    /**
     * Returns the expression evaluator context for evaluating filter expressions.
     *
     * @return expression evaluator context
     */
    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }
}
