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
package com.espertech.esper.common.internal.epl.index.composite;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PropertyCompositeEventTableFactoryFactoryForge implements EventTableFactoryFactoryForge {
    private final int indexedStreamNum;
    private final Integer subqueryNum;
    private final boolean isFireAndForget;
    private final String[] optKeyProps;
    private final Class[] optKeyTypes;
    private final MultiKeyClassRef hashMultikeyClasses;
    private final String[] rangeProps;
    private final Class[] rangeTypes;
    private final DataInputOutputSerdeForge[] rangeSerdes;
    private final EventType eventType;

    public PropertyCompositeEventTableFactoryFactoryForge(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget, String[] optKeyProps, Class[] optKeyTypes, MultiKeyClassRef hashMultikeyClasses, String[] rangeProps, Class[] rangeTypes, DataInputOutputSerdeForge[] rangeSerdes, EventType eventType) {
        this.indexedStreamNum = indexedStreamNum;
        this.subqueryNum = subqueryNum;
        this.isFireAndForget = isFireAndForget;
        this.optKeyProps = optKeyProps;
        this.optKeyTypes = optKeyTypes;
        this.hashMultikeyClasses = hashMultikeyClasses;
        this.rangeProps = rangeProps;
        this.rangeTypes = rangeTypes;
        this.rangeSerdes = rangeSerdes;
        this.eventType = eventType;
    }

    public String toQueryPlan() {
        return this.getClass().getName() +
            " streamNum=" + indexedStreamNum +
            " keys=" + optKeyProps == null ? "none" : Arrays.toString(optKeyProps) +
            " ranges=" + Arrays.toString(rangeProps);
    }

    public Class getEventTableClass() {
        return PropertyCompositeEventTable.class;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PropertyCompositeEventTableFactoryFactory.class, this.getClass(), classScope);

        CodegenExpression hashGetter = constantNull();
        if (optKeyProps != null && optKeyProps.length > 0) {
            Class[] propertyTypes = EventTypeUtility.getPropertyTypes(eventType, optKeyProps);
            EventPropertyGetterSPI[] getters = EventTypeUtility.getGetters(eventType, optKeyProps);
            hashGetter = MultiKeyCodegen.codegenGetterMayMultiKey(eventType, getters, propertyTypes, optKeyTypes, hashMultikeyClasses, method, classScope);
        }

        method.getBlock().declareVar(EventPropertyValueGetter[].class, "rangeGetters", newArrayByLength(EventPropertyValueGetter.class, constant(rangeProps.length)));
        for (int i = 0; i < rangeProps.length; i++) {
            Class propertyType = eventType.getPropertyType(rangeProps[i]);
            EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(rangeProps[i]);
            CodegenExpression getter = EventTypeUtility.codegenGetterWCoerce(getterSPI, propertyType, rangeTypes[i], method, this.getClass(), classScope);
            method.getBlock().assignArrayElement(ref("rangeGetters"), constant(i), getter);
        }

        List<CodegenExpression> params = new ArrayList<>();
        params.add(constant(indexedStreamNum));
        params.add(constant(subqueryNum));
        params.add(constant(isFireAndForget));
        params.add(constant(optKeyProps));
        params.add(constant(optKeyTypes));
        params.add(hashGetter);
        params.add(hashMultikeyClasses.getExprMKSerde(method, classScope));
        params.add(constant(rangeProps));
        params.add(constant(rangeTypes));
        params.add(ref("rangeGetters"));
        params.add(DataInputOutputSerdeForge.codegenArray(rangeSerdes, method, classScope, null));

        method.getBlock().methodReturn(newInstance(PropertyCompositeEventTableFactoryFactory.class, params.toArray(new CodegenExpression[0])));
        return localMethod(method);
    }
}
