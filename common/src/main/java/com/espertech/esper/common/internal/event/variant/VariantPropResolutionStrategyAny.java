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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

/**
 * A property resolution strategy that allows any type, wherein all properties are Object type.
 */
public class VariantPropResolutionStrategyAny implements VariantPropResolutionStrategy {
    private final VariantEventType variantEventType;

    public VariantPropResolutionStrategyAny(VariantEventType variantEventType) {
        this.variantEventType = variantEventType;
    }

    public VariantPropertyDesc resolveProperty(String propertyName, EventType[] variants) {
        // property numbers should start at zero since the serve as array index
        VariantPropertyGetterCache propertyGetterCache = variantEventType.getVariantPropertyGetterCache();
        propertyGetterCache.addGetters(propertyName);
        EventPropertyGetterSPI getter = new VariantEventPropertyGetterAny(variantEventType, propertyName);
        return new VariantPropertyDesc(Object.class, getter, true);
    }
}
