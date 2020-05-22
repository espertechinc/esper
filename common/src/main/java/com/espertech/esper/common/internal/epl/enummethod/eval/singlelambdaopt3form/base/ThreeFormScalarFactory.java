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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeLambdaDesc;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPType;

import java.util.function.Function;

public class ThreeFormScalarFactory extends ThreeFormBaseFactory {
    private final ObjectArrayEventType eventType;
    private final int numParams;
    private final ForgeFunction function;

    public ThreeFormScalarFactory(Function<ExprDotEvalParamLambda, EPType> returnType, ObjectArrayEventType eventType, int numParams, ForgeFunction function) {
        super(returnType);
        this.eventType = eventType;
        this.numParams = numParams;
        this.function = function;
    }

    protected EnumForge makeForgeWithParam(ExprDotEvalParamLambda lambda, EPType typeInfo, StatementCompileTimeServices services) {
        return function.apply(lambda, eventType, numParams, typeInfo, services);
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return new EnumForgeLambdaDesc(new EventType[]{eventType}, new String[]{eventType.getName()});
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(ExprDotEvalParamLambda lambda, ObjectArrayEventType eventType, int numParams, EPType typeInfo, StatementCompileTimeServices services);
    }
}
