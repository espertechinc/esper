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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EnumOrderByAscDescScalarForge extends EnumForgeBase implements EnumEval {

    private final boolean descending;

    public EnumOrderByAscDescScalarForge(int streamCountIncoming, boolean descending) {
        super(streamCountIncoming);
        this.descending = descending;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {

        if (enumcoll == null || enumcoll.isEmpty()) {
            return enumcoll;
        }

        List list = new ArrayList(enumcoll);
        if (descending) {
            Collections.sort(list, Collections.reverseOrder());
        } else {
            Collections.sort(list);
        }
        return list;
    }

    public CodegenExpression codegen(CodegenParamSetEnumMethodNonPremade args, CodegenContext context) {
        CodegenParamSetEnumMethodPremade premade = CodegenParamSetEnumMethodPremade.INSTANCE;
        CodegenBlock block = context.addMethod(Collection.class, EnumOrderByAscDescScalarForge.class).add(premade).begin()
                .ifCondition(or(equalsNull(premade.enumcoll()), exprDotMethod(premade.enumcoll(), "isEmpty")))
                .blockReturn(premade.enumcoll())
                .declareVar(List.class, "list", newInstance(ArrayList.class, premade.enumcoll()));
        if (descending) {
            block.expression(staticMethod(Collections.class, "sort", ref("list"), staticMethod(Collections.class, "reverseOrder")));
        } else {
            block.expression(staticMethod(Collections.class, "sort", ref("list")));
        }
        String method = block.methodReturn(ref("list"));
        return localMethodBuild(method).passAll(args).call();

    }
}
