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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for map array.
 */
public class MapFragmentArrayPropertyGetter implements MapEventPropertyGetter {
    private final String propertyName;
    private final EventType fragmentEventType;
    private final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyNameAtomic  property type
     * @param fragmentEventType   event type of fragment
     * @param eventAdapterService for creating event instances
     */
    public MapFragmentArrayPropertyGetter(String propertyNameAtomic, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        this.propertyName = propertyNameAtomic;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        return map.get(propertyName);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true;
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        Object value = get(eventBean);
        if (value instanceof EventBean[]) {
            return value;
        }
        return BaseNestableEventUtil.getBNFragmentArray(value, fragmentEventType, eventAdapterService);
    }

    private String getFragmentCodegen(CodegenContext context) {
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(EventType.class, fragmentEventType);
        return context.addMethod(Object.class, Map.class, "map", this.getClass())
                .declareVar(Object.class, "value", codegenUnderlyingGet(ref("map"), context))
                .ifInstanceOf("value", EventBean[].class)
                    .blockReturn(ref("value"))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNFragmentArray", ref("value"), ref(mType.getMemberName()), ref(mSvc.getMemberName())));
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
        return exprDotMethod(underlyingExpression, "get", constant(propertyName));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}
