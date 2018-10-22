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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.util.IntArrayUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class SelectEvalStreamNoUndWEventBeanToObjObjArray extends SelectEvalStreamBaseObjectArray {

    private final int[] eventBeanToObjectIndexesArray;

    public SelectEvalStreamNoUndWEventBeanToObjObjArray(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard, Set<String> eventBeanToObjectProps) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
        HashSet<Integer> eventBeanToObjectIndexes = new HashSet<>();
        ObjectArrayEventType type = (ObjectArrayEventType) resultEventType;
        for (String name : eventBeanToObjectProps) {
            Integer index = type.getPropertiesIndexes().get(name);
            if (index != null) {
                eventBeanToObjectIndexes.add(index);
            }
        }
        eventBeanToObjectIndexesArray = IntArrayUtil.toArray(eventBeanToObjectIndexes);
    }

    protected CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpressionRef props, CodegenClassScope codegenClassScope) {
        return staticMethod(SelectEvalStreamNoUndWEventBeanToObjObjArray.class, "processSelectExprbeanToObjArray", props, constant(eventBeanToObjectIndexesArray), eventBeanFactory, resultEventType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param props                      props
     * @param eventBeanToObjectIndexes   indexes
     * @param eventBeanTypedEventFactory svc
     * @param resultEventType            type
     * @return bean
     */
    public static EventBean processSelectExprbeanToObjArray(Object[] props, int[] eventBeanToObjectIndexes, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventType resultEventType) {
        for (Integer propertyIndex : eventBeanToObjectIndexes) {
            Object value = props[propertyIndex];
            if (value instanceof EventBean) {
                props[propertyIndex] = ((EventBean) value).getUnderlying();
            }
        }
        return eventBeanTypedEventFactory.adapterForTypedObjectArray(props, resultEventType);
    }
}
