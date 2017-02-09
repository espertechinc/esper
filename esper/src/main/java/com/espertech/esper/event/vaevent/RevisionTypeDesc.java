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
 * Per-event-type descriptor for fast access to getters for key values and changes properties.
 */
public class RevisionTypeDesc {
    private final EventPropertyGetter[] keyPropertyGetters;
    private final EventPropertyGetter[] changesetPropertyGetters;
    private PropertyGroupDesc group;
    private int[] changesetPropertyIndex;

    /**
     * Ctor.
     *
     * @param keyPropertyGetters       key getters
     * @param changesetPropertyGetters property getters
     * @param group                    group this belongs to
     */
    public RevisionTypeDesc(EventPropertyGetter[] keyPropertyGetters, EventPropertyGetter[] changesetPropertyGetters, PropertyGroupDesc group) {
        this.keyPropertyGetters = keyPropertyGetters;
        this.changesetPropertyGetters = changesetPropertyGetters;
        this.group = group;
    }

    /**
     * Ctor.
     *
     * @param keyPropertyGetters       key getters
     * @param changesetPropertyGetters property getters
     * @param changesetPropertyIndex   indexes of properties contributed
     */
    public RevisionTypeDesc(EventPropertyGetter[] keyPropertyGetters, EventPropertyGetter[] changesetPropertyGetters, int[] changesetPropertyIndex) {
        this.keyPropertyGetters = keyPropertyGetters;
        this.changesetPropertyGetters = changesetPropertyGetters;
        this.changesetPropertyIndex = changesetPropertyIndex;
    }

    /**
     * Returns key getters.
     *
     * @return getters
     */
    public EventPropertyGetter[] getKeyPropertyGetters() {
        return keyPropertyGetters;
    }

    /**
     * Returns property getters.
     *
     * @return getters
     */
    public EventPropertyGetter[] getChangesetPropertyGetters() {
        return changesetPropertyGetters;
    }

    /**
     * Returns group, or null if not using property groups.
     *
     * @return group
     */
    public PropertyGroupDesc getGroup() {
        return group;
    }

    /**
     * Returns indexes of properties contributed, or null if not using indexes.
     *
     * @return indexes
     */
    public int[] getChangesetPropertyIndex() {
        return changesetPropertyIndex;
    }
}
