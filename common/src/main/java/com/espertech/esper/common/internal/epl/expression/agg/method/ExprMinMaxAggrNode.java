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
package com.espertech.esper.common.internal.epl.expression.agg.method;

import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.method.minmax.AggregationForgeFactoryMinMax;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

/**
 * Represents the min/max(distinct? ...) aggregate function is an expression tree.
 */
public class ExprMinMaxAggrNode extends ExprAggregateNodeBase {

    private final MinMaxTypeEnum minMaxTypeEnum;
    private final boolean isFFunc;
    private final boolean isEver;
    private boolean hasFilter;

    public ExprMinMaxAggrNode(boolean distinct, MinMaxTypeEnum minMaxTypeEnum, boolean isFFunc, boolean isEver) {
        super(distinct);
        this.minMaxTypeEnum = minMaxTypeEnum;
        this.isFFunc = isFFunc;
        this.isEver = isEver;
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length == 0 || positionalParams.length > 2) {
            throw new ExprValidationException(minMaxTypeEnum.toString() + " node must have either 1 or 2 parameters");
        }

        ExprNode child = positionalParams[0];
        boolean hasDataWindows;
        if (isEver) {
            hasDataWindows = false;
        } else {
            if (validationContext.getStatementType() == StatementType.CREATE_TABLE) {
                hasDataWindows = true;
            } else {
                hasDataWindows = ExprNodeUtilityAggregation.hasRemoveStreamForAggregations(child, validationContext.getStreamTypeService(), validationContext.isResettingAggregations());
            }
        }

        if (isFFunc) {
            if (positionalParams.length < 2) {
                throw new ExprValidationException(minMaxTypeEnum.toString() + "-filtered aggregation function must have a filter expression as a second parameter");
            }
            super.validateFilter(positionalParams[1]);
        }

        hasFilter = positionalParams.length == 2;
        if (hasFilter) {
            optionalFilter = positionalParams[1];
        }

        Class evaluationType = child.getForge().getEvaluationType();
        DataInputOutputSerdeForge serde = validationContext.getSerdeResolver().serdeForAggregation(evaluationType, validationContext.getStatementRawInfo());
        DataInputOutputSerdeForge distinctSerde = isDistinct ? validationContext.getSerdeResolver().serdeForAggregationDistinct(evaluationType, validationContext.getStatementRawInfo()) : null;
        return new AggregationForgeFactoryMinMax(this, evaluationType, hasDataWindows, serde, distinctSerde);
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprMinMaxAggrNode)) {
            return false;
        }

        ExprMinMaxAggrNode other = (ExprMinMaxAggrNode) node;
        return other.minMaxTypeEnum == this.minMaxTypeEnum && other.isEver == this.isEver;
    }

    /**
     * Returns the indicator for minimum or maximum.
     *
     * @return min/max indicator
     */
    public MinMaxTypeEnum getMinMaxTypeEnum() {
        return minMaxTypeEnum;
    }

    public boolean isHasFilter() {
        return hasFilter;
    }

    public String getAggregationFunctionName() {
        return minMaxTypeEnum.getExpressionText();
    }

    public boolean isEver() {
        return isEver;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}
