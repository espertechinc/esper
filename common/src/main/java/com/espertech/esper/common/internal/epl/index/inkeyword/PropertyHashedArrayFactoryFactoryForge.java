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
package com.espertech.esper.common.internal.epl.index.inkeyword;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PropertyHashedArrayFactoryFactoryForge implements EventTableFactoryFactoryForge {
    protected final int streamNum;
    protected final EventType eventType;
    protected final String[] propertyNames;
    protected final Class[] propertyTypes;
    protected final DataInputOutputSerdeForge[] serdes;
    protected final boolean unique;
    protected final boolean isFireAndForget;

    public PropertyHashedArrayFactoryFactoryForge(int streamNum, EventType eventType, String[] propertyNames, Class[] propertyTypes, DataInputOutputSerdeForge[] serdes, boolean unique, boolean isFireAndForget) {
        this.streamNum = streamNum;
        this.eventType = eventType;
        this.propertyNames = propertyNames;
        this.propertyTypes = propertyTypes;
        this.serdes = serdes;
        this.unique = unique;
        this.isFireAndForget = isFireAndForget;
    }

    public Class getEventTableClass() {
        return PropertyHashedEventTable.class;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PropertyHashedArrayFactoryFactory.class, this.getClass(), classScope);

        method.getBlock().declareVar(EventPropertyValueGetter[].class, "getters", newArrayByLength(EventPropertyValueGetter.class, constant(propertyNames.length)));
        for (int i = 0; i < propertyNames.length; i++) {
            EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(propertyNames[i]);
            CodegenExpression getter = EventTypeUtility.codegenGetterWCoerce(getterSPI, propertyTypes[i], propertyTypes[i], method, this.getClass(), classScope);
            method.getBlock().assignArrayElement(ref("getters"), constant(i), getter);
        }

        method.getBlock().methodReturn(newInstance(PropertyHashedArrayFactoryFactory.class,
                constant(streamNum), constant(propertyNames), constant(propertyTypes), DataInputOutputSerdeForge.codegenArray(serdes, method, classScope, null), constant(unique), ref("getters"),
                constant(isFireAndForget)));
        return localMethod(method);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                (unique ? " unique" : " non-unique") +
                " streamNum=" + streamNum +
                " propertyNames=" + Arrays.toString(propertyNames);
    }
}
