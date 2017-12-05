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
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.event.EventAdapterService;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class EvalSelectStreamNoUndWEventBeanToObj extends EvalSelectStreamBaseMap implements SelectExprProcessor {

    private final Set<String> eventBeanToObjectProps;

    public EvalSelectStreamNoUndWEventBeanToObj(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard, Set<String> eventBeanToObjectProps) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
        this.eventBeanToObjectProps = eventBeanToObjectProps;
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return processSelectExprbeanToMap(props, eventBeanToObjectProps, super.getContext().getEventAdapterService(), super.resultEventType);
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember indexes = codegenClassScope.makeAddMember(Set.class, eventBeanToObjectProps);
        return staticMethod(EvalSelectStreamNoUndWEventBeanToObj.class, "processSelectExprbeanToMap", props, CodegenExpressionBuilder.member(indexes.getMemberId()), CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), CodegenExpressionBuilder.member(memberResultEventType.getMemberId()));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param props props
     * @param eventBeanToObjectProps indexes
     * @param eventAdapterService svc
     * @param resultEventType type
     * @return bean
     */
    public static EventBean processSelectExprbeanToMap(Map<String, Object> props, Set<String> eventBeanToObjectProps, EventAdapterService eventAdapterService, EventType resultEventType) {
        for (String property : eventBeanToObjectProps) {
            Object value = props.get(property);
            if (value instanceof EventBean) {
                props.put(property, ((EventBean) value).getUnderlying());
            }
        }
        return eventAdapterService.adapterForTypedMap(props, resultEventType);
    }
}
