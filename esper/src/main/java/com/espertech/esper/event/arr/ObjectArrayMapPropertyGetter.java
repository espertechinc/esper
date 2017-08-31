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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.map.MapEventPropertyGetter;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ObjectArrayMapPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int index;
    private final MapEventPropertyGetter getter;

    /**
     * Ctor.
     *
     * @param getter is the getter to use to interrogate the property in the map
     * @param index  index
     */
    public ObjectArrayMapPropertyGetter(int index, MapEventPropertyGetter getter) {
        if (getter == null) {
            throw new IllegalArgumentException("Getter is a required parameter");
        }
        this.index = index;
        this.getter = getter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object valueTopObj = array[index];
        if (!(valueTopObj instanceof Map)) {
            return null;
        }
        return getter.getMap((Map) valueTopObj);
    }

    private CodegenMethodNode getObjectArrayCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object[].class, "array").getBlock()
                .declareVar(Object.class, "valueTopObj", arrayAtIndex(ref("array"), constant(index)))
                .ifRefNotTypeReturnConst("valueTopObj", Map.class, null)
                .methodReturn(getter.underlyingGetCodegen(cast(Map.class, ref("valueTopObj")), codegenMethodScope, codegenClassScope));
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        Object valueTopObj = array[index];
        if (!(valueTopObj instanceof Map)) {
            return false;
        }
        return getter.isMapExistsProperty((Map) valueTopObj);
    }

    private CodegenMethodNode isObjectArrayExistsPropertyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Object[].class, "array").getBlock()
                .declareVar(Object.class, "valueTopObj", arrayAtIndex(ref("array"), constant(index)))
                .ifRefNotTypeReturnConst("valueTopObj", Map.class, false)
                .methodReturn(getter.underlyingExistsCodegen(cast(Map.class, ref("valueTopObj")), codegenMethodScope, codegenClassScope));
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return isObjectArrayExistsProperty(array);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Object[].class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(Object[].class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getObjectArrayCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(isObjectArrayExistsPropertyCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
