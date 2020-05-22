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

public class TwoLambdaThreeFormScalarFactory implements EnumForgeDescFactory {
    private final ObjectArrayEventType typeFirst;
    private final ObjectArrayEventType typeSecond;
    private final int numParams;
    private final EPType returnType;
    private final TwoLambdaThreeFormScalarFactory.ForgeFunction function;

    public TwoLambdaThreeFormScalarFactory(ObjectArrayEventType typeFirst, ObjectArrayEventType typeSecond, int numParams, EPType returnType, TwoLambdaThreeFormScalarFactory.ForgeFunction function) {
        this.typeFirst = typeFirst;
        this.typeSecond = typeSecond;
        this.numParams = numParams;
        this.returnType = returnType;
        this.function = function;
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return parameterNum == 0 ? makeDesc(typeFirst) : makeDesc(typeSecond);
    }

    public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        ExprDotEvalParamLambda second = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);
        EnumForge forge = function.apply(first, second, typeFirst, typeSecond, streamCountIncoming, numParams, returnType);
        return new EnumForgeDesc(returnType, forge);
    }

    private static EnumForgeLambdaDesc makeDesc(ObjectArrayEventType type) {
        return new EnumForgeLambdaDesc(new EventType[]{type}, new String[]{type.getName()});
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(ExprDotEvalParamLambda first, ExprDotEvalParamLambda second, ObjectArrayEventType eventTypeFirst, ObjectArrayEventType eventTypeSecond, int streamCountIncoming, int numParams, EPType typeInfo);
    }
}
