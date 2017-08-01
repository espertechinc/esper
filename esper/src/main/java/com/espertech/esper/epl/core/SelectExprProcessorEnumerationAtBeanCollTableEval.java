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
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEnumerationEval;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadataInternalEventToPublic;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorEnumerationAtBeanCollTableEval implements ExprEvaluator {
    private final SelectExprProcessorEnumerationAtBeanCollTableForge forge;
    private final ExprEnumerationEval enumEval;

    public SelectExprProcessorEnumerationAtBeanCollTableEval(SelectExprProcessorEnumerationAtBeanCollTableForge forge, ExprEnumerationEval enumEval) {
        this.forge = forge;
        this.enumEval = enumEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // the protocol is EventBean[]
        Object result = enumEval.evaluateGetROCollectionEvents(eventsPerStream, isNewData, exprEvaluatorContext);
        if (result == null) {
            return null;
        }
        return convertToTableType(result, forge.tableMetadata.getEventToPublic(), eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(SelectExprProcessorEnumerationAtBeanCollTableForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember eventToPublic = context.makeAddMember(TableMetadataInternalEventToPublic.class, forge.tableMetadata.getEventToPublic());
        String method = context.addMethod(EventBean[].class, SelectExprProcessorEnumerationAtBeanCollTableEval.class).add(params).begin()
                .declareVar(Object.class, "result", forge.enumerationForge.evaluateGetROCollectionEventsCodegen(params, context))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(SelectExprProcessorEnumerationAtBeanCollTableEval.class, "convertToTableType", ref("result"), ref(eventToPublic.getMemberName()), params.passEPS(), params.passIsNewData(), params.passEvalCtx()));
        return localMethodBuild(method).passAll(params).call();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param result result
     * @param eventToPublic conversion
     * @param eventsPerStream events
     * @param isNewData
     * @param exprEvaluatorContext
     * @return
     */
    public static EventBean[] convertToTableType(Object result, TableMetadataInternalEventToPublic eventToPublic, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (result instanceof Collection) {
            Collection<EventBean> events = (Collection<EventBean>) result;
            EventBean[] out = new EventBean[events.size()];
            int index = 0;
            for (EventBean event : events) {
                out[index++] = eventToPublic.convert(event, eventsPerStream, isNewData, exprEvaluatorContext);
            }
            return out;
        }
        EventBean[] events = (EventBean[]) result;
        for (int i = 0; i < events.length; i++) {
            events[i] = eventToPublic.convert(events[i], eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return events;
    }
}
