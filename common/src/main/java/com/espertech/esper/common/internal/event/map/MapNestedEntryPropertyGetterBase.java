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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class MapNestedEntryPropertyGetterBase implements MapEventPropertyGetter {

    protected final String propertyMap;
    protected final EventType fragmentType;
    protected final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    /**
     * Ctor.
     *
     * @param propertyMap                the property to look at
     * @param eventBeanTypedEventFactory factory for event beans and event types
     * @param fragmentType               type of the entry returned
     */
    public MapNestedEntryPropertyGetterBase(String propertyMap, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.propertyMap = propertyMap;
        this.fragmentType = fragmentType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public abstract Object handleNestedValue(Object value);

    public abstract boolean handleNestedValueExists(Object value);

    public abstract Object handleNestedValueFragment(Object value);

    public abstract CodegenExpression handleNestedValueCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);

    public abstract CodegenExpression handleNestedValueExistsCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);

    public abstract CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope);

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        return handleNestedValue(value);
    }

    private CodegenMethod getMapCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
                .ifRefNullReturnNull("value")
                .methodReturn(handleNestedValueCodegen(ref("value"), codegenMethodScope, codegenClassScope));
    }

    private CodegenMethod getMapExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
            .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
            .ifRefNullReturnFalse("value")
            .methodReturn(handleNestedValueExistsCodegen(ref("value"), codegenMethodScope, codegenClassScope));
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        Object value = map.get(propertyMap);
        if (value == null) {
            return false;
        }
        return handleNestedValueExists(value);
    }

    public Object get(EventBean obj) {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isMapExistsProperty((Map<String, Object>) eventBean.getUnderlying());
    }

    public Object getFragment(EventBean obj) {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(obj);
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        return handleNestedValueFragment(value);
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
                .ifRefNullReturnNull("value")
                .methodReturn(handleNestedValueFragmentCodegen(ref("value"), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getMapCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getMapExistsCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
