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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodNonPremade;
import com.espertech.esper.codegen.model.method.CodegenParamSetEnumMethodPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumFirstOfNoPredicateForge extends EnumForgeBase implements EnumForge, EnumEval {

    private final EPType resultType;

    public EnumFirstOfNoPredicateForge(int streamCountIncoming, EPType resultType) {
        super(streamCountIncoming);
        this.resultType = resultType;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll == null || enumcoll.isEmpty()) {
            return null;
        }
        return enumcoll.iterator().next();
    }

    public CodegenExpression codegen(CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        Class type = EPTypeHelper.getCodegenReturnType(resultType);
        String method = context.addMethod(type, EnumFirstOfNoPredicateForge.class).add(premade).begin()
                .ifCondition(or(equalsNull(premade.enumcoll()), exprDotMethod(premade.enumcoll(), "isEmpty")))
                .blockReturn(constantNull())
                .methodReturn(cast(type, exprDotMethodChain(premade.enumcoll()).add("iterator").add("next")));
        return localMethodBuild(method).passAll(args).call();
    }
}
