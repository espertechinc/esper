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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class MapNestedEntryPropertyGetterArrayMap extends MapNestedEntryPropertyGetterBase {

    private final int index;
    private final MapEventPropertyGetter getter;

    public MapNestedEntryPropertyGetterArrayMap(String propertyMap, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory, int index, MapEventPropertyGetter getter) {
        super(propertyMap, fragmentType, eventBeanTypedEventFactory);
        this.index = index;
        this.getter = getter;
    }

    public Object handleNestedValue(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMap(value, index, getter);
    }

    public boolean handleNestedValueExists(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMapExists(value, index, getter);
    }

    public Object handleNestedValueFragment(Object value) {
        return BaseNestableEventUtil.handleBNNestedValueArrayWithMapFragment(value, index, getter, eventBeanTypedEventFactory, fragmentType);
    }

    public CodegenExpression handleNestedValueCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMapCode(index, getter, name, codegenMethodScope, codegenClassScope, this.getClass());
    }

    public CodegenExpression handleNestedValueExistsCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return BaseNestableEventUtil.handleNestedValueArrayWithMapExistsCode(index, getter, name, codegenMethodScope, codegenClassScope, this.getClass());
    }

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return BaseNestableEventUtil.handleBNNestedValueArrayWithMapFragmentCode(index, getter, name, codegenMethodScope, codegenClassScope, eventBeanTypedEventFactory, fragmentType, this.getClass());
    }
}
