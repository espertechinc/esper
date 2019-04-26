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
package com.espertech.esper.common.internal.view.sort;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.IStreamSortRankRandomAccess;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.Comparator;

/**
 * Factory for sort window views.
 */
public class SortWindowViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious {
    protected ExprEvaluator[] sortCriteriaEvaluators;
    protected Class[] sortCriteriaTypes;
    protected DataInputOutputSerde<Object>[] sortSerdes;
    protected boolean[] isDescendingValues;
    protected ExprEvaluator size;
    protected boolean useCollatorSort;

    protected Comparator<Object> comparator;
    protected EventType eventType;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        comparator = ExprNodeUtilityMake.getComparatorHashableMultiKeys(sortCriteriaTypes, useCollatorSort, isDescendingValues); // hashable-key comparator since we may remove sort keys
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int sortWindowSize = ViewFactoryUtil.evaluateSizeParam(getViewName(), size, agentInstanceViewFactoryContext.getAgentInstanceContext());
        IStreamSortRankRandomAccess sortedRandomAccess = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprSortedRankedAccess(agentInstanceViewFactoryContext);
        return new SortWindowView(this, sortWindowSize, sortedRandomAccess, agentInstanceViewFactoryContext);
    }

    public PreviousGetterStrategy makePreviousGetter() {
        return new RandomAccessByIndexGetter();
    }

    public EventType getEventType() {
        return eventType;
    }

    public ExprEvaluator[] getSortCriteriaEvaluators() {
        return sortCriteriaEvaluators;
    }

    public ExprEvaluator getSize() {
        return size;
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

    public void setSortCriteriaEvaluators(ExprEvaluator[] sortCriteriaEvaluators) {
        this.sortCriteriaEvaluators = sortCriteriaEvaluators;
    }

    public void setSortCriteriaTypes(Class[] sortCriteriaTypes) {
        this.sortCriteriaTypes = sortCriteriaTypes;
    }

    public void setIsDescendingValues(boolean[] isDescendingValues) {
        this.isDescendingValues = isDescendingValues;
    }

    public void setSize(ExprEvaluator size) {
        this.size = size;
    }

    public void setUseCollatorSort(boolean useCollatorSort) {
        this.useCollatorSort = useCollatorSort;
    }

    public void setSortSerdes(DataInputOutputSerde<Object>[] sortSerdes) {
        this.sortSerdes = sortSerdes;
    }

    public String getViewName() {
        return ViewEnum.SORT_WINDOW.getName();
    }
}
