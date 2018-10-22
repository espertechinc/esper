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
package com.espertech.esper.common.internal.epl.index.sorted;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForgeBase;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class PropertySortedFactoryFactoryForge extends EventTableFactoryFactoryForgeBase {

    private final String indexedProp;
    private final EventType eventType;
    private final CoercionDesc coercionDesc;

    public PropertySortedFactoryFactoryForge(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, String indexedProp, EventType eventType, CoercionDesc coercionDesc) {
        super(indexedStreamNum, subqueryNum, isFireAndForget);
        this.indexedProp = indexedProp;
        this.eventType = eventType;
        this.coercionDesc = coercionDesc;
    }

    protected Class typeOf() {
        return PropertySortedFactoryFactory.class;
    }

    protected List<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        List<CodegenExpression> params = new ArrayList<>();
        params.add(constant(indexedProp));
        params.add(constant(coercionDesc.getCoercionTypes()[0]));
        Class propertyType = eventType.getPropertyType(indexedProp);
        EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(indexedProp);
        CodegenExpression getter = EventTypeUtility.codegenGetterWCoerce(getterSPI, propertyType, coercionDesc.getCoercionTypes()[0], method, this.getClass(), classScope);
        params.add(getter);
        return params;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + indexedStreamNum +
                " propertyName=" + indexedProp;
    }

    public Class getEventTableClass() {
        return PropertySortedEventTable.class;
    }
}
