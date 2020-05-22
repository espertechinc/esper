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
package com.espertech.esper.common.internal.epl.enummethod.eval.twolambda.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeLambdaDesc;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPType;

import java.util.List;

public class TwoLambdaThreeFormEventPlusFactory implements EnumForgeDescFactory {
    private final EventType inputEventType;
    private final String streamNameFirst;
    private final String streamNameSecond;
    private final ObjectArrayEventType typeKey;
    private final ObjectArrayEventType typeValue;
    private final int numParams;
    private final EPType returnType;
    private final TwoLambdaThreeFormEventPlusFactory.ForgeFunction function;

    public TwoLambdaThreeFormEventPlusFactory(EventType inputEventType, String streamNameFirst, String streamNameSecond, ObjectArrayEventType typeKey, ObjectArrayEventType typeValue, int numParams, EPType returnType, ForgeFunction function) {
        this.inputEventType = inputEventType;
        this.streamNameFirst = streamNameFirst;
        this.streamNameSecond = streamNameSecond;
        this.typeKey = typeKey;
        this.typeValue = typeValue;
        this.numParams = numParams;
        this.returnType = returnType;
        this.function = function;
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return parameterNum == 0 ? makeDesc(typeKey, streamNameFirst) : makeDesc(typeValue, streamNameSecond);
    }

    public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
        ExprDotEvalParamLambda key = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        ExprDotEvalParamLambda value = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);
        EnumForge forge = function.apply(key, value, streamCountIncoming, typeKey, typeValue, numParams, returnType, services);
        return new EnumForgeDesc(returnType, forge);
    }

    private EnumForgeLambdaDesc makeDesc(ObjectArrayEventType type, String streamName) {
        return new EnumForgeLambdaDesc(new EventType[]{inputEventType, type}, new String[]{streamName, type.getName()});
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(ExprDotEvalParamLambda first, ExprDotEvalParamLambda second, int streamCountIncoming, ObjectArrayEventType firstType, ObjectArrayEventType secondType, int numParameters, EPType typeInfo, StatementCompileTimeServices services);
    }
}
