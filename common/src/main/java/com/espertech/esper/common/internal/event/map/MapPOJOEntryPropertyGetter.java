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
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.getter.BaseNativePropertyGetter;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class MapPOJOEntryPropertyGetter extends BaseNativePropertyGetter implements MapEventPropertyGetter {
    private final String propertyMap;
    private final BeanEventPropertyGetter mapEntryGetter;

    public MapPOJOEntryPropertyGetter(String propertyMap, BeanEventPropertyGetter mapEntryGetter, EventBeanTypedEventFactory eventBeanTypedEventFactory, EPTypeClass returnType, BeanEventTypeFactory beanEventTypeFactory) {
        super(eventBeanTypedEventFactory, beanEventTypeFactory, returnType);
        this.propertyMap = propertyMap;
        this.mapEntryGetter = mapEntryGetter;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = map.get(propertyMap);
        if (value == null) {
            return null;
        }
        // Object within the map
        if (value instanceof EventBean) {
            return mapEntryGetter.get((EventBean) value);
        }
        return mapEntryGetter.getBeanProp(value);
    }

    private CodegenMethod getMapCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(EPTypePremade.MAP.getEPType(), "map").getBlock()
            .declareVar(EPTypePremade.OBJECT.getEPType(), "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
            .ifRefNullReturnNull("value")
            .ifInstanceOf("value", EventBean.EPTYPE)
            .blockReturn(mapEntryGetter.eventBeanGetCodegen(castRef(EventBean.EPTYPE, "value"), codegenMethodScope, codegenClassScope))
            .methodReturn(mapEntryGetter.underlyingGetCodegen(castRef(mapEntryGetter.getTargetType(), "value"), codegenMethodScope, codegenClassScope));
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

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(EPTypePremade.MAP.getEPType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getMapCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public EPTypeClass getTargetType() {
        return EPTypePremade.MAP.getEPType();
    }

    @Override
    public EPTypeClass getBeanPropType() {
        return EPTypePremade.OBJECT.getEPType();
    }
}
