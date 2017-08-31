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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
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

    private CodegenMethodNode getMapInternalCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").addParam(int.class, "index").getBlock()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyName)))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNArrayValueAtIndexWithNullCheck", ref("value"), ref("index")));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean obj) throws PropertyAccessException {
        Object value = get(obj);
        return BaseNestableEventUtil.getBNFragmentNonPojo(value, fragmentType, eventAdapterService);
    }

    private CodegenMethodNode getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember mSvc = codegenClassScope.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = codegenClassScope.makeAddMember(EventType.class, fragmentType);
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("map"), codegenMethodScope, codegenClassScope))
                .methodReturn(staticMethod(BaseNestableEventUtil.class, "getBNFragmentNonPojo", ref("value"), member(mType.getMemberId()), member(mSvc.getMemberId())));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getMapInternalCodegen(codegenMethodScope, codegenClassScope), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getMapInternalCodegen(codegenMethodScope, codegenClassScope), castUnderlying(Map.class, beanExpression), key);
    }
}