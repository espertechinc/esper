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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class SelectEvalStreamNoUndWEventBeanToObj extends SelectEvalStreamBaseMap {

    private final String[] eventBeanToObjectProps;

    public SelectEvalStreamNoUndWEventBeanToObj(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard, Set<String> eventBeanToObjectProps) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
        this.eventBeanToObjectProps = eventBeanToObjectProps.toArray(new String[eventBeanToObjectProps.size()]);
    }

    protected CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpression props, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return staticMethod(SelectEvalStreamNoUndWEventBeanToObj.class, "processSelectExprbeanToMap", props, constant(eventBeanToObjectProps), eventBeanFactory, resultEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param props                  props
     * @param eventBeanToObjectProps indexes
     * @param eventAdapterService    svc
     * @param resultEventType        type
     * @return bean
     */
    public static EventBean processSelectExprbeanToMap(Map<String, Object> props, String[] eventBeanToObjectProps, EventBeanTypedEventFactory eventAdapterService, EventType resultEventType) {
        for (String property : eventBeanToObjectProps) {
            Object value = props.get(property);
            if (value instanceof EventBean) {
                props.put(property, ((EventBean) value).getUnderlying());
            }
        }
        return eventAdapterService.adapterForTypedMap(props, resultEventType);
    }
}
