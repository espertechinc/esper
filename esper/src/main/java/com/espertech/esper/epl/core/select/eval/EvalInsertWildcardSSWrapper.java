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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.DecoratingEventBean;
import com.espertech.esper.event.EventAdapterService;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalInsertWildcardSSWrapper extends EvalBaseMap implements SelectExprProcessor {

    public EvalInsertWildcardSSWrapper(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        super(selectExprForgeContext, resultEventType);
    }

    protected void initSelectExprProcessorSpecific(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
    }

    // In case of a wildcard and single stream that is itself a
    // wrapper bean, we also need to add the map properties
    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        return processSelectExprSSWrapper(props, eventsPerStream, evaluators.length == 0, super.getEventAdapterService(), super.resultEventType);
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenMethodNode methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        return staticMethod(EvalInsertWildcardSSWrapper.class, "processSelectExprSSWrapper", props, refEPS, constant(context.getExprForges().length == 0), member(memberEventAdapterService.getMemberId()), member(memberResultEventType.getMemberId()));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param props props
     * @param eventsPerStream events
     * @param emptyExpressions flag
     * @param eventAdapterService svc
     * @param resultEventType type
     * @return bean
     */
    public static EventBean processSelectExprSSWrapper(Map<String, Object> props, EventBean[] eventsPerStream, boolean emptyExpressions, EventAdapterService eventAdapterService, EventType resultEventType) {
        EventBean theEvent = eventsPerStream[0];
        DecoratingEventBean wrapper = (DecoratingEventBean) theEvent;
        if (wrapper != null) {
            Map<String, Object> map = wrapper.getDecoratingProperties();
            if (emptyExpressions && !map.isEmpty()) {
                props = new HashMap<String, Object>(map);
            } else {
                props.putAll(map);
            }
        }

        // Using a wrapper bean since we cannot use the same event type else same-type filters match.
        // Wrapping it even when not adding properties is very inexpensive.
        return eventAdapterService.adapterForTypedWrapper(theEvent, props, resultEventType);
    }
}
