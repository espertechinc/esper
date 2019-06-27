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
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorFactory;
import com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorForge;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.SimpleTypeCaster;
import com.espertech.esper.common.internal.util.SimpleTypeCasterFactory;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for event beans created and populate anew from a set of values.
 */
public class EventBeanManufacturerBeanForge implements EventBeanManufacturerForge {
    private final BeanInstantiatorForge beanInstantiator;
    private final BeanEventType beanEventType;
    private final WriteablePropertyDescriptor[] properties;
    private final ClasspathImportService classpathImportService;
    private final Method[] writeMethodsReflection;
    private final boolean[] primitiveType;

    /**
     * Ctor.
     *
     * @param beanEventType          target type
     * @param properties             written properties
     * @param classpathImportService for resolving write methods
     * @throws EventBeanManufactureException if the write method lookup fail
     */
    public EventBeanManufacturerBeanForge(BeanEventType beanEventType,
                                          WriteablePropertyDescriptor[] properties,
                                          ClasspathImportService classpathImportService
    )
            throws EventBeanManufactureException {
        this.beanEventType = beanEventType;
        this.properties = properties;
        this.classpathImportService = classpathImportService;

        beanInstantiator = BeanInstantiatorFactory.makeInstantiator(beanEventType, classpathImportService);

        writeMethodsReflection = new Method[properties.length];

        primitiveType = new boolean[properties.length];
        for (int i = 0; i < properties.length; i++) {
            writeMethodsReflection[i] = properties[i].getWriteMethod();
            primitiveType[i] = properties[i].getType().isPrimitive();
        }
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) throws EventBeanManufactureException {
        return new EventBeanManufacturerBean(beanEventType, eventBeanTypedEventFactory, properties, classpathImportService);
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod init = codegenClassScope.getPackageScope().getInitMethod();

        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField beanType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(beanEventType, EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass manufacturer = newAnonymousClass(init.getBlock(), EventBeanManufacturer.class);

        CodegenMethod makeUndMethod = CodegenMethod.makeParentNode(Object.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("makeUnderlying", makeUndMethod);
        makeUnderlyingCodegen(makeUndMethod, codegenClassScope);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(EventBean.class, this.getClass(), codegenClassScope).addParam(Object[].class, "properties");
        manufacturer.addMethod("make", makeMethod);
        makeMethod.getBlock()
                .declareVar(Object.class, "und", localMethod(makeUndMethod, ref("properties")))
                .methodReturn(exprDotMethod(factory, "adapterForTypedBean", ref("und"), beanType));

        return codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, manufacturer);
    }

    private void makeUnderlyingCodegen(CodegenMethod method, CodegenClassScope codegenClassScope) {
        method.getBlock()
                .declareVar(beanEventType.getUnderlyingType(), "und", cast(beanEventType.getUnderlyingType(), beanInstantiator.make(method, codegenClassScope)))
                .declareVar(Object.class, "value", constantNull());

        for (int i = 0; i < writeMethodsReflection.length; i++) {
            method.getBlock().assignRef("value", arrayAtIndex(ref("properties"), constant(i)));

            Class targetType = writeMethodsReflection[i].getParameterTypes()[0];
            CodegenExpression value;
            if (targetType.isPrimitive()) {
                SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(Object.class, targetType);
                value = caster.codegen(ref("value"), Object.class, method, codegenClassScope);
            } else {
                value = cast(targetType, ref("value"));
            }
            CodegenExpression set = exprDotMethod(ref("und"), writeMethodsReflection[i].getName(), value);
            if (primitiveType[i]) {
                method.getBlock().ifRefNotNull("value").expression(set).blockEnd();
            } else {
                method.getBlock().expression(set);
            }
        }
        method.getBlock().methodReturn(ref("und"));
    }
}
