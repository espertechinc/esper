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

import java.util.Arrays;
import java.util.Map;

/**
 * For use with building groups of event properties to reduce overhead in maintaining versions.
 */
public class PropertyGroupDesc {

    private final int groupNum;
    private final Map<EventType, String> types;
    private final String[] properties;

    /**
     * Ctor.
     *
     * @param groupNum    the group number
     * @param nameTypeSet the event types and their names whose totality of properties fully falls within this group.
     * @param properties  is the properties in the group
     */
    public PropertyGroupDesc(int groupNum, Map<EventType, String> nameTypeSet, String[] properties) {
        this.groupNum = groupNum;
        this.types = nameTypeSet;
        this.properties = properties;
    }

    /**
     * Returns the group number.
     *
     * @return group number
     */
    public int getGroupNum() {
        return groupNum;
    }

    /**
     * Returns the types.
     *
     * @return types
     */
    public Map<EventType, String> getTypes() {
        return types;
    }

    /**
     * Returns the properties.
     *
     * @return properties
     */
    public String[] getProperties() {
        return properties;
    }

    public String toString() {
        return "groupNum=" + groupNum +
                " properties=" + Arrays.toString(properties) +
                " nameTypes=" + types.toString();
    }
}
