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
 * Getter for Map-entries with well-defined fragment type.
 */
public class MapArrayPropertyGetter implements MapEventPropertyGetter, MapEventPropertyGetterAndIndexed {
    private final String propertyName;
    private final int index;
    private final EventAdapterService eventAdapterService;
    private final EventType fragmentType;

    /**
     * Ctor.
     *
     * @param propertyNameAtomic  property name
     * @param index               array index
     * @param eventAdapterService factory for event beans and event types
     * @param fragmentType        type of the entry returned
     */
    public MapArrayPropertyGetter(String propertyNameAtomic, int index, EventAdapterService eventAdapterService, EventType fragmentType) {
        this.propertyName = propertyNameAtomic;
        this.index = index;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        return getMapInternal(map, index);
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        return getMapInternal(map, index);
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(obj);
        return getMap(map);
    }

    private Object getMapInternal(Map<String, Object> map, int index) throws PropertyAccessException {
        Object value = map.get(propertyName);
        return BaseNestableEventUtil.getBNArrayValueAtIndexWithNullCheck(value, index);
    }

    private String getMapInternalCodegen(CodegenContext context) {
        return context.addMethod(Object.class, Map.class, "map", this.getClass())
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyName)))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndexWithNullCheck", ref("value"), constant(index)));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean obj) throws PropertyAccessException {
        Object value = get(obj);
        return BaseNestableEventUtil.getBNFragmentNonPojo(value, fragmentType, eventAdapterService);
    }

    private String getFragmentCodegen(CodegenContext context) {
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(EventType.class, fragmentType);
        return context.addMethod(Object.class, Map.class, "map", this.getClass())
                .declareVar(Object.class, "value", codegenUnderlyingGet(ref("map"), context))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNFragmentNonPojo", ref("value"), ref(mType.getMemberName()), ref(mSvc.getMemberName())));
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
        return localMethod(getMapInternalCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}