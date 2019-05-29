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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.getter.BaseNativePropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.util.CollectionUtil;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter that works on arrays residing within a Map as an event property.
 */
public class ObjectArrayArrayPOJOEntryIndexedPropertyGetter extends BaseNativePropertyGetter implements ObjectArrayEventPropertyGetterAndIndexed {
    private final int propertyIndex;
    private final int index;

    public static Object getArrayValue(Object[] array, int propertyIndex, int index) throws PropertyAccessException {
        // If the oa does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.getBNArrayValueAtIndexWithNullCheck(value, index);
    }

    public ObjectArrayArrayPOJOEntryIndexedPropertyGetter(int propertyIndex, int index, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, Class returnType) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, returnType, null);
        this.propertyIndex = propertyIndex;
        this.index = index;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return getArrayValue(array, propertyIndex, index);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return array.length > index;
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getArrayValue(array, propertyIndex, index);
    }

    public Object get(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return CollectionUtil.arrayExistsAtIndex(array[propertyIndex], index);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Object[].class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(Object[].class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "getArrayValue", underlyingExpression, constant(propertyIndex), constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayExistsAtIndex", arrayAtIndex(underlyingExpression, constant(propertyIndex)), constant(index));
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return staticMethod(this.getClass(), "getArrayValue", castUnderlying(Object[].class, beanExpression), constant(propertyIndex), key);
    }

    public Class getTargetType() {
        return Object[].class;
    }

    public Class getBeanPropType() {
        return Object.class;
    }
}
