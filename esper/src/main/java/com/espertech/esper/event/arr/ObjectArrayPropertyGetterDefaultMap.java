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

/**
 * Getter for map entry.
 */
public class ObjectArrayPropertyGetterDefaultMap extends ObjectArrayPropertyGetterDefaultBase {
    public ObjectArrayPropertyGetterDefaultMap(int propertyIndex, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        super(propertyIndex, fragmentEventType, eventAdapterService);
    }

    protected Object handleCreateFragment(Object value) {
        return BaseNestableEventUtil.handleCreateFragmentMap(value, fragmentEventType, eventAdapterService);
    }
}
