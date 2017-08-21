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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class VariantEventPropertyGetterAny implements EventPropertyGetterSPI {
    private final VariantPropertyGetterCache propertyGetterCache;
    private final int assignedPropertyNumber;

    public VariantEventPropertyGetterAny(VariantPropertyGetterCache propertyGetterCache, int assignedPropertyNumber) {
        this.propertyGetterCache = propertyGetterCache;
        this.assignedPropertyNumber = assignedPropertyNumber;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventBean              bean
     * @param propertyGetterCache    cache
     * @param assignedPropertyNumber num
     * @return value
     * @throws PropertyAccessException ex
     */
    public static Object variantGet(EventBean eventBean, VariantPropertyGetterCache propertyGetterCache, int assignedPropertyNumber) throws PropertyAccessException {
        VariantEvent variant = (VariantEvent) eventBean;
        EventPropertyGetter getter = propertyGetterCache.getGetter(assignedPropertyNumber, variant.getUnderlyingEventBean().getEventType());
        if (getter == null) {
            return null;
        }
        return getter.get(variant.getUnderlyingEventBean());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventBean              bean
     * @param propertyGetterCache    cache
     * @param assignedPropertyNumber num
     * @return value
     */
    public static boolean variantExists(EventBean eventBean, VariantPropertyGetterCache propertyGetterCache, int assignedPropertyNumber) {
        VariantEvent variant = (VariantEvent) eventBean;
        EventPropertyGetter getter = propertyGetterCache.getGetter(assignedPropertyNumber, variant.getUnderlyingEventBean().getEventType());
        return getter != null && getter.isExistsProperty(variant.getUnderlyingEventBean());
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return variantGet(eventBean, propertyGetterCache, assignedPropertyNumber);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return variantExists(eventBean, propertyGetterCache, assignedPropertyNumber);
    }

    public Object getFragment(EventBean eventBean) {
        return null; // no fragments provided as the type is not known in advance
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember member = codegenClassScope.makeAddMember(VariantPropertyGetterCache.class, propertyGetterCache);
        return staticMethod(this.getClass(), "variantGet", beanExpression, member(member.getMemberId()), constant(assignedPropertyNumber));
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember member = codegenClassScope.makeAddMember(VariantPropertyGetterCache.class, propertyGetterCache);
        return staticMethod(this.getClass(), "variantExists", beanExpression, member(member.getMemberId()), constant(assignedPropertyNumber));
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
