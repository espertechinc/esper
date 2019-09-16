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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.event.core.DecoratingEventBean;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class SelectEvalInsertWildcardSSWrapper extends SelectEvalBaseMap {

    public SelectEvalInsertWildcardSSWrapper(SelectExprForgeContext selectExprForgeContext, EventType resultEventType) {
        super(selectExprForgeContext, resultEventType);
    }

    protected CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpression props, CodegenMethod methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        return staticMethod(SelectEvalInsertWildcardSSWrapper.class, "processSelectExprSSWrapper", props, refEPS, constant(context.getExprForges().length == 0), eventBeanFactory, resultEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param props                      props
     * @param eventsPerStream            events
     * @param emptyExpressions           flag
     * @param eventBeanTypedEventFactory svc
     * @param resultEventType            type
     * @return bean
     */
    public static EventBean processSelectExprSSWrapper(Map<String, Object> props, EventBean[] eventsPerStream, boolean emptyExpressions, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType resultEventType) {
        EventBean theEvent = eventsPerStream[0];
        DecoratingEventBean wrapper = (DecoratingEventBean) theEvent;
        if (wrapper != null) {
            Map<String, Object> map = wrapper.getDecoratingProperties();
            if (emptyExpressions && !map.isEmpty()) {
                props = new HashMap<>(map);
            } else {
                props.putAll(map);
            }
        }
        return eventBeanTypedEventFactory.adapterForTypedWrapper(theEvent, props, resultEventType);
    }
}
