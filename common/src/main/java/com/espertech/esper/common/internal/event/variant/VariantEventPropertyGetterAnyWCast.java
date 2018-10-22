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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.util.SimpleTypeCaster;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.event.variant.VariantEventPropertyGetterAny.variantImplementationNotProvided;

public class VariantEventPropertyGetterAnyWCast implements EventPropertyGetterSPI {
    private final VariantEventType variantEventType;
    private final String propertyName;
    private final SimpleTypeCaster caster;

    public VariantEventPropertyGetterAnyWCast(VariantEventType variantEventType, String propertyName, SimpleTypeCaster caster) {
        this.variantEventType = variantEventType;
        this.propertyName = propertyName;
        this.caster = caster;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object value = VariantEventPropertyGetterAny.variantGet(eventBean, variantEventType.getVariantPropertyGetterCache(), propertyName);
        if (value == null) {
            return null;
        }
        return caster.cast(value);
    }

    private CodegenMethod getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        CodegenExpressionField cache = codegenClassScope.addOrGetFieldSharable(new VariantPropertyGetterCacheCodegenField(variantEventType));
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(EventBean.class, "eventBean");
        method.getBlock()
                .declareVar(Object.class, "value", staticMethod(VariantEventPropertyGetterAny.class, "variantGet", ref("eventBean"), cache, constant(propertyName)))
                .methodReturn(caster.codegen(ref("value"), Object.class, method, codegenClassScope));
        return method;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return VariantEventPropertyGetterAny.variantExists(eventBean, variantEventType.getVariantPropertyGetterCache(), propertyName);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), beanExpression);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField cache = codegenClassScope.addOrGetFieldSharable(new VariantPropertyGetterCacheCodegenField(variantEventType));
        return staticMethod(VariantEventPropertyGetterAny.class, "variantExists", beanExpression, cache, constant(propertyName));
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw variantImplementationNotProvided();
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw variantImplementationNotProvided();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        throw variantImplementationNotProvided();
    }
}
