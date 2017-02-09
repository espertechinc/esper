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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvalSelectStreamNoUndWEventBeanToObjObjArray extends EvalSelectStreamBaseObjectArray implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalSelectStreamNoUndWEventBeanToObjObjArray.class);

    private final Set<Integer> eventBeanToObjectIndexes;

    public EvalSelectStreamNoUndWEventBeanToObjObjArray(SelectExprContext selectExprContext, EventType resultEventType, List<SelectClauseStreamCompiledSpec> namedStreams, boolean usingWildcard, Set<String> eventBeanToObjectProps) {
        super(selectExprContext, resultEventType, namedStreams, usingWildcard);
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
        for (Integer propertyIndex : eventBeanToObjectIndexes) {
            Object value = props[propertyIndex];
            if (value instanceof EventBean) {
                props[propertyIndex] = ((EventBean) value).getUnderlying();
            }
        }
        return super.getSelectExprContext().getEventAdapterService().adapterForTypedObjectArray(props, super.getResultEventType());
    }
}
