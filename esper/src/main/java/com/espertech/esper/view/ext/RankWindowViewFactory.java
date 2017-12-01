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
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.view.*;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;

import java.util.Comparator;
import java.util.List;

/**
 * Factory for rank window views.
 */
public class RankWindowViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    private final static String NAME = "Rank";

    private List<ExprNode> viewParameters;

    /**
     * The unique-by expressions.
     */
    protected ExprNode[] uniqueCriteriaExpressions;

    /**
     * The sort-by expressions.
     */
    protected ExprNode[] sortCriteriaExpressions;


    /**
     * The flags defining the ascending or descending sort order.
     */
    protected boolean[] isDescendingValues;


    protected ExprEvaluator[] uniqueEvals;
    protected ExprEvaluator[] sortEvals;

    /**
     * The sort window size.
     */
    protected ExprEvaluator sizeEvaluator;

    private EventType eventType;

    protected boolean useCollatorSort;

    protected Comparator<Object> comparator;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParams) throws ViewParameterException {
        this.viewParameters = viewParams;
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        eventType = parentEventType;
        String message = NAME + " view requires a list of expressions providing unique keys, a numeric size parameter and a list of expressions providing sort keys";
        if (viewParameters.size() < 3) {
            throw new ViewParameterException(message);
        }

        // validate
        ExprNode[] validated = ViewFactorySupport.validate(NAME, parentEventType, statementContext, viewParameters, true);

        // find size-parameter index
        int indexNumericSize = -1;
        for (int i = 0; i < validated.length; i++) {
            if (validated[i] instanceof ExprConstantNode || validated[i] instanceof ExprContextPropertyNode) {
                indexNumericSize = i;
                break;
            }
        }
        if (indexNumericSize == -1) {
            throw new ViewParameterException("Failed to find constant value for the numeric size parameter");
        }
        if (indexNumericSize == 0) {
            throw new ViewParameterException("Failed to find unique value expressions that are expected to occur before the numeric size parameter");
        }
        if (indexNumericSize == validated.length - 1) {
            throw new ViewParameterException("Failed to find sort key expressions after the numeric size parameter");
        }

        // validate non-constant for unique-keys and sort-keys
        for (int i = 0; i < indexNumericSize; i++) {
            ViewFactorySupport.assertReturnsNonConstant(NAME, validated[i], i);
        }
        for (int i = indexNumericSize + 1; i < validated.length; i++) {
            ViewFactorySupport.assertReturnsNonConstant(NAME, validated[i], i);
        }

        // get sort size
        ViewFactorySupport.validateNoProperties(getViewName(), validated[indexNumericSize], indexNumericSize);
        sizeEvaluator = ViewFactorySupport.validateSizeParam(getViewName(), statementContext, validated[indexNumericSize], indexNumericSize);

        // compile unique expressions
        uniqueCriteriaExpressions = new ExprNode[indexNumericSize];
        System.arraycopy(validated, 0, uniqueCriteriaExpressions, 0, indexNumericSize);

        // compile sort expressions
        sortCriteriaExpressions = new ExprNode[validated.length - indexNumericSize - 1];
        isDescendingValues = new boolean[sortCriteriaExpressions.length];

        int count = 0;
        for (int i = indexNumericSize + 1; i < validated.length; i++) {
            if (validated[i] instanceof ExprOrderedExpr) {
                isDescendingValues[count] = ((ExprOrderedExpr) validated[i]).isDescending();
                sortCriteriaExpressions[count] = validated[i].getChildNodes()[0];
            } else {
                sortCriteriaExpressions[count] = validated[i];
            }
            count++;
        }

        this.uniqueEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(uniqueCriteriaExpressions, statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
        this.sortEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(sortCriteriaExpressions, statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());

        if (statementContext.getConfigSnapshot() != null) {
            useCollatorSort = statementContext.getConfigSnapshot().getEngineDefaults().getLanguage().isSortUsingCollator();
        }

        comparator = ExprNodeUtilityCore.getComparatorHashableMultiKeys(sortCriteriaExpressions, useCollatorSort, isDescendingValues); // hashable-key comparator since we may remove sort keys
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int sortWindowSize = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceViewFactoryContext.getAgentInstanceContext());
        IStreamSortRankRandomAccess rankedRandomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);
        return new RankWindowView(this, sortWindowSize, rankedRandomAccess, agentInstanceViewFactoryContext);
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

    public String getViewName() {
        return NAME;
    }

    public boolean[] getIsDescendingValues() {
        return isDescendingValues;
    }

    public ExprEvaluator[] getUniqueEvals() {
        return uniqueEvals;
    }

    public ExprEvaluator[] getSortEvals() {
        return sortEvals;
    }

    public boolean isUseCollatorSort() {
        return useCollatorSort;
    }

    public ExprNode[] getUniqueCriteriaExpressions() {
        return uniqueCriteriaExpressions;
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
}
