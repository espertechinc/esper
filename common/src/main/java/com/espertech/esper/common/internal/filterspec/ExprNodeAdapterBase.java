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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExprNodeAdapterBase {
    private static final Logger log = LoggerFactory.getLogger(ExprNodeAdapterBase.class);

    protected final FilterSpecParamExprNode factory;
    protected final ExprEvaluatorContext evaluatorContext;

    /**
     * Evaluate the boolean expression given the event as a stream zero event.
     *
     * @param theEvent is the stream zero event (current event)
     * @return boolean result of the expression
     */
    public abstract boolean evaluate(EventBean theEvent);

    public ExprNodeAdapterBase(FilterSpecParamExprNode factory, ExprEvaluatorContext evaluatorContext) {
        this.factory = factory;
        this.evaluatorContext = evaluatorContext;
    }

    protected final boolean evaluatePerStream(EventBean[] eventsPerStream) {
        try {
            Boolean result = (Boolean) factory.getExprNode().evaluate(eventsPerStream, true, this.evaluatorContext);
            if (result == null) {
                return false;
            }
            return result;
        } catch (RuntimeException ex) {
            String message = "Error evaluating expression '" + factory.getExprText() + "' statement '" + getStatementName() + "': " + ex.getMessage();
            log.error(message, ex);
            throw new EPException(message, ex);
        }
    }

    public String getStatementName() {
        return evaluatorContext.getStatementName();
    }

    public int getStatementId() {
        return evaluatorContext.getStatementId();
    }

    public int getFilterBoolExprNum() {
        return factory.getFilterBoolExprId();
    }

    public ExprEvaluatorContext getEvaluatorContext() {
        return evaluatorContext;
    }

    public int getStatementIdBoolExpr() {
        return factory.getStatementIdBooleanExpr();
    }

    public String getExpression() {
        return factory.getExprText();
    }

    /**
     * NOTE: Overridden by subclasses as additional information is required for multistream-equals
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExprNodeAdapterBase that = (ExprNodeAdapterBase) o;
        return evaluatorContext.getStatementId() == that.evaluatorContext.getStatementId() &&
                evaluatorContext.getAgentInstanceId() == that.evaluatorContext.getAgentInstanceId() &&
                factory.getFilterBoolExprId() == that.factory.getFilterBoolExprId();
    }

    public int hashCode() {
        int result = evaluatorContext.getStatementId();
        result = 31 * result + evaluatorContext.getAgentInstanceId();
        result = 31 * result + factory.getFilterBoolExprId();
        return result;
    }
}
