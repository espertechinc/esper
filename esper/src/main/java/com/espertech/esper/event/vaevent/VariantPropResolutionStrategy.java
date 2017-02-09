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

/**
 * Strategy for resolving a property against any of the variant types.
 */
public interface VariantPropResolutionStrategy {
    /**
     * Resolve the property for each of the types.
     *
     * @param propertyName to resolve
     * @param variants     the variants to resolve the property for
     * @return property descriptor
     */
    public VariantPropertyDesc resolveProperty(String propertyName, EventType[] variants);
}
