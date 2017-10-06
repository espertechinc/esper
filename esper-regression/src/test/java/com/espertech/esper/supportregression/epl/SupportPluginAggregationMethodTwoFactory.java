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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;

import java.io.Serializable;

public class SupportPluginAggregationMethodTwoFactory implements AggregationFunctionFactory, Serializable {
    public void validate(AggregationValidationContext validationContext) {
        throw new IllegalArgumentException("Invalid parameter type '" + validationContext.getParameterTypes()[0].getName() + "', expecting string");
    }

    public void setFunctionName(String functionName) {

    }

    public AggregationMethod newAggregator() {
        return new SupportPluginAggregationMethodTwo();
    }

    public Class getValueType() {
        return null;
    }
}
