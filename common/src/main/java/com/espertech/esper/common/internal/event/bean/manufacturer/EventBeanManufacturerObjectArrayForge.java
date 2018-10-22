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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.*;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for ObjectArray-underlying events.
 */
public class EventBeanManufacturerObjectArrayForge implements EventBeanManufacturerForge {
    private final ObjectArrayEventType eventType;
    private final int[] indexPerWritable;
    private final boolean oneToOne;

    /**
     * Ctor.
     *
     * @param eventType  type to create
     * @param properties written properties
     */
    public EventBeanManufacturerObjectArrayForge(ObjectArrayEventType eventType, WriteablePropertyDescriptor[] properties) {
        this.eventType = eventType;

        Map<String, Integer> indexes = eventType.getPropertiesIndexes();
        indexPerWritable = new int[properties.length];
        boolean oneToOneMapping = true;
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();
            Integer index = indexes.get(propertyName);
            if (index == null) {
                throw new IllegalStateException("Failed to find property '" + propertyName + "' among the array indexes");
            }
            indexPerWritable[i] = index;
            if (index != i) {
                oneToOneMapping = false;
            }
        }
        oneToOne = oneToOneMapping && properties.length == eventType.getPropertyNames().length;
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new EventBeanManufacturerObjectArray(eventType, eventBeanTypedEventFactory, indexPerWritable, oneToOne);
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod init = codegenClassScope.getPackageScope().getInitMethod();

        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(this.eventType, EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass manufacturer = newAnonymousClass(init.getBlock(), EventBeanManufacturer.class);

        CodegenMethod makeUndMethod = CodegenMethod.makeParentNode(Object[].class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("makeUnderlying", makeUndMethod);
        makeUnderlyingCodegen(makeUndMethod, codegenClassScope);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(EventBean.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("make", makeMethod);
        makeMethod.getBlock()
                .declareVar(Object[].class, "und", localMethod(makeUndMethod, ref("properties")))
                .methodReturn(exprDotMethod(factory, "adapterForTypedObjectArray", ref("und"), eventType));

        return codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, manufacturer);
    }

    private void makeUnderlyingCodegen(CodegenMethod method, CodegenClassScope codegenClassScope) {
        if (oneToOne) {
            method.getBlock().methodReturn(ref("properties"));
            return;
        }

        method.getBlock().declareVar(Object[].class, "cols", newArrayByLength(Object.class, constant(eventType.getPropertyNames().length)));
        for (int i = 0; i < indexPerWritable.length; i++) {
            method.getBlock().assignArrayElement(ref("cols"), constant(indexPerWritable[i]), arrayAtIndex(ref("properties"), constant(i)));
        }
        method.getBlock().methodReturn(ref("cols"));
    }
}
