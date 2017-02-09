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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.util.JavaClassHelper;

public class PropertySortedEventTableCoerced extends PropertySortedEventTableImpl {
    private Class coercionType;

    public PropertySortedEventTableCoerced(EventPropertyGetter propertyGetter, EventTableOrganization organization, Class coercionType) {
        super(propertyGetter, organization);
        this.coercionType = coercionType;
    }

    @Override
    protected Object coerce(Object value) {
        if (value != null && !value.getClass().equals(coercionType)) {
            if (value instanceof Number) {
                return JavaClassHelper.coerceBoxed((Number) value, coercionType);
            }
        }
        return value;
    }

    public String toString() {
        return "PropertySortedEventTableCoerced" +
                " streamNum=" + organization.getStreamNum() +
                " propertyGetter=" + propertyGetter +
                " coercionType=" + coercionType;
    }

    public Class getProviderClass() {
        return PropertySortedEventTableCoerced.class;
    }
}
