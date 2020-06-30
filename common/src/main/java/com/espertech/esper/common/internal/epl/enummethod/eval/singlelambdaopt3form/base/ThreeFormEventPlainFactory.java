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
import com.espertech.esper.common.internal.rettype.EPChainableType;

public class ThreeFormEventPlainFactory extends ThreeFormBaseFactory {
    private final EventType eventType;
    private final String streamName;
    private final ForgeFunction function;

    public ThreeFormEventPlainFactory(ThreeFormInitFunction returnType, EventType eventType, String streamName, ForgeFunction function) {
        super(returnType);
        this.eventType = eventType;
        this.streamName = streamName;
        this.function = function;
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return new EnumForgeLambdaDesc(new EventType[]{eventType}, new String[]{streamName});
    }

    protected EnumForge makeForgeWithParam(ExprDotEvalParamLambda lambda, EPChainableType typeInfo, StatementCompileTimeServices services) {
        return function.apply(lambda, typeInfo, services);
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(ExprDotEvalParamLambda lambda, EPChainableType typeInfo, StatementCompileTimeServices services);
    }
}
