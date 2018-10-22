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
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class VariantEventPropertyGetterAny implements EventPropertyGetterSPI {
    private final VariantEventType variantEventType;
    private final String propertyName;

    public VariantEventPropertyGetterAny(VariantEventType variantEventType, String propertyName) {
        this.variantEventType = variantEventType;
        this.propertyName = propertyName;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventBean           bean
     * @param propertyGetterCache cache
     * @param propertyName        name
     * @return value
     * @throws PropertyAccessException ex
     */
    public static Object variantGet(EventBean eventBean, VariantPropertyGetterCache propertyGetterCache, String propertyName) throws PropertyAccessException {
        VariantEvent variant = (VariantEvent) eventBean;
        EventPropertyGetter getter = propertyGetterCache.getGetter(propertyName, variant.getUnderlyingEventBean().getEventType());
        if (getter == null) {
            return null;
        }
        Object result = getter.get(variant.getUnderlyingEventBean());
        return result;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventBean           bean
     * @param propertyGetterCache cache
     * @param propertyName        name
     * @return value
     */
    public static boolean variantExists(EventBean eventBean, VariantPropertyGetterCache propertyGetterCache, String propertyName) {
        VariantEvent variant = (VariantEvent) eventBean;
        EventPropertyGetter getter = propertyGetterCache.getGetter(propertyName, variant.getUnderlyingEventBean().getEventType());
        return getter != null && getter.isExistsProperty(variant.getUnderlyingEventBean());
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return variantGet(eventBean, variantEventType.getVariantPropertyGetterCache(), propertyName);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return variantExists(eventBean, variantEventType.getVariantPropertyGetterCache(), propertyName);
    }

    public Object getFragment(EventBean eventBean) {
        return null; // no fragments provided as the type is not known in advance
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField cache = codegenClassScope.addOrGetFieldSharable(new VariantPropertyGetterCacheCodegenField(variantEventType));
        return staticMethod(this.getClass(), "variantGet", beanExpression, cache, constant(propertyName));
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField cache = codegenClassScope.addOrGetFieldSharable(new VariantPropertyGetterCacheCodegenField(variantEventType));
        return staticMethod(this.getClass(), "variantExists", beanExpression, cache, constant(propertyName));
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
        return constantNull();
    }

    protected static UnsupportedOperationException variantImplementationNotProvided() {
        return new UnsupportedOperationException("Variant event type does not provide an implementation for underlying get");
    }
}
