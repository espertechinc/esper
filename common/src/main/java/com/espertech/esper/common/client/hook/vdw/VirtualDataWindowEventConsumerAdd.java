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
package com.espertech.esper.common.client.hook.vdw;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

/**
 * Event indicating a named-window consuming statement is being added.
 */
public class VirtualDataWindowEventConsumerAdd extends VirtualDataWindowEventConsumerBase {

    private final ExprEvaluator filter;
    private final ExprEvaluatorContext exprEvaluatorContext;

    /**
     * Ctor.
     *
     * @param namedWindowName      the named window name
     * @param consumerObject       an object that identifies the consumer, the same instance or the add and for the remove event
     * @param statementName        statement name
     * @param agentInstanceId      agent instance id
     * @param filter               filter expressions
     * @param exprEvaluatorContext for expression evaluation
     */
    public VirtualDataWindowEventConsumerAdd(String namedWindowName, Object consumerObject, String statementName, int agentInstanceId, ExprEvaluator filter, ExprEvaluatorContext exprEvaluatorContext) {
        super(namedWindowName, consumerObject, statementName, agentInstanceId);
        this.filter = filter;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    /**
     * Provides the filter expressions.
     * <p>
     * Evaluate filter expressions, if any, as follows:
     * {@code
     * Boolean pass = filter[...].getExprEvaluator().evaluate(new EventBean[] {vdwEvent}, true, addEvent.getExprEvaluatorContext());
     * }
     * </p>
     * <p>
     * Filter expressions must be evaluated using the same ExprEvaluatorContext instance as provided by this event.
     * </p>
     *
     * @return filter expression list
     */
    public ExprEvaluator getFilter() {
        return filter;
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
