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
package com.espertech.esper.common.internal.view.rank;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.IStreamSortRankRandomAccess;

import java.util.Comparator;

/**
 * Factory for rank window views.
 */
public class RankWindowViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    protected boolean[] isDescendingValues;
    protected ExprEvaluator criteriaEval;
    protected Class[] criteriaTypes;
    protected DataInputOutputSerde<Object> keySerde;
    protected ExprEvaluator[] sortCriteriaEvaluators;
    protected Class[] sortCriteriaTypes;
    protected ExprEvaluator size;
    protected boolean useCollatorSort;
    protected DataInputOutputSerde<Object>[] sortSerdes;

    protected EventType eventType;
    protected Comparator<Object> comparator;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        comparator = ExprNodeUtilityMake.getComparatorHashableMultiKeys(sortCriteriaTypes, useCollatorSort, isDescendingValues); // hashable-key comparator since we may remove sort keys
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int sortWindowSize = ViewFactoryUtil.evaluateSizeParam(getViewName(), size, agentInstanceViewFactoryContext.getAgentInstanceContext());
        IStreamSortRankRandomAccess rankedRandomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);
        return new RankWindowView(this, sortWindowSize, rankedRandomAccess, agentInstanceViewFactoryContext);
    }

    public RandomAccessByIndexGetter makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean[] getIsDescendingValues() {
        return isDescendingValues;
    }

    public ExprEvaluator getCriteriaEval() {
        return criteriaEval;
    }

    public ExprEvaluator[] getSortCriteriaEvaluators() {
        return sortCriteriaEvaluators;
    }

    public boolean isUseCollatorSort() {
        return useCollatorSort;
    }

    public ExprEvaluator getSizeEvaluator() {
        return size;
    }

    public Comparator<Object> getComparator() {
        return comparator;
    }

    public void setIsDescendingValues(boolean[] isDescendingValues) {
        this.isDescendingValues = isDescendingValues;
    }

    public void setCriteriaEval(ExprEvaluator criteriaEval) {
        this.criteriaEval = criteriaEval;
    }

    public void setSortCriteriaEvaluators(ExprEvaluator[] sortCriteriaEvaluators) {
        this.sortCriteriaEvaluators = sortCriteriaEvaluators;
    }

    public void setSortCriteriaTypes(Class[] sortCriteriaTypes) {
        this.sortCriteriaTypes = sortCriteriaTypes;
    }

    public void setSize(ExprEvaluator size) {
        this.size = size;
    }

    public void setUseCollatorSort(boolean useCollatorSort) {
        this.useCollatorSort = useCollatorSort;
    }

    public void setCriteriaTypes(Class[] criteriaTypes) {
        this.criteriaTypes = criteriaTypes;
    }

    public String getViewName() {
        return ViewEnum.RANK_WINDOW.getName();
    }

    public void setKeySerde(DataInputOutputSerde<Object> keySerde) {
        this.keySerde = keySerde;
    }

    public void setSortSerdes(DataInputOutputSerde<Object>[] sortSerdes) {
        this.sortSerdes = sortSerdes;
    }
}
