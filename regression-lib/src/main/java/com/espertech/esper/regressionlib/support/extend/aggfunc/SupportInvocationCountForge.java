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
package com.espertech.esper.regressionlib.support.extend.aggfunc;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionForge;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionMode;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionModeMultiParam;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionValidationContext;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class SupportInvocationCountForge implements AggregationFunctionForge {
    public void setFunctionName(String functionName) {

    }

    public void validate(AggregationFunctionValidationContext validationContext) throws ExprValidationException {

    }

    public Class getValueType() {
        return int.class;
    }

    public AggregationFunctionMode getAggregationFunctionMode() {
        return new AggregationFunctionModeMultiParam().setInjectionStrategyAggregationFunctionFactory(new InjectionStrategyClassNewInstance(SupportInvocationCountFactory.class));
    }
}
