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

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorEnumerationAtBeanCollEval implements ExprEvaluator {
    private final SelectExprProcessorEnumerationAtBeanCollForge forge;
    private final ExprEnumerationEval enumEval;

    public SelectExprProcessorEnumerationAtBeanCollEval(SelectExprProcessorEnumerationAtBeanCollForge forge, ExprEnumerationEval enumEval) {
        this.forge = forge;
        this.enumEval = enumEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // the protocol is EventBean[]
        Object result = enumEval.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
        if (result != null && result instanceof Collection) {
            Collection<EventBean> events = (Collection<EventBean>) result;
            return events.toArray(new EventBean[events.size()]);
        }
        return result;
    }

    public static CodegenExpression codegen(SelectExprProcessorEnumerationAtBeanCollForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean[].class, SelectExprProcessorEnumerationAtBeanCollEval.class, codegenClassScope);
        methodNode.getBlock()
                .declareVar(Object.class, "result", forge.enumerationForge.evaluateGetROCollectionEventsCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifCondition(and(notEqualsNull(ref("result")), instanceOf(ref("result"), Collection.class)))
                .declareVar(Collection.class, EventBean.class, "events", cast(Collection.class, ref("result")))
                .blockReturn(cast(EventBean[].class, exprDotMethod(ref("events"), "toArray", newArrayByLength(EventBean.class, exprDotMethod(ref("events"), "size")))))
                .methodReturn(cast(EventBean[].class, ref("result")));
        return localMethod(methodNode);
    }

}
