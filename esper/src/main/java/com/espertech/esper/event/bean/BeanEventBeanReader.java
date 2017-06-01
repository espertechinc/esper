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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventBeanReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Reader for fast access to all event properties for an event backed by a Java object.
 */
public class BeanEventBeanReader implements EventBeanReader {
    private BeanEventPropertyGetter[] getterArray;

    /**
     * Ctor.
     *
     * @param type the type of read
     */
    public BeanEventBeanReader(BeanEventType type) {
        String[] properties = type.getPropertyNames();
        List<BeanEventPropertyGetter> getters = new ArrayList<BeanEventPropertyGetter>();
        for (String property : properties) {
            BeanEventPropertyGetter getter = (BeanEventPropertyGetter) type.getGetterSPI(property);
            if (getter != null) {
                getters.add(getter);
            }
        }
        getterArray = getters.toArray(new BeanEventPropertyGetter[getters.size()]);
    }

    public Object[] read(EventBean theEvent) {
        Object underlying = theEvent.getUnderlying();
        Object[] values = new Object[getterArray.length];
        for (int i = 0; i < getterArray.length; i++) {
            values[i] = getterArray[i].getBeanProp(underlying);
        }
        return values;
    }
}
