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
package com.espertech.esper.common.internal.event.json.parser.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class JsonDelegateEventObjectArray extends JsonDelegateBase {
    private final JsonDelegateFactory factory;
    private final Class componentType;
    private final ArrayList events = new ArrayList(4);

    public JsonDelegateEventObjectArray(JsonHandlerDelegator baseHandler, JsonDelegateBase parent, JsonDelegateFactory factory, Class componentType) {
        super(baseHandler, parent);
        this.factory = factory;
        this.componentType = componentType;
    }

    public JsonDelegateBase startObject(String name) {
        return factory.make(baseHandler, this);
    }

    public JsonDelegateBase startArray(String name) {
        return null;
    }

    public boolean endObjectValue(String name) {
        return false;
    }

    @Override
    public void endArrayValue(String name) {
        events.add(objectValue);
    }

    public Object getResult() {
        return collectionToTypedArray(events, componentType);
    }

    public static Object collectionToTypedArray(Collection events, Class componentType) {
        Object array = Array.newInstance(componentType, events.size());
        int count = 0;
        Iterator<Object> it = events.iterator();
        while (it.hasNext()) {
            Object event = it.next();
            Array.set(array, count++, event);
        }
        return array;
    }
}
