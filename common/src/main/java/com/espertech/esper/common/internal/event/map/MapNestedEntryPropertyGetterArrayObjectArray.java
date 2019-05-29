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
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

public class MapNestedEntryPropertyGetterArrayObjectArray extends MapNestedEntryPropertyGetterBase {

    private final int index;
    private final ObjectArrayEventPropertyGetter getter;

    public MapNestedEntryPropertyGetterArrayObjectArray(String propertyMap, EventType fragmentType, EventBeanTypedEventFactory eventBeanTypedEventFactory, int index, ObjectArrayEventPropertyGetter getter) {
        super(propertyMap, fragmentType, eventBeanTypedEventFactory);
        this.index = index;
        this.getter = getter;
    }

    public Object handleNestedValue(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArray(value, index, getter);
    }

    public boolean handleNestedValueExists(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayExists(value, index, getter);
    }

    public Object handleNestedValueFragment(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayFragment(value, index, getter, fragmentType, eventBeanTypedEventFactory);
    }

    public CodegenExpression handleNestedValueCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayCodegen(index, getter, name, codegenMethodScope, codegenClassScope, this.getClass());
    }

    public CodegenExpression handleNestedValueExistsCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayExistsCodegen(index, getter, name, codegenMethodScope, codegenClassScope, this.getClass());
    }

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayFragmentCodegen(index, getter, name, codegenMethodScope, codegenClassScope, this.getClass());
    }
}
