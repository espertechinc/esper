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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for map entry.
 */
public abstract class MapPropertyGetterDefaultBase implements MapEventPropertyGetter {
    private final String propertyName;
    protected final EventType fragmentEventType;
    protected final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyNameAtomic  property name
     * @param fragmentEventType   fragment type
     * @param eventAdapterService factory for event beans and event types
     */
    public MapPropertyGetterDefaultBase(String propertyNameAtomic, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        this.propertyName = propertyNameAtomic;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    protected abstract Object handleCreateFragment(Object value);
    protected abstract CodegenExpression handleCreateFragmentCodegen(CodegenExpression value, CodegenContext context);

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
        return handleCreateFragment(value);
    }

    private String getFragmentCodegen(CodegenContext context) throws PropertyAccessException {
        return context.addMethod(Object.class, Map.class, "underlying", this.getClass())
                .declareVar(Object.class, "value", codegenUnderlyingGet(ref("underlying"), context))
                .methodReturn(handleCreateFragmentCodegen(ref("value"), context));
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Map.class, beanExpression), context);
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
