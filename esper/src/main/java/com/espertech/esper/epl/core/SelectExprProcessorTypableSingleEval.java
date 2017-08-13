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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventBeanManufacturer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.SelectExprProcessorHelper.applyWideners;
import static com.espertech.esper.epl.core.SelectExprProcessorHelper.applyWidenersCodegen;

public class SelectExprProcessorTypableSingleEval implements ExprEvaluator {
    private final SelectExprProcessorTypableSingleForge forge;
    private final ExprTypableReturnEval typable;

    public SelectExprProcessorTypableSingleEval(SelectExprProcessorTypableSingleForge forge, ExprTypableReturnEval typable) {
        this.forge = forge;
        this.typable = typable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] row = typable.evaluateTypableSingle(eventsPerStream, isNewData, exprEvaluatorContext);
        if (row == null) {
            return null;
        }
        if (forge.hasWideners) {
            applyWideners(row, forge.wideners);
        }
        return new EventBean[]{forge.factory.make(row)};
    }

    public static CodegenExpression codegen(SelectExprProcessorTypableSingleForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember factory = context.makeAddMember(EventBeanManufacturer.class, forge.factory);
        CodegenBlock block = context.addMethod(EventBean[].class, SelectExprProcessorTypableSingleEval.class).add(params).begin()
                .declareVar(Object[].class, "row", forge.typable.evaluateTypableSingleCodegen(params, context))
                .ifRefNullReturnNull("row");
        if (forge.hasWideners) {
            block.expression(applyWidenersCodegen(ref("row"), forge.wideners, context));
        }
        CodegenMethodId method = block.declareVar(EventBean[].class, "events", newArray(EventBean.class, constant(1)))
                .assignArrayElement("events", constant(0), exprDotMethod(member(factory.getMemberId()), "make", ref("row")))
                .methodReturn(ref("events"));
        return localMethodBuild(method).passAll(params).call();
    }

}
