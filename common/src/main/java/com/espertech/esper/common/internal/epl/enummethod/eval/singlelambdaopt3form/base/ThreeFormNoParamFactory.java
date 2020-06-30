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
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeLambdaDesc;
import com.espertech.esper.common.internal.rettype.EPChainableType;

import java.util.List;

public class ThreeFormNoParamFactory implements EnumForgeDescFactory {
    private final EPChainableType returnType;
    private final ForgeFunction function;

    public ThreeFormNoParamFactory(EPChainableType returnType, ForgeFunction function) {
        this.returnType = returnType;
        this.function = function;
    }

    public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
        return new EnumForgeLambdaDesc(new EventType[0], new String[0]);
    }

    public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices statementCompileTimeService) {
        return new EnumForgeDesc(returnType, function.apply(streamCountIncoming));
    }

    @FunctionalInterface
    public interface ForgeFunction {
        EnumForge apply(int streamCountIncoming);
    }
}
