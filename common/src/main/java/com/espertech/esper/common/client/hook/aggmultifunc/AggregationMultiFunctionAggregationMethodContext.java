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
package com.espertech.esper.common.client.hook.aggmultifunc;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;

/**
 * Context for use with plug-in aggregation multi-functions aggregation methods.
 */
public class AggregationMultiFunctionAggregationMethodContext {
    private final String aggregationMethodName;
    private final ExprNode[] parameters;
    private final ExprValidationContext validationContext;

    /**
     * Ctor.
     * @param aggregationMethodName name of aggregation method
     * @param parameters parameter expressions
     * @param validationContext validation context
     */
    public AggregationMultiFunctionAggregationMethodContext(String aggregationMethodName, ExprNode[] parameters, ExprValidationContext validationContext) {
        this.aggregationMethodName = aggregationMethodName;
        this.parameters = parameters;
        this.validationContext = validationContext;
    }

    /**
     * Returns the aggregation method name.
     * @return name
     */
    public String getAggregationMethodName() {
        return aggregationMethodName;
    }

    /**
     * Returns the parameter expressions
     * @return params
     */
    public ExprNode[] getParameters() {
        return parameters;
    }

    /**
     * Returns the validation context
     * @return validation context
     */
    public ExprValidationContext getValidationContext() {
        return validationContext;
    }
}
