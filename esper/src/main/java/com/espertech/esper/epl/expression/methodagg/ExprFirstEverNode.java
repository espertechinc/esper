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

import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;

/**
 * Represents the "firstever" aggregate function is an expression tree.
 */
public class ExprFirstEverNode extends ExprAggregateNodeBase {
    private static final long serialVersionUID = 1436994080693454617L;

    /**
     * Ctor.
     *
     * @param distinct - flag indicating unique or non-unique value aggregation
     */
    public ExprFirstEverNode(boolean distinct) {
        super(distinct);
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        if (positionalParams.length > 2) {
            throw makeExceptionExpectedParamNum(0, 2);
        }
        if (positionalParams.length == 2) {
            super.validateFilter(positionalParams[1].getExprEvaluator());
        }
        return validationContext.getEngineImportService().getAggregationFactoryFactory().makeFirstEver(validationContext.getStatementExtensionSvcContext(), this, positionalParams[0].getExprEvaluator().getType());
    }

    public boolean hasFilter() {
        return positionalParams.length == 2;
    }

    public String getAggregationFunctionName() {
        return "firstever";
    }

    public final boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        return node instanceof ExprFirstEverNode;
    }
}