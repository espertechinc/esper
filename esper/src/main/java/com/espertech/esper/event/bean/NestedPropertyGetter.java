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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;

import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for one or more levels deep nested properties.
 */
public class NestedPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter {
    private final BeanEventPropertyGetter[] getterChain;

    /**
     * Ctor.
     *
     * @param getterChain         is the chain of getters to retrieve each nested property
     * @param eventAdapterService is the cache and factory for event bean types and event wrappers
     * @param finalPropertyType   type of the entry returned
     * @param finalGenericType    generic type parameter of the entry returned, if any
     */
    public NestedPropertyGetter(List<EventPropertyGetter> getterChain, EventAdapterService eventAdapterService, Class finalPropertyType, Class finalGenericType) {
        super(eventAdapterService, finalPropertyType, finalGenericType);
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

    public Class getBeanPropType() {
        return getterChain[getterChain.length - 1].getBeanPropType();
    }

    public Class getTargetType() {
        return getterChain[0].getTargetType();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingExistsCodegen(castUnderlying(getTargetType(), beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getBeanPropCodegen(context, false), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getBeanPropCodegen(context, true), underlyingExpression);
    }

    private CodegenMethodId getBeanPropCodegen(CodegenContext context, boolean exists) {
        CodegenBlock block = context.addMethod(exists ? boolean.class : JavaClassHelper.getBoxedType(getterChain[getterChain.length - 1].getBeanPropType()), this.getClass()).add(getterChain[0].getTargetType(), "value").begin();
        if (!exists) {
            block.ifRefNullReturnNull("value");
        } else {
            block.ifRefNullReturnFalse("value");
        }
        String lastName = "value";
        for (int i = 0; i < getterChain.length - 1; i++) {
            String varName = "l" + i;
            block.declareVar(getterChain[i].getBeanPropType(), varName, getterChain[i].underlyingGetCodegen(ref(lastName), context));
            lastName = varName;
            if (!exists) {
                block.ifRefNullReturnNull(lastName);
            } else {
                block.ifRefNullReturnFalse(lastName);
            }
        }
        if (!exists) {
            return block.methodReturn(getterChain[getterChain.length - 1].underlyingGetCodegen(ref(lastName), context));
        } else {
            return block.methodReturn(getterChain[getterChain.length - 1].underlyingExistsCodegen(ref(lastName), context));
        }
    }
}
