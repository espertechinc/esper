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
package com.espertech.esper.common.internal.event.json.getter.fromschema;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public class JsonGetterSimpleSchemaWFragment extends JsonGetterSimpleSchemaBase {

    public JsonGetterSimpleSchemaWFragment(JsonUnderlyingField field, String underlyingClassName, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        super(field, underlyingClassName, fragmentType, eventBeanTypedEventFactory);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentType == null) {
            return constantNull();
        }
        CodegenExpression factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpression eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(fragmentType, EPStatementInitServices.REF));
        return staticMethod(JsonFieldGetterHelperSchema.class, "handleJsonCreateFragmentSimple", underlyingExpression, constant(field.getPropertyNumber()), eventType, factory);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        if (fragmentType == null) {
            return null;
        }
        return JsonFieldGetterHelperSchema.handleJsonCreateFragmentSimple((JsonEventObjectBase) object, field.getPropertyNumber(), fragmentType, eventBeanTypedEventFactory);
    }
}
