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
import com.espertech.esper.common.internal.rettype.EPType;

import java.util.List;

public class TwoLambdaThreeFormEventPlainFactory implements EnumForgeDescFactory {
    private final EventType eventType;
    private final String streamNameFirst;
    private final String streamNameSecond;
    private final EPType returnType;
    private final TwoLambdaThreeFormEventPlainFactory.ForgeFunction function;

    public TwoLambdaThreeFormEventPlainFactory(EventType eventType, String streamNameFirst, String streamNameSecond, EPType returnType, ForgeFunction function) {
        this.eventType = eventType;
        this.streamNameFirst = streamNameFirst;
        this.streamNameSecond = streamNameSecond;
        this.returnType = returnType;
        this.function = function;
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return new EnumForgeLambdaDesc(new EventType[]{eventType}, new String[]{parameterNum == 0 ? streamNameFirst : streamNameSecond});
    }

    public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        ExprDotEvalParamLambda second = (ExprDotEvalParamLambda) bodiesAndParameters.get(1);
        EnumForge forge = function.apply(first, second, streamCountIncoming, returnType, services);
        return new EnumForgeDesc(returnType, forge);
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(ExprDotEvalParamLambda first, ExprDotEvalParamLambda second, int streamCountIncoming, EPType typeInfo, StatementCompileTimeServices services);
    }
}
