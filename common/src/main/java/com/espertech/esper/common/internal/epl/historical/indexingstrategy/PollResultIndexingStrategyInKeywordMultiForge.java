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

import com.espertech.esper.common.client.EventPropertyValueGetter;
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

public class PollResultIndexingStrategyInKeywordMultiForge implements PollResultIndexingStrategyForge {
    private final int streamNum;
    private final EventType eventType;
    private final String[] propertyNames;

    public PollResultIndexingStrategyInKeywordMultiForge(int streamNum, EventType eventType, String[] propertyNames) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.propertyNames = propertyNames;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {

        CodegenMethod method = parent.makeChild(PollResultIndexingStrategyInKeywordMulti.class, this.getClass(), classScope);

        method.getBlock().declareVar(EventPropertyValueGetter[].class, "getters", newArrayByLength(EventPropertyValueGetter.class, constant(propertyNames.length)));
        for (int i = 0; i < propertyNames.length; i++) {
            EventPropertyGetterSPI getter = ((EventTypeSPI) eventType).getGetterSPI(propertyNames[i]);
            Class getterType = eventType.getPropertyType(propertyNames[i]);
            CodegenExpression eval = EventTypeUtility.codegenGetterWCoerce(getter, getterType, getterType, method, this.getClass(), classScope);
            method.getBlock().assignArrayElement(ref("getters"), constant(i), eval);
        }

        method.getBlock()
                .declareVar(PollResultIndexingStrategyInKeywordMulti.class, "strat", newInstance(PollResultIndexingStrategyInKeywordMulti.class))
                .exprDotMethod(ref("strat"), "setStreamNum", constant(streamNum))
                .exprDotMethod(ref("strat"), "setPropertyNames", constant(propertyNames))
                .exprDotMethod(ref("strat"), "setValueGetters", ref("getters"))
                .exprDotMethod(ref("strat"), "init")
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
