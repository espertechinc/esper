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
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PollResultIndexingStrategyHashForge implements PollResultIndexingStrategyForge {
    private final int streamNum;
    private final EventType eventType;
    private final String[] propertyNames;
    private final Class[] coercionTypes;
    private final MultiKeyClassRef multiKeyClasses;

    public PollResultIndexingStrategyHashForge(int streamNum, EventType eventType, String[] propertyNames, Class[] coercionTypes, MultiKeyClassRef multiKeyClasses) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.propertyNames = propertyNames;
        this.coercionTypes = coercionTypes;
        this.multiKeyClasses = multiKeyClasses;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {

        CodegenMethod method = parent.makeChild(PollResultIndexingStrategyHash.class, this.getClass(), classScope);

        EventPropertyGetterSPI[] propertyGetters = EventTypeUtility.getGetters(eventType, propertyNames);
        Class[] propertyTypes = EventTypeUtility.getPropertyTypes(eventType, propertyNames);
        CodegenExpression valueGetter = MultiKeyCodegen.codegenGetterMayMultiKey(eventType, propertyGetters, propertyTypes, coercionTypes, multiKeyClasses, method, classScope);

        method.getBlock()
            .declareVar(PollResultIndexingStrategyHash.class, "strat", newInstance(PollResultIndexingStrategyHash.class))
            .exprDotMethod(ref("strat"), "setStreamNum", constant(streamNum))
            .exprDotMethod(ref("strat"), "setPropertyNames", constant(propertyNames))
            .exprDotMethod(ref("strat"), "setValueGetter", valueGetter)
            .exprDotMethod(ref("strat"), "init")
            .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
