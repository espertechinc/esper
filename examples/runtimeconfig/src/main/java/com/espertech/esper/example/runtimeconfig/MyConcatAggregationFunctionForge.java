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

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionForge;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionMode;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionModeManaged;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionValidationContext;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategy;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;

public class MyConcatAggregationFunctionForge implements AggregationFunctionForge {

    public void validate(AggregationFunctionValidationContext validationContext) {
        if ((validationContext.getParameterTypes().length != 1) ||
            (validationContext.getParameterTypes()[0] != String.class)) {
            throw new IllegalArgumentException("Concat aggregation requires a single parameter of type String");
        }
    }

    public AggregationFunctionMode getAggregationFunctionMode() {
        // Inject a factory by using "new"
        InjectionStrategy injectionStrategy = new InjectionStrategyClassNewInstance(MyConcatAggregationFunctionFactory.class);

        // The managed mode means there is no need to write code that generates code
        AggregationFunctionModeManaged mode = new AggregationFunctionModeManaged();
        mode.setInjectionStrategyAggregationFunctionFactory(injectionStrategy);

        return mode;
    }

    public Class getValueType() {
        return String.class;
    }

    public void setFunctionName(String functionName) {
        // not required here
    }
}
