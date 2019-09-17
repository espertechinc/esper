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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public class JsonGetterSimpleProvidedWFragmentSimple extends JsonGetterSimpleProvidedBase {

    public JsonGetterSimpleProvidedWFragmentSimple(Field field, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        super(field, fragmentType, eventBeanTypedEventFactory);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(field.getDeclaringClass(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentType == null) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        if (fragmentType == null) {
            return null;
        }
        return JsonFieldGetterHelperProvided.handleJsonProvidedCreateFragmentSimple(object, field, fragmentType, eventBeanTypedEventFactory);
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(fragmentType, EPStatementInitServices.REF));
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(field.getDeclaringClass(), "record");
        method.getBlock()
            .declareVar(Object.class, "value", underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope))
            .ifRefNullReturnNull("value");
        String adapterMethod;
        if (fragmentType instanceof BeanEventType) {
            adapterMethod = "adapterForTypedBean";
        } else if (fragmentType instanceof JsonEventType) {
            adapterMethod = "adapterForTypedJson";
        } else {
            throw new IllegalStateException("Unrecognized fragment event type " + fragmentType);
        }
        method.getBlock().methodReturn(exprDotMethod(factory, adapterMethod, ref("value"), eventType));
        return method;
    }
}
