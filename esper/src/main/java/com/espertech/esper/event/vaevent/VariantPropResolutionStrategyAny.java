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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventPropertyGetterSPI;

/**
 * A property resolution strategy that allows any type, wherein all properties are Object type.
 */
public class VariantPropResolutionStrategyAny implements VariantPropResolutionStrategy {
    private int currentPropertyNumber;
    private VariantPropertyGetterCache propertyGetterCache;

    /**
     * Ctor.
     *
     * @param variantSpec specified the preconfigured types
     */
    public VariantPropResolutionStrategyAny(VariantSpec variantSpec) {
        propertyGetterCache = new VariantPropertyGetterCache(variantSpec.getEventTypes());
    }

    public VariantPropertyDesc resolveProperty(String propertyName, EventType[] variants) {
        // property numbers should start at zero since the serve as array index
        final int assignedPropertyNumber = currentPropertyNumber;
        currentPropertyNumber++;
        propertyGetterCache.addGetters(assignedPropertyNumber, propertyName);
        EventPropertyGetterSPI getter = new VariantEventPropertyGetterAny(propertyGetterCache, assignedPropertyNumber);
        return new VariantPropertyDesc(Object.class, getter, true);
    }
}
