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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowStateForgeDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class AggGroupByDesc {
    private final AggregationRowStateForgeDesc rowStateForgeDescs;
    private final boolean isUnidirectional;
    private final boolean isFireAndForget;
    private final boolean isOnSelect;
    private final ExprNode[] groupByNodes;
    private final MultiKeyClassRef groupByMultiKey;

    private boolean refcounted;
    private boolean reclaimAged;
    private AggSvcGroupByReclaimAgedEvalFuncFactoryForge reclaimEvaluationFunctionMaxAge;
    private AggSvcGroupByReclaimAgedEvalFuncFactoryForge reclaimEvaluationFunctionFrequency;

    public AggGroupByDesc(AggregationRowStateForgeDesc rowStateForgeDescs, boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect, ExprNode[] groupByNodes, MultiKeyClassRef groupByMultiKey) {
        this.rowStateForgeDescs = rowStateForgeDescs;
        this.isUnidirectional = isUnidirectional;
        this.isFireAndForget = isFireAndForget;
        this.isOnSelect = isOnSelect;
        this.groupByNodes = groupByNodes;
        this.groupByMultiKey = groupByMultiKey;
    }

    public AggregationRowStateForgeDesc getRowStateForgeDescs() {
        return rowStateForgeDescs;
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

    public AggSvcGroupByReclaimAgedEvalFuncFactoryForge getReclaimEvaluationFunctionMaxAge() {
        return reclaimEvaluationFunctionMaxAge;
    }

    public AggSvcGroupByReclaimAgedEvalFuncFactoryForge getReclaimEvaluationFunctionFrequency() {
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

    public void setReclaimEvaluationFunctionMaxAge(AggSvcGroupByReclaimAgedEvalFuncFactoryForge reclaimEvaluationFunctionMaxAge) {
        this.reclaimEvaluationFunctionMaxAge = reclaimEvaluationFunctionMaxAge;
    }

    public void setReclaimEvaluationFunctionFrequency(AggSvcGroupByReclaimAgedEvalFuncFactoryForge reclaimEvaluationFunctionFrequency) {
        this.reclaimEvaluationFunctionFrequency = reclaimEvaluationFunctionFrequency;
    }

    public Object getNumMethods() {
        return rowStateForgeDescs.getNumMethods();
    }

    public Object getNumAccess() {
        return rowStateForgeDescs.getNumAccess();
    }

    public MultiKeyClassRef getGroupByMultiKey() {
        return groupByMultiKey;
    }
}
