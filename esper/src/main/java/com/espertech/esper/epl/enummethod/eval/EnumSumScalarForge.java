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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class EnumSumScalarForge extends EnumForgeBase implements EnumForge, EnumEval {

    private final ExprDotEvalSumMethodFactory sumMethodFactory;

    public EnumSumScalarForge(int streamCountIncoming, ExprDotEvalSumMethodFactory sumMethodFactory) {
        super(streamCountIncoming);
        this.sumMethodFactory = sumMethodFactory;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        ExprDotEvalSumMethod method = sumMethodFactory.getSumAggregator();
        for (Object next : enumcoll) {
            method.enter(next);
        }
        return method.getValue();
    }

    public CodegenExpression codegen(CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(sumMethodFactory.getValueType(), EnumSumScalarForge.class).add(premade).begin();

        sumMethodFactory.codegenDeclare(block);

        CodegenBlock forEach = block.forEach(Object.class, "next", premade.enumcoll())
                    .ifRefNull("next").blockContinue();
        sumMethodFactory.codegenEnterObjectTypedNonNull(forEach, ref("next"));

        String method = sumMethodFactory.codegenReturn(block);
        return localMethodBuild(method).passAll(args).call();
    }
}
