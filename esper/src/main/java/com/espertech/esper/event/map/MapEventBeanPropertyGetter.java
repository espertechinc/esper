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

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class MapEventBeanPropertyGetter implements MapEventPropertyGetter {
    private final String propertyName;
    private final Class underlyingType;

    public MapEventBeanPropertyGetter(String propertyName, Class underlyingType) {
        this.propertyName = propertyName;
        this.underlyingType = underlyingType;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object eventBean = map.get(propertyName);

        if (eventBean == null) {
            return null;
        }

        EventBean theEvent = (EventBean) eventBean;
        return theEvent.getUnderlying();
    }

    private String getMapCodegen(CodegenContext context) {
        return context.addMethod(underlyingType, this.getClass()).add(Map.class, "map").begin()
                .declareVar(Object.class, "eventBean", exprDotMethod(ref("map"), "get", constant(propertyName)))
                .ifRefNullReturnNull("eventBean")
                .methodReturn(cast(underlyingType, exprDotMethod(cast(EventBean.class, ref("eventBean")), "getUnderlying")));
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
        return map.get(propertyName);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingFragmentCodegen(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getMapCodegen(context), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return exprDotMethod(underlyingExpression, "get", constant(propertyName));
    }
}
