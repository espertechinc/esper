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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.core.ExprOrderedExpr;
import com.espertech.esper.view.*;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;

import java.util.Comparator;
import java.util.List;

/**
 * Factory for sort window views.
 */
public class SortWindowViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    private final static String NAME = "Sort";

    private List<ExprNode> viewParameters;

    /**
     * The sort-by expressions.
     */
    protected ExprNode[] sortCriteriaExpressions;

    protected ExprEvaluator[] sortCriteriaEvaluators;

    /**
     * The flags defining the ascending or descending sort order.
     */
    protected boolean[] isDescendingValues;

    /**
     * The sort window size.
     */
    protected ExprEvaluator sizeEvaluator;

    protected Comparator<Object> comparator;

    private EventType eventType;
    private boolean useCollatorSort = false;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParams) throws ViewParameterException {
        this.viewParameters = viewParams;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        eventType = parentEventType;
        String message = NAME + " window requires a numeric size parameter and a list of expressions providing sort keys";
        if (viewParameters.size() < 2) {
            throw new ViewParameterException(message);
        }

        ExprNode[] validated = ViewFactorySupport.validate(NAME + " window", parentEventType, statementContext, viewParameters, true);
        for (int i = 1; i < validated.length; i++) {
            ViewFactorySupport.assertReturnsNonConstant(NAME + " window", validated[i], i);
        }

        ViewFactorySupport.validateNoProperties(getViewName(), validated[0], 0);
        sizeEvaluator = ViewFactorySupport.validateSizeParam(getViewName(), statementContext, validated[0], 0);

        sortCriteriaExpressions = new ExprNode[validated.length - 1];
        isDescendingValues = new boolean[sortCriteriaExpressions.length];

        for (int i = 1; i < validated.length; i++) {
            if (validated[i] instanceof ExprOrderedExpr) {
                isDescendingValues[i - 1] = ((ExprOrderedExpr) validated[i]).isDescending();
                sortCriteriaExpressions[i - 1] = validated[i].getChildNodes()[0];
            } else {
                sortCriteriaExpressions[i - 1] = validated[i];
            }
        }
        sortCriteriaEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(sortCriteriaExpressions, statementContext.getEngineImportService(), SortWindowViewFactory.class, false, statementContext.getStatementName());

        if (statementContext.getConfigSnapshot() != null) {
            useCollatorSort = statementContext.getConfigSnapshot().getEngineDefaults().getLanguage().isSortUsingCollator();
        }

        comparator = ExprNodeUtilityCore.getComparatorHashableMultiKeys(sortCriteriaExpressions, useCollatorSort, isDescendingValues); // hashable-key comparator since we may remove sort keys
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int sortWindowSize = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceViewFactoryContext.getAgentInstanceContext());
        IStreamSortRankRandomAccess sortedRandomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);
        return new SortWindowView(this, sortWindowSize, sortedRandomAccess, agentInstanceViewFactoryContext);
    }

    public Object makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof SortWindowView)) {
            return false;
        }

        SortWindowView other = (SortWindowView) view;
        int sortWindowSize = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceContext);
        if ((other.getSortWindowSize() != sortWindowSize) ||
                (!compare(other.getIsDescendingValues(), isDescendingValues)) ||
                (!ExprNodeUtilityCore.deepEquals(other.getSortCriteriaExpressions(), sortCriteriaExpressions, false))) {
            return false;
        }

        return other.isEmpty();
    }

    public String getViewName() {
        return NAME;
    }

    public ExprEvaluator[] getSortCriteriaEvaluators() {
        return sortCriteriaEvaluators;
    }

    public ExprNode[] getSortCriteriaExpressions() {
        return sortCriteriaExpressions;
    }

    public ExprEvaluator getSizeEvaluator() {
        return sizeEvaluator;
    }

    public Comparator<Object> getComparator() {
        return comparator;
    }

    public boolean[] getIsDescendingValues() {
        return isDescendingValues;
    }

    public boolean isUseCollatorSort() {
        return useCollatorSort;
    }

    private boolean compare(boolean[] one, boolean[] two) {
        if (one.length != two.length) {
            return false;
        }

        for (int i = 0; i < one.length; i++) {
            if (one[i] != two[i]) {
                return false;
            }
        }

        return true;
    }
}
