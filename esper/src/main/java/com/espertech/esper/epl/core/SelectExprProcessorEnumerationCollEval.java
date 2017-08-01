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

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorEnumerationCollEval implements ExprEvaluator {
    private final SelectExprProcessorEnumerationCollForge forge;
    private final ExprEnumerationEval enumeration;

    public SelectExprProcessorEnumerationCollEval(SelectExprProcessorEnumerationCollForge forge, ExprEnumerationEval enumeration) {
        this.forge = forge;
        this.enumeration = enumeration;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Collection<EventBean> events = enumeration.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
        if (events == null) {
            return null;
        }
        return events.toArray(new EventBean[events.size()]);
    }

    public static CodegenExpression codegen(SelectExprProcessorEnumerationCollForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        String method = context.addMethod(EventBean[].class, SelectExprProcessorEnumerationCollEval.class).add(params).begin()
                .declareVar(Collection.class, EventBean.class, "events", forge.enumerationForge.evaluateGetROCollectionEventsCodegen(params, context))
                .ifRefNullReturnNull("events")
                .methodReturn(cast(EventBean[].class, exprDotMethod(ref("events"), "toArray", newArray(EventBean.class, exprDotMethod(ref("events"), "size")))));
        return localMethodBuild(method).passAll(params).call();
    }

}
