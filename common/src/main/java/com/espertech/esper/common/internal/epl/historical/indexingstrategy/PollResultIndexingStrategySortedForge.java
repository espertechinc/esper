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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PollResultIndexingStrategySortedForge implements PollResultIndexingStrategyForge {
    private final int streamNum;
    private final EventType eventType;
    private final String propertyName;
    private final Class valueType;

    public PollResultIndexingStrategySortedForge(int streamNum, EventType eventType, String propertyName, Class valueType) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.propertyName = propertyName;
        this.valueType = valueType;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {

        CodegenMethod method = parent.makeChild(PollResultIndexingStrategySorted.class, this.getClass(), classScope);

        EventPropertyGetterSPI propertyGetter = ((EventTypeSPI) eventType).getGetterSPI(propertyName);
        Class propertyType = eventType.getPropertyType(propertyName);
        CodegenExpression valueGetter = EventTypeUtility.codegenGetterWCoerce(propertyGetter, propertyType, valueType, method, this.getClass(), classScope);

        method.getBlock()
                .declareVar(PollResultIndexingStrategySorted.class, "strat", newInstance(PollResultIndexingStrategySorted.class))
                .exprDotMethod(ref("strat"), "setStreamNum", constant(streamNum))
                .exprDotMethod(ref("strat"), "setPropertyName", constant(propertyName))
                .exprDotMethod(ref("strat"), "setValueGetter", valueGetter)
                .exprDotMethod(ref("strat"), "setValueType", constant(valueType))
                .exprDotMethod(ref("strat"), "init")
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
