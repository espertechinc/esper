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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class SupportAggregateExprNode extends ExprAggregateNodeBase {
    private static int validateCount;

    private Class type;
    private Object value;
    private int validateCountSnapshot;

    public static void setValidateCount(int validateCount) {
        SupportAggregateExprNode.validateCount = validateCount;
    }

    public SupportAggregateExprNode(Class type) {
        super(false);
        this.type = type;
        this.value = null;
    }

    public SupportAggregateExprNode(Object value) {
        super(false);
        this.type = value.getClass();
        this.value = value;
    }

    public SupportAggregateExprNode(Object value, Class type) {
        super(false);
        this.value = value;
        this.type = type;
    }

    protected AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        // Keep a count for if and when this was validated
        validateCount++;
        validateCountSnapshot = validateCount;
        return null;
    }

    public int getValidateCountSnapshot() {
        return validateCountSnapshot;
    }

    public AggregationMethod getAggregationFunction() {
        return null;
    }

    public String getAggregationFunctionName() {
        return "support";
    }

    public boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void evaluateEnter(EventBean[] eventsPerStream) {
    }

    public void evaluateLeave(EventBean[] eventsPerStream) {
    }

    public void setValue(Object value) {
        this.value = value;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return true;
    }
}
