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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.getter.BaseNativePropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class ObjectArrayPOJOEntryPropertyGetter extends BaseNativePropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final BeanEventPropertyGetter entryGetter;

    public ObjectArrayPOJOEntryPropertyGetter(int propertyIndex, BeanEventPropertyGetter entryGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory, EPTypeClass eptype) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, eptype);
        this.propertyIndex = propertyIndex;
        this.entryGetter = entryGetter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];

        if (value == null) {
            return null;
        }

        // Object within the map
        if (value instanceof EventBean) {
            return entryGetter.get((EventBean) value);
        }
        return entryGetter.getBeanProp(value);
    }

    private CodegenMethod getObjectArrayCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(EPTypePremade.OBJECTARRAY.getEPType(), "array").getBlock()
            .declareVar(EPTypePremade.OBJECT.getEPType(), "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
            .ifRefNullReturnNull("value")
            .ifInstanceOf("value", EventBean.EPTYPE)
            .blockReturn(entryGetter.eventBeanGetCodegen(castRef(EventBean.EPTYPE, "value"), codegenMethodScope, codegenClassScope))
            .methodReturn(entryGetter.underlyingGetCodegen(cast(entryGetter.getTargetType(), ref("value")), codegenMethodScope, codegenClassScope));
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return isExistsProperty(array);
    }

    private boolean isExistsProperty(Object[] array) {
        Object value = array[propertyIndex];

        if (value == null) {
            return false;
        }

        // Object within the map
        if (value instanceof EventBean) {
            return entryGetter.isExistsProperty((EventBean) value);
        }
        return entryGetter.isBeanExistsProperty(value);
    }

    private CodegenMethod isExistsPropertyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), this.getClass(), codegenClassScope).addParam(EPTypePremade.OBJECTARRAY.getEPType(), "array").getBlock()
            .declareVar(EPTypePremade.OBJECT.getEPType(), "value", arrayAtIndex(ref("array"), constant(propertyIndex)))
            .ifRefNullReturnFalse("value")
            .ifInstanceOf("value", EventBean.EPTYPE)
            .blockReturn(entryGetter.eventBeanExistsCodegen(castRef(EventBean.EPTYPE, "value"), codegenMethodScope, codegenClassScope))
            .methodReturn(entryGetter.underlyingExistsCodegen(cast(entryGetter.getTargetType(), ref("value")), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(EPTypePremade.OBJECTARRAY.getEPType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(EPTypePremade.OBJECTARRAY.getEPType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getObjectArrayCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(isExistsPropertyCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public EPTypeClass getTargetType() {
        return EPTypePremade.OBJECTARRAY.getEPType();
    }

    @Override
    public EPTypeClass getBeanPropType() {
        return EPTypePremade.OBJECT.getEPType();
    }
}
