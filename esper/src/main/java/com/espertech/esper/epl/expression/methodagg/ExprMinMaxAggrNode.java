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
package com.espertech.esper.epl.expression.methodagg;

import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.type.MinMaxTypeEnum;

/**
 * Represents the min/max(distinct? ...) aggregate function is an expression tree.
 */
public class ExprMinMaxAggrNode extends ExprAggregateNodeBase {
    private final MinMaxTypeEnum minMaxTypeEnum;
    private static final long serialVersionUID = -7828413362615586145L;

    private final boolean hasFilter;
    private final boolean isEver;

    public ExprMinMaxAggrNode(boolean distinct, MinMaxTypeEnum minMaxTypeEnum, boolean hasFilter, boolean isEver) {
        super(distinct);
        this.minMaxTypeEnum = minMaxTypeEnum;
        this.hasFilter = hasFilter;
        this.isEver = isEver;
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length == 0 || positionalParams.length > 2) {
            throw new ExprValidationException(minMaxTypeEnum.toString() + " node must have either 1 or 2 parameters");
        }

        ExprNode child = positionalParams[0];
        boolean hasDataWindows;
        if (isEver) {
            hasDataWindows = false;
        } else {
            if (validationContext.getExprEvaluatorContext().getStatementType() == StatementType.CREATE_TABLE) {
                hasDataWindows = true;
            } else {
                hasDataWindows = ExprNodeUtility.hasRemoveStreamForAggregations(child, validationContext.getStreamTypeService(), validationContext.isResettingAggregations());
            }
        }

        if (hasFilter) {
            if (positionalParams.length < 2) {
                throw new ExprValidationException(minMaxTypeEnum.toString() + "-filtered aggregation function must have a filter expression as a second parameter");
            }
            super.validateFilter(positionalParams[1].getExprEvaluator());
        }
        return validationContext.getEngineImportService().getAggregationFactoryFactory().makeMinMax(validationContext.getStatementExtensionSvcContext(), this, child.getExprEvaluator().getType(), hasDataWindows);
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
}
