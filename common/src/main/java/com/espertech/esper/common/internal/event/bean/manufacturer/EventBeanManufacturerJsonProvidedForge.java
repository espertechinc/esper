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
import com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorForge;
import com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorForgeByNewInstanceReflection;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.SimpleTypeCaster;
import com.espertech.esper.common.internal.util.SimpleTypeCasterFactory;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for event beans created and populate anew from a set of values.
 */
public class EventBeanManufacturerJsonProvidedForge implements EventBeanManufacturerForge {
    private final BeanInstantiatorForge beanInstantiator;
    private final JsonEventType jsonEventType;
    private final WriteablePropertyDescriptor[] properties;
    private final ClasspathImportService classpathImportService;
    private final Field[] writeFieldReflection;
    private final boolean[] primitiveType;

    /**
     * Ctor.
     *
     * @param jsonEventType          target type
     * @param properties             written properties
     * @param classpathImportService for resolving write methods
     * @throws EventBeanManufactureException if the write method lookup fail
     */
    public EventBeanManufacturerJsonProvidedForge(JsonEventType jsonEventType,
                                                  WriteablePropertyDescriptor[] properties,
                                                  ClasspathImportService classpathImportService
    )
            throws EventBeanManufactureException {
        this.jsonEventType = jsonEventType;
        this.properties = properties;
        this.classpathImportService = classpathImportService;

        beanInstantiator = new BeanInstantiatorForgeByNewInstanceReflection(jsonEventType.getUnderlyingType());

        writeFieldReflection = new Field[properties.length];

        primitiveType = new boolean[properties.length];
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();
            JsonUnderlyingField field = jsonEventType.getDetail().getFieldDescriptors().get(propertyName);
            writeFieldReflection[i] = field.getOptionalField();
            primitiveType[i] = properties[i].getType().isPrimitive();
        }
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) throws EventBeanManufactureException {
        return new EventBeanManufacturerJsonProvided(jsonEventType, eventBeanTypedEventFactory, properties, classpathImportService);
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod init = codegenClassScope.getPackageScope().getInitMethod();

        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField beanType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(jsonEventType, EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass manufacturer = newAnonymousClass(init.getBlock(), EventBeanManufacturer.class);

        CodegenMethod makeUndMethod = CodegenMethod.makeParentNode(Object.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("makeUnderlying", makeUndMethod);
        makeUnderlyingCodegen(makeUndMethod, codegenClassScope);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(EventBean.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("make", makeMethod);
        makeMethod.getBlock()
                .declareVar(Object.class, "und", localMethod(makeUndMethod, ref("properties")))
                .methodReturn(exprDotMethod(factory, "adapterForTypedJson", ref("und"), beanType));

        return codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, manufacturer);
    }

    private void makeUnderlyingCodegen(CodegenMethod method, CodegenClassScope codegenClassScope) {
        method.getBlock()
                .declareVar(jsonEventType.getUnderlyingType(), "und", cast(jsonEventType.getUnderlyingType(), beanInstantiator.make(method, codegenClassScope)))
                .declareVar(Object.class, "value", constantNull());

        for (int i = 0; i < writeFieldReflection.length; i++) {
            method.getBlock().assignRef("value", arrayAtIndex(ref("properties"), constant(i)));

            Class targetType = writeFieldReflection[i].getType();
            CodegenExpression value;
            if (targetType.isPrimitive()) {
                SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(Object.class, targetType);
                value = caster.codegen(ref("value"), Object.class, method, codegenClassScope);
            } else {
                value = cast(targetType, ref("value"));
            }
            CodegenExpression set = assign(exprDotName(ref("und"), writeFieldReflection[i].getName()), value);
            if (primitiveType[i]) {
                method.getBlock().ifRefNotNull("value").expression(set).blockEnd();
            } else {
                method.getBlock().expression(set);
            }
        }
        method.getBlock().methodReturn(ref("und"));
    }
}
