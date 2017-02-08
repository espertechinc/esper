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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventType;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

public class ObjectArrayNestedEntryPropertyGetterArrayObjectArray extends ObjectArrayNestedEntryPropertyGetterBase {

    private final int index;
    private final ObjectArrayEventPropertyGetter getter;

    public ObjectArrayNestedEntryPropertyGetterArrayObjectArray(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService, int index, ObjectArrayEventPropertyGetter getter) {
        super(propertyIndex, fragmentType, eventAdapterService);
        this.index = index;
        this.getter = getter;
    }

    public Object handleNestedValue(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArray(value, index, getter);
    }

    public Object handleNestedValueFragment(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayFragment(value, index, getter, fragmentType, eventAdapterService);
    }

    public boolean handleNestedValueExists(Object value) {
        return BaseNestableEventUtil.handleNestedValueArrayWithObjectArrayExists(value, index, getter);
    }
}
