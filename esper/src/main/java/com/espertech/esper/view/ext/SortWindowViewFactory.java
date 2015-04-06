/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprOrderedExpr;
import com.espertech.esper.view.*;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;

import java.util.List;

/**
 * Factory for sort window views.
 */
public class SortWindowViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious
{
    private final static String NAME = "Sort";

    private List<ExprNode> viewParameters;

    /**
     * The sort-by expressions.
     */
    protected ExprNode[] sortCriteriaExpressions;


    /**
     * The flags defining the ascending or descending sort order.
     */
    protected boolean[] isDescendingValues;

    /**
     * The sort window size.
     */
    protected int sortWindowSize;

    private EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParams) throws ViewParameterException
    {
        this.viewParameters = viewParams;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException
    {
        eventType = parentEventType;
        String message = NAME + " window requires a numeric size parameter and a list of expressions providing sort keys";
        if (viewParameters.size() < 2)
        {
            throw new ViewParameterException(message);
        }

        ExprNode[] validated = ViewFactorySupport.validate(NAME + " window", parentEventType, statementContext, viewParameters, true);
        for (int i = 1; i < validated.length; i++)
        {
            ViewFactorySupport.assertReturnsNonConstant(NAME + " window", validated[i], i);
        }

        ExprEvaluatorContextStatement exprEvaluatorContext = new ExprEvaluatorContextStatement(statementContext, false);
        Object sortSize = ViewFactorySupport.evaluateAssertNoProperties(NAME + " window", validated[0], 0, exprEvaluatorContext);
        if ((sortSize == null) || (!(sortSize instanceof Number)))
        {
            throw new ViewParameterException(message);
        }
        sortWindowSize = ((Number) sortSize).intValue();

        sortCriteriaExpressions = new ExprNode[validated.length - 1];
        isDescendingValues = new boolean[sortCriteriaExpressions.length];

        for (int i = 1; i < validated.length; i++)
        {
            if (validated[i] instanceof ExprOrderedExpr)
            {
                isDescendingValues[i - 1] = ((ExprOrderedExpr) validated[i]).isDescending();
                sortCriteriaExpressions[i - 1] = validated[i].getChildNodes()[0];
            }
            else
            {
                sortCriteriaExpressions[i - 1] = validated[i];
            }
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext)
    {
        IStreamSortRankRandomAccess sortedRandomAccess = ViewServiceHelper.getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);

        boolean useCollatorSort = false;
        if (agentInstanceViewFactoryContext.getAgentInstanceContext().getStatementContext().getConfigSnapshot() != null)
        {
            useCollatorSort = agentInstanceViewFactoryContext.getAgentInstanceContext().getStatementContext().getConfigSnapshot().getEngineDefaults().getLanguage().isSortUsingCollator();
        }

        ExprEvaluator[] childEvals = ExprNodeUtility.getEvaluators(sortCriteriaExpressions);
        return new SortWindowView(this, sortCriteriaExpressions, childEvals, isDescendingValues, sortWindowSize, sortedRandomAccess, useCollatorSort, agentInstanceViewFactoryContext);
    }

    public Object makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public boolean canReuse(View view)
    {
        if (!(view instanceof SortWindowView))
        {
            return false;
        }

        SortWindowView other = (SortWindowView) view;
        if ((other.getSortWindowSize() != sortWindowSize) ||
            (!compare(other.getIsDescendingValues(), isDescendingValues)) ||
            (!ExprNodeUtility.deepEquals(other.getSortCriteriaExpressions(), sortCriteriaExpressions)) )
        {
            return false;
        }

        return other.isEmpty();
    }

    public String getViewName() {
        return NAME;
    }

    private boolean compare(boolean[] one, boolean[] two)
    {
        if (one.length != two.length)
        {
            return false;
        }

        for (int i = 0; i < one.length; i++)
        {
            if (one[i] != two[i])
            {
                return false;
            }
        }

        return true;
    }
}
