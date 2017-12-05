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
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class EvalSelectStreamNoUndWEventBeanToObjObjArray extends EvalSelectStreamBaseObjectArray implements SelectExprProcessor {

    private final Set<Integer> eventBeanToObjectIndexes;

    public EvalSelectStreamNoUndWEventBeanToObjObjArray(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard, Set<String> eventBeanToObjectProps) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
        this.eventBeanToObjectIndexes = new HashSet<Integer>();
        ObjectArrayEventType type = (ObjectArrayEventType) resultEventType;
        for (String name : eventBeanToObjectProps) {
            Integer index = type.getPropertiesIndexes().get(name);
            if (index != null) {
                eventBeanToObjectIndexes.add(index);
            }
        }
    }

    public EventBean processSpecific(Object[] props, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        return processSelectExprbeanToObjArray(props, eventBeanToObjectIndexes, context.getEventAdapterService(), super.getResultEventType());
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpressionRef props, CodegenClassScope codegenClassScope) {
        CodegenMember indexes = codegenClassScope.makeAddMember(Set.class, eventBeanToObjectIndexes);
        return staticMethod(EvalSelectStreamNoUndWEventBeanToObjObjArray.class, "processSelectExprbeanToObjArray", props, CodegenExpressionBuilder.member(indexes.getMemberId()), CodegenExpressionBuilder.member(memberEventAdapterService.getMemberId()), CodegenExpressionBuilder.member(memberResultEventType.getMemberId()));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param props props
     * @param eventBeanToObjectIndexes indexes
     * @param eventAdapterService svc
     * @param resultEventType type
     * @return bean
     */
    public static EventBean processSelectExprbeanToObjArray(Object[] props, Set<Integer> eventBeanToObjectIndexes, EventAdapterService eventAdapterService, EventType resultEventType) {
        for (Integer propertyIndex : eventBeanToObjectIndexes) {
            Object value = props[propertyIndex];
            if (value instanceof EventBean) {
                props[propertyIndex] = ((EventBean) value).getUnderlying();
            }
        }
        return eventAdapterService.adapterForTypedObjectArray(props, resultEventType);
    }
}
