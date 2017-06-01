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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for an array of event bean using a nested getter.
 */
public class MapEventBeanArrayIndexedElementPropertyGetter implements MapEventPropertyGetter {
    private final String propertyName;
    private final int index;
    private final EventPropertyGetterSPI nestedGetter;

    /**
     * Ctor.
     *
     * @param propertyName property name
     * @param index        array index
     * @param nestedGetter nested getter
     */
    public MapEventBeanArrayIndexedElementPropertyGetter(String propertyName, int index, EventPropertyGetterSPI nestedGetter) {
        this.propertyName = propertyName;
        this.index = index;
        this.nestedGetter = nestedGetter;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        EventBean[] wrapper = (EventBean[]) map.get(propertyName);
        return BaseNestableEventUtil.getArrayPropertyValue(wrapper, index, nestedGetter);
    }

    private String getMapCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Map.class, "map", this.getClass())
                .declareVar(EventBean[].class, "wrapper", cast(EventBean[].class, exprDotMethod(ref("map"), "get", constant(propertyName))))
                .methodReturn(localMethod(BaseNestableEventUtil.getArrayPropertyValueCodegen(context, index, nestedGetter), ref("wrapper")));
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(obj);
        EventBean[] wrapper = (EventBean[]) map.get(propertyName);
        return BaseNestableEventUtil.getArrayPropertyFragment(wrapper, index, nestedGetter);
    }

    private String getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Map.class, "map", this.getClass())
                .declareVar(EventBean[].class, "wrapper", cast(EventBean[].class, exprDotMethod(ref("map"), "get", constant(propertyName))))
                .methodReturn(localMethod(BaseNestableEventUtil.getArrayPropertyFragmentCodegen(context, index, nestedGetter), ref("wrapper")));
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingFragment(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getMapCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}