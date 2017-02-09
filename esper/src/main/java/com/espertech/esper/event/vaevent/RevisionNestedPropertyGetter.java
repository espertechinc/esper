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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.event.EventAdapterService;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class RevisionNestedPropertyGetter implements EventPropertyGetter {
    private final EventPropertyGetter revisionGetter;
    private final EventPropertyGetter nestedGetter;
    private final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param revisionGetter      getter for revision value
     * @param nestedGetter        getter to apply to revision value
     * @param eventAdapterService for handling object types
     */
    public RevisionNestedPropertyGetter(EventPropertyGetter revisionGetter, EventPropertyGetter nestedGetter, EventAdapterService eventAdapterService) {
        this.revisionGetter = revisionGetter;
        this.eventAdapterService = eventAdapterService;
        this.nestedGetter = nestedGetter;
    }

    public Object get(EventBean obj) {
        Object result = revisionGetter.get(obj);
        if (result == null) {
            return result;
        }

        // Object within the map
        EventBean theEvent = eventAdapterService.adapterForBean(result);
        return nestedGetter.get(theEvent);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean eventBean) {
        return null; // no fragments provided by revision events
    }
}
