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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorEnumerationSingleToCollEval implements ExprEvaluator {
    private final SelectExprProcessorEnumerationSingleToCollForge forge;
    private final ExprEnumerationEval enumeration;

    public SelectExprProcessorEnumerationSingleToCollEval(SelectExprProcessorEnumerationSingleToCollForge forge, ExprEnumerationEval enumeration) {
        this.forge = forge;
        this.enumeration = enumeration;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = enumeration.evaluateGetEventBean(eventsPerStream, isNewData, exprEvaluatorContext);
        if (event == null) {
            return null;
        }
        return new EventBean[]{event};
    }

    public static CodegenExpression codegen(SelectExprProcessorEnumerationSingleToCollForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean[].class, SelectExprProcessorEnumerationSingleToCollEval.class, codegenClassScope);

        methodNode.getBlock()
                .declareVar(EventBean.class, "event", forge.enumerationForge.evaluateGetEventBeanCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("event")
                .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(1)))
                .assignArrayElement(ref("events"), constant(0), ref("event"))
                .methodReturn(ref("events"));
        return localMethod(methodNode);
    }

}
