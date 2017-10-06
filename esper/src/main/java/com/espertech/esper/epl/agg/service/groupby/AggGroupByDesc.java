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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.epl.agg.service.common.AggregationRowStateForgeDesc;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggGroupByDesc {
    private final AggregationRowStateForgeDesc rowStateForgeDescs;
    private final boolean isJoin;
    private final boolean isUnidirectional;
    private final boolean isFireAndForget;
    private final boolean isOnSelect;
    private final ExprNode[] groupByNodes;

    private boolean refcounted;
    private boolean reclaimAged;
    private AggSvcGroupByReclaimAgedEvalFuncFactory reclaimEvaluationFunctionMaxAge;
    private AggSvcGroupByReclaimAgedEvalFuncFactory reclaimEvaluationFunctionFrequency;

    public AggGroupByDesc(AggregationRowStateForgeDesc rowStateForgeDescs, boolean isJoin, boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect, ExprNode[] groupByNodes) {
        this.rowStateForgeDescs = rowStateForgeDescs;
        this.isJoin = isJoin;
        this.isUnidirectional = isUnidirectional;
        this.isFireAndForget = isFireAndForget;
        this.isOnSelect = isOnSelect;
        this.groupByNodes = groupByNodes;
    }

    public AggregationRowStateForgeDesc getRowStateForgeDescs() {
        return rowStateForgeDescs;
    }

    public boolean isJoin() {
        return isJoin;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
    }

    public boolean isFireAndForget() {
        return isFireAndForget;
    }

    public boolean isOnSelect() {
        return isOnSelect;
    }

    public boolean isRefcounted() {
        return refcounted;
    }

    public boolean isReclaimAged() {
        return reclaimAged;
    }

    public AggSvcGroupByReclaimAgedEvalFuncFactory getReclaimEvaluationFunctionMaxAge() {
        return reclaimEvaluationFunctionMaxAge;
    }

    public AggSvcGroupByReclaimAgedEvalFuncFactory getReclaimEvaluationFunctionFrequency() {
        return reclaimEvaluationFunctionFrequency;
    }

    public ExprNode[] getGroupByNodes() {
        return groupByNodes;
    }

    public void setRefcounted(boolean refcounted) {
        this.refcounted = refcounted;
    }

    public void setReclaimAged(boolean reclaimAged) {
        this.reclaimAged = reclaimAged;
    }

    public void setReclaimEvaluationFunctionMaxAge(AggSvcGroupByReclaimAgedEvalFuncFactory reclaimEvaluationFunctionMaxAge) {
        this.reclaimEvaluationFunctionMaxAge = reclaimEvaluationFunctionMaxAge;
    }

    public void setReclaimEvaluationFunctionFrequency(AggSvcGroupByReclaimAgedEvalFuncFactory reclaimEvaluationFunctionFrequency) {
        this.reclaimEvaluationFunctionFrequency = reclaimEvaluationFunctionFrequency;
    }
}
