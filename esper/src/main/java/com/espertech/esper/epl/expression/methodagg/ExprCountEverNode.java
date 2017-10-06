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

import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.core.ExprWildcard;

/**
 * Represents the "countever" aggregate function is an expression tree.
 */
public class ExprCountEverNode extends ExprAggregateNodeBase {
    private static final long serialVersionUID = 1436994080693454617L;

    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprCountEverNode(boolean distinct) {
        super(distinct);
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length > 2) {
            throw makeExceptionExpectedParamNum(0, 2);
        }
        if (super.isDistinct) {
            throw new ExprValidationException("Aggregation function '" + getAggregationFunctionName() + "' does now allow distinct");
        }

        boolean ignoreNulls = false;
        if (positionalParams.length == 0) {
            // no parameters is allowed
        } else {
            ignoreNulls = !(positionalParams[0] instanceof ExprWildcard);
            if (positionalParams.length == 2) {
                super.validateFilter(positionalParams[1].getForge());
            }
        }
        return validationContext.getEngineImportService().getAggregationFactoryFactory().makeCountEver(validationContext.getStatementExtensionSvcContext(), this, ignoreNulls);
    }

    public boolean hasFilter() {
        return positionalParams.length == 2;
    }

    public String getAggregationFunctionName() {
        return "countever";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return node instanceof ExprCountEverNode;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}