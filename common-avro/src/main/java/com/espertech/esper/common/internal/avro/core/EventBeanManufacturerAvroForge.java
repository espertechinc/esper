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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.core.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for Avro-underlying events.
 */
public class EventBeanManufacturerAvroForge implements EventBeanManufacturerForge {
    private final AvroEventType eventType;
    private final int[] indexPerWritable;

    public EventBeanManufacturerAvroForge(AvroSchemaEventType eventType, WriteablePropertyDescriptor[] properties) {
        this.eventType = (AvroEventType) eventType;

        Schema schema = this.eventType.getSchemaAvro();
        indexPerWritable = new int[properties.length];
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();

            Schema.Field field = schema.getField(propertyName);
            if (field == null) {
                throw new IllegalStateException("Failed to find property '" + propertyName + "' among the array indexes");
            }
            indexPerWritable[i] = field.pos();
        }
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new EventBeanManufacturerAvro(eventType, eventBeanTypedEventFactory, indexPerWritable);
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod init = codegenClassScope.getPackageScope().getInitMethod();

        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField beanType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass manufacturer = newAnonymousClass(init.getBlock(), EventBeanManufacturer.class);

        CodegenMethod makeUndMethod = CodegenMethod.makeParentNode(GenericData.Record.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("makeUnderlying", makeUndMethod);
        makeUnderlyingCodegen(makeUndMethod, codegenClassScope);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(EventBean.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("make", makeMethod);
        makeMethod.getBlock()
                .declareVar(GenericData.Record.class, "und", localMethod(makeUndMethod, ref("properties")))
                .methodReturn(exprDotMethod(factory, "adapterForTypedAvro", ref("und"), beanType));

        return codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, manufacturer);
    }

    private void makeUnderlyingCodegen(CodegenMethod method, CodegenClassScope codegenClassScope) {
        CodegenExpressionField schema = codegenClassScope.getPackageScope().addFieldUnshared(true, Schema.class, staticMethod(AvroSchemaUtil.class, "resolveAvroSchema", EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF)));
        method.getBlock()
                .declareVar(GenericData.Record.class, "record", newInstance(GenericData.Record.class, schema));

        for (int i = 0; i < indexPerWritable.length; i++) {
            method.getBlock().exprDotMethod(ref("record"), "put", constant(indexPerWritable[i]), arrayAtIndex(ref("properties"), constant(i)));
        }
        method.getBlock().methodReturn(ref("record"));
    }
}
