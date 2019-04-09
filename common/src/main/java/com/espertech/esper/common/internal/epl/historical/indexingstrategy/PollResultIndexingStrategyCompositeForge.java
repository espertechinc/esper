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
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PollResultIndexingStrategyCompositeForge implements PollResultIndexingStrategyForge {
    private final int streamNum;
    private final EventType eventType;
    private final String[] optHashPropertyNames;
    private final Class[] optHashCoercedTypes;
    private final MultiKeyClassRef optHashMultiKeyClasses;
    private final String[] rangeProps;
    private final Class[] rangeTypes;

    public PollResultIndexingStrategyCompositeForge(int streamNum, EventType eventType, String[] optHashPropertyNames, Class[] optHashCoercedTypes, MultiKeyClassRef optHashMultiKeyClasses, String[] rangeProps, Class[] rangeTypes) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.optHashPropertyNames = optHashPropertyNames;
        this.optHashCoercedTypes = optHashCoercedTypes;
        this.optHashMultiKeyClasses = optHashMultiKeyClasses;
        this.rangeProps = rangeProps;
        this.rangeTypes = rangeTypes;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {

        CodegenMethod method = parent.makeChild(PollResultIndexingStrategyComposite.class, this.getClass(), classScope);

        CodegenExpression hashGetter = constantNull();
        if (optHashPropertyNames != null) {
            EventPropertyGetterSPI[] propertyGetters = EventTypeUtility.getGetters(eventType, optHashPropertyNames);
            Class[] propertyTypes = EventTypeUtility.getPropertyTypes(eventType, optHashPropertyNames);
            hashGetter = MultiKeyCodegen.codegenGetterMayMultiKey(eventType, propertyGetters, propertyTypes, optHashCoercedTypes, optHashMultiKeyClasses, method, classScope);
        }

        method.getBlock().declareVar(EventPropertyValueGetter[].class, "rangeGetters", newArrayByLength(EventPropertyValueGetter.class, constant(rangeProps.length)));
        for (int i = 0; i < rangeProps.length; i++) {
            Class propertyType = eventType.getPropertyType(rangeProps[i]);
            EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(rangeProps[i]);
            CodegenExpression getter = EventTypeUtility.codegenGetterWCoerce(getterSPI, propertyType, rangeTypes[i], method, this.getClass(), classScope);
            method.getBlock().assignArrayElement(ref("rangeGetters"), constant(i), getter);
        }

        method.getBlock()
            .declareVar(PollResultIndexingStrategyComposite.class, "strat", newInstance(PollResultIndexingStrategyComposite.class))
            .exprDotMethod(ref("strat"), "setStreamNum", constant(streamNum))
            .exprDotMethod(ref("strat"), "setOptionalKeyedProps", constant(optHashPropertyNames))
            .exprDotMethod(ref("strat"), "setOptKeyCoercedTypes", constant(optHashCoercedTypes))
            .exprDotMethod(ref("strat"), "setHashGetter", hashGetter)
            .exprDotMethod(ref("strat"), "setRangeProps", constant(rangeProps))
            .exprDotMethod(ref("strat"), "setOptRangeCoercedTypes", constant(rangeTypes))
            .exprDotMethod(ref("strat"), "setRangeGetters", ref("rangeGetters"))
            .exprDotMethod(ref("strat"), "init")
            .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
