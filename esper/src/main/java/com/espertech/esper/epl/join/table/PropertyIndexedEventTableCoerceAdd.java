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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.util.SimpleNumberCoercer;

/**
 * Index that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 * <p>
 * Performs coercion of the index keys before storing the keys.
 * <p>
 * Takes a list of property names as parameter. Doesn't care which event type the events have as long as the properties
 * exist. If the same event is added twice, the class throws an exception on add.
 */
public class PropertyIndexedEventTableCoerceAdd extends PropertyIndexedEventTableUnadorned {
    private final SimpleNumberCoercer[] coercers;
    protected final Class[] coercionTypes;

    public PropertyIndexedEventTableCoerceAdd(EventPropertyGetter[] propertyGetters, EventTableOrganization organization, SimpleNumberCoercer[] coercers, Class[] coercionTypes) {
        super(propertyGetters, organization);
        this.coercers = coercers;
        this.coercionTypes = coercionTypes;
    }

    @Override
    protected MultiKeyUntyped getMultiKey(EventBean theEvent) {
        Object[] keyValues = new Object[propertyGetters.length];
        for (int i = 0; i < propertyGetters.length; i++) {
            Object value = propertyGetters[i].get(theEvent);
            if ((value != null) && (!value.getClass().equals(coercionTypes[i]))) {
                if (value instanceof Number) {
                    value = coercers[i].coerceBoxed((Number) value);
                }
            }
            keyValues[i] = value;
        }
        return new MultiKeyUntyped(keyValues);
    }
}
