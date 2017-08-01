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

public class AggregationStateSortedSpec {
    private final int streamId;
    private final ExprEvaluator[] criteria;
    private final Class[] criteriaTypes;
    private final Comparator<Object> comparator;
    private Object criteriaKeyBinding;
    private final ExprEvaluator optionalFilter;

    public AggregationStateSortedSpec(int streamId, ExprEvaluator[] criteria, Class[] criteriaTypes, Comparator<Object> comparator, Object criteriaKeyBinding, ExprEvaluator optionalFilter) {
        this.streamId = streamId;
        this.criteria = criteria;
        this.criteriaTypes = criteriaTypes;
        this.comparator = comparator;
        this.criteriaKeyBinding = criteriaKeyBinding;
        this.optionalFilter = optionalFilter;
    }

    public int getStreamId() {
        return streamId;
    }

    public ExprEvaluator[] getCriteria() {
        return criteria;
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

    public ExprEvaluator getOptionalFilter() {
        return optionalFilter;
    }

    public Class[] getCriteriaTypes() {
        return criteriaTypes;
    }
}
