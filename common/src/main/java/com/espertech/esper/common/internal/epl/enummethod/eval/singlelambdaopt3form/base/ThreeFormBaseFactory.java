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

import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.rettype.EPChainableType;

import java.util.List;

public abstract class ThreeFormBaseFactory implements EnumForgeDescFactory {
    protected abstract EnumForge makeForgeWithParam(ExprDotEvalParamLambda lambda, EPChainableType typeInfo, StatementCompileTimeServices services);

    private final ThreeFormInitFunction returnType;

    public ThreeFormBaseFactory(ThreeFormInitFunction returnType) {
        this.returnType = returnType;
    }

    public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) throws ExprValidationException {
        if (bodiesAndParameters.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        EPChainableType typeInfo = returnType.apply(first);
        EnumForge forge = makeForgeWithParam(first, typeInfo, services);
        return new EnumForgeDesc(typeInfo, forge);
    }
}
