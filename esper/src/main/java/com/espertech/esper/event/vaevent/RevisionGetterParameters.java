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

import com.espertech.esper.client.EventPropertyGetter;

/**
 * Getter parameters for revision events.
 */
public class RevisionGetterParameters {
    private final int propertyNumber;
    private final EventPropertyGetter baseGetter;
    private final int[] propertyGroups;

    /**
     * Ctor.
     *
     * @param propertyName   the property this gets
     * @param propertyNumber the property number
     * @param fullGetter     the getter of the base event to use, if any
     * @param authoritySets  is the group numbers that the getter may access to obtain a property value
     */
    public RevisionGetterParameters(String propertyName, int propertyNumber, EventPropertyGetter fullGetter, int[] authoritySets) {
        this.propertyNumber = propertyNumber;
        this.baseGetter = fullGetter;
        this.propertyGroups = authoritySets;
    }

    /**
     * Returns the group numbers to look for updated properties comparing version numbers.
     *
     * @return groups
     */
    public int[] getPropertyGroups() {
        return propertyGroups;
    }

    /**
     * Returns the property number.
     *
     * @return property number
     */
    public int getPropertyNumber() {
        return propertyNumber;
    }

    /**
     * Returns the getter for the base event type.
     *
     * @return base getter
     */
    public EventPropertyGetter getBaseGetter() {
        return baseGetter;
    }
}
