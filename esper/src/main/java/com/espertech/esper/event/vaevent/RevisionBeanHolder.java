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

/**
 * Holds revisions for property groups in an overlay strategy.
 */
public class RevisionBeanHolder {
    private long version;
    private EventBean eventBean;
    private EventPropertyGetter[] getters;

    /**
     * Ctor.
     *
     * @param version   the current version
     * @param eventBean the new event
     * @param getters   the getters
     */
    public RevisionBeanHolder(long version, EventBean eventBean, EventPropertyGetter[] getters) {
        this.version = version;
        this.eventBean = eventBean;
        this.getters = getters;
    }

    /**
     * Returns current version number.
     *
     * @return version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Returns the contributing event.
     *
     * @return event
     */
    public EventBean getEventBean() {
        return eventBean;
    }

    /**
     * Returns getters for event property access.
     *
     * @return getters
     */
    public EventPropertyGetter[] getGetters() {
        return getters;
    }

    /**
     * Returns a property value.
     *
     * @param propertyNumber number of property
     * @return value
     */
    public Object getValueForProperty(int propertyNumber) {
        return getters[propertyNumber].get(eventBean);
    }
}
