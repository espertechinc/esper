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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethod;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationGivenEvent;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.table.core.Table;

public class ExprTableEvalStrategyFactory {
    private ExprTableEvalStrategyEnum strategyEnum;
    private Table table;
    private ExprEvaluator groupKeyEval;
    private int aggColumnNum = -1;
    private int propertyIndex;
    private ExprEnumerationGivenEvent optionalEnumEval;
    private AggregationMultiFunctionAggregationMethod aggregationMethod;

    public void setStrategyEnum(ExprTableEvalStrategyEnum strategyEnum) {
        this.strategyEnum = strategyEnum;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void setGroupKeyEval(ExprEvaluator groupKeyEval) {
        this.groupKeyEval = groupKeyEval;
    }

    public void setAggColumnNum(int aggColumnNum) {
        this.aggColumnNum = aggColumnNum;
    }

    public void setPropertyIndex(int propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public void setOptionalEnumEval(ExprEnumerationGivenEvent optionalEnumEval) {
        this.optionalEnumEval = optionalEnumEval;
    }

    public void setAggregationMethod(AggregationMultiFunctionAggregationMethod aggregationMethod) {
        this.aggregationMethod = aggregationMethod;
    }

    public ExprTableEvalStrategy makeStrategy(TableAndLockProvider provider) {
        switch (strategyEnum) {
            case UNGROUPED_TOP:
                return new ExprTableEvalStrategyUngroupedTopLevel((TableAndLockProviderUngrouped) provider, this);
            case GROUPED_TOP:
                return new ExprTableEvalStrategyGroupedTopLevel((TableAndLockProviderGrouped) provider, this);
            case UNGROUPED_AGG_SIMPLE:
                return new ExprTableEvalStrategyUngroupedAggSimple((TableAndLockProviderUngrouped) provider, this);
            case GROUPED_AGG_SIMPLE:
                return new ExprTableEvalStrategyGroupedAggSimple((TableAndLockProviderGrouped) provider, this);
            case UNGROUPED_PLAINCOL:
                return new ExprTableEvalStrategyUngroupedProp((TableAndLockProviderUngrouped) provider, this);
            case GROUPED_PLAINCOL:
                return new ExprTableEvalStrategyGroupedProp((TableAndLockProviderGrouped) provider, this);
            case UNGROUPED_AGG_ACCESSREAD:
                return new ExprTableEvalStrategyUngroupedAggAccessRead((TableAndLockProviderUngrouped) provider, this);
            case GROUPED_AGG_ACCESSREAD:
                return new ExprTableEvalStrategyGroupedAggAccessRead((TableAndLockProviderGrouped) provider, this);
            case KEYS:
                return new ExprTableEvalStrategyGroupedKeys((TableAndLockProviderGrouped) provider, this);
            default:
                throw new IllegalStateException("Unrecognized strategy " + strategyEnum);
        }
    }

    public ExprTableEvalStrategyEnum getStrategyEnum() {
        return strategyEnum;
    }

    public Table getTable() {
        return table;
    }

    public ExprEvaluator getGroupKeyEval() {
        return groupKeyEval;
    }

    public int getAggColumnNum() {
        return aggColumnNum;
    }

    public int getPropertyIndex() {
        return propertyIndex;
    }

    public ExprEnumerationGivenEvent getOptionalEnumEval() {
        return optionalEnumEval;
    }

    public AggregationMultiFunctionAggregationMethod getAggregationMethod() {
        return aggregationMethod;
    }
}
