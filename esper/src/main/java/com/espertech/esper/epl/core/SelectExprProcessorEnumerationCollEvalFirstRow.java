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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.event.EventBeanUtility;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorEnumerationCollEvalFirstRow implements ExprEvaluator {
    private final SelectExprProcessorEnumerationCollForge forge;
    private final ExprEnumerationEval enumeration;

    public SelectExprProcessorEnumerationCollEvalFirstRow(SelectExprProcessorEnumerationCollForge forge, ExprEnumerationEval enumeration) {
        this.forge = forge;
        this.enumeration = enumeration;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Collection<EventBean> events = enumeration.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
        if (events == null || events.size() == 0) {
            return null;
        }
        return EventBeanUtility.getNonemptyFirstEvent(events);
    }

    public static CodegenExpression codegen(SelectExprProcessorEnumerationCollForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        String method = context.addMethod(EventBean.class, SelectExprProcessorEnumerationCollEval.class).add(params).begin()
                .declareVar(Collection.class, EventBean.class, "events", forge.enumerationForge.evaluateGetROCollectionEventsCodegen(params, context))
                .ifRefNullReturnNull("events")
                .ifCondition(equalsIdentity(exprDotMethod(ref("events"), "size"), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", ref("events")));
        return localMethodBuild(method).passAll(params).call();
    }
}
