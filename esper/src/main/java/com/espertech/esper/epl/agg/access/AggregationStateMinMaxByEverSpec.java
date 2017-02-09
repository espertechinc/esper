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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.epl.expression.core.ExprEvaluator;

import java.util.Comparator;

public class AggregationStateMinMaxByEverSpec {
    protected final int streamId;
    private final ExprEvaluator[] criteria;
    private final boolean max;
    private final Comparator<Object> comparator;
    private Object criteriaKeyBinding;

    public AggregationStateMinMaxByEverSpec(int streamId, ExprEvaluator[] criteria, boolean max, Comparator<Object> comparator, Object criteriaKeyBinding) {
        this.streamId = streamId;
        this.criteria = criteria;
        this.max = max;
        this.comparator = comparator;
        this.criteriaKeyBinding = criteriaKeyBinding;
    }

    public int getStreamId() {
        return streamId;
    }

    public ExprEvaluator[] getCriteria() {
        return criteria;
    }

    public boolean isMax() {
        return max;
    }

    public Comparator<Object> getComparator() {
        return comparator;
    }

    public Object getCriteriaKeyBinding() {
        return criteriaKeyBinding;
    }

    public void setCriteriaKeyBinding(Object criteriaKeyBinding) {
        this.criteriaKeyBinding = criteriaKeyBinding;
    }
}
