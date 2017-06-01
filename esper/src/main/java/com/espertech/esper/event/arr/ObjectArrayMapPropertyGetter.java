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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
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

    private String getObjectArrayCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "valueTopObj", arrayAtIndex(ref("array"), constant(index)))
                .ifRefNotTypeReturnConst("valueTopObj", Map.class, null)
                .methodReturn(getter.codegenUnderlyingGet(cast(Map.class, ref("valueTopObj")), context));
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        Object valueTopObj = array[index];
        if (!(valueTopObj instanceof Map)) {
            return false;
        }
        return getter.isMapExistsProperty((Map) valueTopObj);
    }

    private String isObjectArrayExistsPropertyCodegen(CodegenContext context) {
        return context.addMethod(boolean.class, Object[].class, "array", this.getClass())
                .declareVar(Object.class, "valueTopObj", arrayAtIndex(ref("array"), constant(index)))
                .ifRefNotTypeReturnConst("valueTopObj", Map.class, false)
                .methodReturn(getter.codegenUnderlyingExists(cast(Map.class, ref("valueTopObj")), context));
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

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Object[].class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getObjectArrayCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(isObjectArrayExistsPropertyCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}
