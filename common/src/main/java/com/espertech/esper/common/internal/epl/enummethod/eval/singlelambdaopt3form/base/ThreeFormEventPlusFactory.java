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
import com.espertech.esper.common.internal.rettype.EPChainableType;

public class ThreeFormEventPlusFactory extends ThreeFormBaseFactory {
    private final EventType eventType;
    private final String streamName;
    private final ObjectArrayEventType fieldType;
    private final int numParameters;
    private final ForgeFunction function;

    public ThreeFormEventPlusFactory(ThreeFormInitFunction returnType, EventType eventType, String streamName, ObjectArrayEventType fieldType, int numParameters, ForgeFunction function) {
        super(returnType);
        this.eventType = eventType;
        this.streamName = streamName;
        this.fieldType = fieldType;
        this.numParameters = numParameters;
        this.function = function;
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return new EnumForgeLambdaDesc(new EventType[]{eventType, fieldType}, new String[]{streamName, fieldType.getName()});
    }

    protected EnumForge makeForgeWithParam(ExprDotEvalParamLambda lambda, EPChainableType typeInfo, StatementCompileTimeServices services) {
        return function.apply(lambda, fieldType, numParameters, typeInfo, services);
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldType, int numParameters, EPChainableType typeInfo, StatementCompileTimeServices services);
    }
}
