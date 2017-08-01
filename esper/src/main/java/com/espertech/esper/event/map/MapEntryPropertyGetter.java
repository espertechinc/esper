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
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class MapEntryPropertyGetter implements MapEventPropertyGetter {
    private final String propertyName;
    private final EventAdapterService eventAdapterService;
    private final BeanEventType eventType;

    /**
     * Ctor.
     *
     * @param propertyName        property to get
     * @param eventAdapterService factory for event beans and event types
     * @param eventType           type of the entry returned
     */
    public MapEntryPropertyGetter(String propertyName, BeanEventType eventType, EventAdapterService eventAdapterService) {
        this.propertyName = propertyName;
        this.eventAdapterService = eventAdapterService;
        this.eventType = eventType;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = map.get(propertyName);
        if (value instanceof EventBean) {
            return ((EventBean) value).getUnderlying();
        }
        return value;
    }

    private CodegenExpression getMapCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        String method = context.addMethod(Object.class, MapEntryPropertyGetter.class).add(Map.class, "map").begin()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyName)))
                .ifInstanceOf("value", EventBean.class)
                    .blockReturn(exprDotUnderlying(cast(EventBean.class, ref("value"))))
                .methodReturn(ref("value"));
        return localMethod(method, underlyingExpression);
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

    public Object getFragment(EventBean eventBean) {
        if (eventType == null) {
            return null;
        }
        Object result = get(eventBean);
        return BaseNestableEventUtil.getBNFragmentPojo(result, eventType, eventAdapterService);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        if (eventType == null) {
            return constantNull();
        }
        return underlyingFragmentCodegen(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return getMapCodegen(underlyingExpression, context);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return exprDotMethod(underlyingExpression, "containsKey", constant(propertyName));
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        if (eventType == null) {
            return constantNull();
        }
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(BeanEventType.class, eventType);
        return staticMethod(BaseNestableEventUtil.class, "getBNFragmentPojo", underlyingGetCodegen(underlyingExpression, context), ref(mType.getMemberName()), ref(mSvc.getMemberName()));
    }
}
