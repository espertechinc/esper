/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExprNodeAdapterBase
{
    private static final Log log = LogFactory.getLog(ExprNodeAdapterBase.class);

    protected final ExprNode exprNode;
    protected final ExprEvaluator exprNodeEval;
    protected final ExprEvaluatorContext evaluatorContext;

    /**
     * Ctor.
     * @param exprNode is the boolean expression
     */
    public ExprNodeAdapterBase(ExprNode exprNode, ExprEvaluatorContext evaluatorContext)
    {
        this.exprNode = exprNode;
        this.exprNodeEval = exprNode.getExprEvaluator();
        this.evaluatorContext = evaluatorContext;
    }

    /**
     * Evaluate the boolean expression given the event as a stream zero event.
     *
     * @param theEvent is the stream zero event (current event)
     * @return boolean result of the expression
     */
    public boolean evaluate(EventBean theEvent)
    {
        return evaluatePerStream(new EventBean[] {theEvent});
    }

    protected boolean evaluatePerStream(EventBean[] eventsPerStream) {
        try {
            Boolean result = (Boolean) exprNodeEval.evaluate(eventsPerStream, true, this.evaluatorContext);
            if (result == null)
            {
                return false;
            }
            return result;
        }
        catch (RuntimeException ex) {
            log.error("Error evaluating expression '" + ExprNodeUtility.toExpressionStringMinPrecedenceSafe(exprNode) + "' statement '" + getStatementName() + "': " + ex.getMessage(), ex);
            return false;
        }
    }

    public String getStatementName() {
        return evaluatorContext.getStatementName();
    }

    public ExprNode getExprNode() {
        return exprNode;
    }
}
