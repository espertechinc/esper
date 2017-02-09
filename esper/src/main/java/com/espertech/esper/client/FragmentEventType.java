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
package com.espertech.esper.client;

/**
 * Provides an event type for a property of an event.
 * <p>
 * A fragment is a property value that is itself an event, or that can be represented as an event.
 * Thereby a fragment comes with event type metadata and means of querying the fragment's properties.
 * <p>
 * A array or collection of property values that is an array of events or that can be represented as an array of events
 * has the indexed flag set.
 * <p>
 * A map of property values that is an map of events or that can be represented as a map of events
 * has the mapped flag set.
 */
public class FragmentEventType {
    private EventType fragmentType;
    private boolean isIndexed;
    private boolean isNative;

    /**
     * Ctor.
     *
     * @param fragmentType the event type for a property value for an event.
     * @param indexed      true to indicate that property value is an array of events
     * @param isNative     true
     */
    public FragmentEventType(EventType fragmentType, boolean indexed, boolean isNative) {
        this.fragmentType = fragmentType;
        this.isIndexed = indexed;
        this.isNative = isNative;
    }

    /**
     * Returns true if the fragment type is an array.
     * <p>
     * If a property value is an array and thereby a fragment array, this flag is set to true.
     *
     * @return indicator if array fragment
     */
    public boolean isIndexed() {
        return isIndexed;
    }

    /**
     * Returns the type of the fragment.
     *
     * @return fragment type
     */
    public EventType getFragmentType() {
        return fragmentType;
    }

    /**
     * Returns true if the fragment is a native representation, i.e. a Java class.
     *
     * @return indicator whether fragment is a Class.
     */
    public boolean isNative() {
        return isNative;
    }
}
