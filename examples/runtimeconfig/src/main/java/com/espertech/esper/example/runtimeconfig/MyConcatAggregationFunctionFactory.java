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
package com.espertech.esper.example.runtimeconfig;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;

public class MyConcatAggregationFunctionFactory implements AggregationFunctionFactory {

    public void validate(AggregationValidationContext validationContext) {
        if ((validationContext.getParameterTypes().length != 1) ||
                (validationContext.getParameterTypes()[0] != String.class)) {
            throw new IllegalArgumentException("Concat aggregation requires a single parameter of type String");
        }
    }

    public Class getValueType() {
        return String.class;
    }

    public void setFunctionName(String functionName) {
        // not required here
    }

    public AggregationMethod newAggregator() {
        return new MyConcatAggregationFunction();
    }
}
