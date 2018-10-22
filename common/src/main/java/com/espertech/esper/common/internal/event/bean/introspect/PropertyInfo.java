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
package com.espertech.esper.common.internal.event.bean.introspect;

import com.espertech.esper.common.internal.event.bean.core.PropertyStem;

public class PropertyInfo {
    private Class clazz;
    private EventPropertyGetterSPIFactory getterFactory;
    private PropertyStem stem;

    /**
     * Ctor.
     *
     * @param clazz         is the class
     * @param getterFactory is the getter
     * @param stem          is the property info
     */
    public PropertyInfo(Class clazz, EventPropertyGetterSPIFactory getterFactory, PropertyStem stem) {
        this.clazz = clazz;
        this.getterFactory = getterFactory;
        this.stem = stem;
    }

    /**
     * Returns the return type.
     *
     * @return return type
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the getter.
     *
     * @return getter
     */
    public EventPropertyGetterSPIFactory getGetterFactory() {
        return getterFactory;
    }

    /**
     * Returns the property info.
     *
     * @return property info
     */
    public PropertyStem getDescriptor() {
        return stem;
    }
}
