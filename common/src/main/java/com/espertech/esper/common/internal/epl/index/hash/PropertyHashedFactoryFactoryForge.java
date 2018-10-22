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
package com.espertech.esper.common.internal.epl.index.hash;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForgeBase;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class PropertyHashedFactoryFactoryForge extends EventTableFactoryFactoryForgeBase {

    private final String[] indexedProps;
    private final EventType eventType;
    private final boolean unique;
    private final CoercionDesc hashCoercionDesc;

    public PropertyHashedFactoryFactoryForge(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, String[] indexedProps, EventType eventType, boolean unique, CoercionDesc hashCoercionDesc) {
        super(indexedStreamNum, subqueryNum, isFireAndForget);
        this.indexedProps = indexedProps;
        this.eventType = eventType;
        this.unique = unique;
        this.hashCoercionDesc = hashCoercionDesc;
    }

    protected Class typeOf() {
        return PropertyHashedFactoryFactory.class;
    }

    protected List<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        List<CodegenExpression> params = new ArrayList<>();
        params.add(constant(indexedProps));
        params.add(constant(hashCoercionDesc.getCoercionTypes()));
        params.add(constant(unique));
        Class[] propertyTypes = EventTypeUtility.getPropertyTypes(eventType, indexedProps);
        EventPropertyGetterSPI[] getters = EventTypeUtility.getGetters(eventType, indexedProps);
        CodegenExpression getter = EventTypeUtility.codegenGetterMayMultiKeyWCoerce(eventType, getters, propertyTypes, hashCoercionDesc.getCoercionTypes(), method, this.getClass(), classScope);
        params.add(getter);
        return params;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + indexedStreamNum +
                " propertyNames=" + Arrays.toString(indexedProps);
    }

    public Class getEventTableClass() {
        return unique ? PropertyHashedEventTableUnique.class : PropertyHashedEventTable.class;
    }
}
