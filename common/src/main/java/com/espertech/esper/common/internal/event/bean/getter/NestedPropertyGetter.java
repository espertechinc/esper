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
package com.espertech.esper.common.internal.event.bean.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for one or more levels deep nested properties.
 */
public class NestedPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter {
    private final BeanEventPropertyGetter[] getterChain;

    public NestedPropertyGetter(List<EventPropertyGetter> getterChain, EventBeanTypedEventFactory eventBeanTypedEventFactory, EPTypeClass type, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, type);
        this.getterChain = new BeanEventPropertyGetter[getterChain.size()];

        for (int i = 0; i < getterChain.size(); i++) {
            this.getterChain[i] = (BeanEventPropertyGetter) getterChain.get(i);
        }
    }

    public Object getBeanProp(Object value) throws PropertyAccessException {
        if (value == null) {
            return value;
        }

        for (int i = 0; i < getterChain.length; i++) {
            value = getterChain[i].getBeanProp(value);

            if (value == null) {
                return null;
            }
        }
        return value;
    }

    public boolean isBeanExistsProperty(Object value) {
        if (value == null) {
            return false;
        }

        int lastElementIndex = getterChain.length - 1;

        // walk the getter chain up to the previous-to-last element, returning its object value.
        // any null values in between mean the property does not exists
        for (int i = 0; i < getterChain.length - 1; i++) {
            value = getterChain[i].getBeanProp(value);

            if (value == null) {
                return false;
            }
        }

        return getterChain[lastElementIndex].isBeanExistsProperty(value);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getBeanProp(eventBean.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isBeanExistsProperty(eventBean.getUnderlying());
    }

    public EPTypeClass getTargetType() {
        return getterChain[0].getTargetType();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropCodegen(codegenMethodScope, codegenClassScope, false), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropCodegen(codegenMethodScope, codegenClassScope, true), underlyingExpression);
    }

    private CodegenMethod getBeanPropCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, boolean exists) {
        EPTypeClass typeClass = getterChain[getterChain.length - 1].getBeanPropType();
        EPTypeClass typeClassBoxed = JavaClassHelper.getBoxedType(typeClass);
        EPTypeClass targetType = getterChain[0].getTargetType();
        CodegenBlock block = codegenMethodScope.makeChild(exists ? EPTypePremade.BOOLEANPRIMITIVE.getEPType() : typeClassBoxed, this.getClass(), codegenClassScope).addParam(targetType, "value").getBlock();
        if (!exists) {
            block.ifRefNullReturnNull("value");
        } else {
            block.ifRefNullReturnFalse("value");
        }
        String lastName = "value";
        for (int i = 0; i < getterChain.length - 1; i++) {
            String varName = "l" + i;
            block.declareVar(getterChain[i].getBeanPropType(), varName, getterChain[i].underlyingGetCodegen(ref(lastName), codegenMethodScope, codegenClassScope));
            lastName = varName;
            if (!exists) {
                block.ifRefNullReturnNull(lastName);
            } else {
                block.ifRefNullReturnFalse(lastName);
            }
        }
        if (!exists) {
            return block.methodReturn(getterChain[getterChain.length - 1].underlyingGetCodegen(ref(lastName), codegenMethodScope, codegenClassScope));
        } else {
            return block.methodReturn(getterChain[getterChain.length - 1].underlyingExistsCodegen(ref(lastName), codegenMethodScope, codegenClassScope));
        }
    }
}
