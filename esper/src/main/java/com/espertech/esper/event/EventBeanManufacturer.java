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
package com.espertech.esper.event;

import com.espertech.esper.client.EventBean;

/**
 * Factory for creating an event bean instance by writing property values to an underlying object.
 */
public interface EventBeanManufacturer {
    /**
     * Make an event object populating property values.
     *
     * @param properties values to populate
     * @return event object
     */
    public EventBean make(Object[] properties);

    public Object makeUnderlying(Object[] properties);
}
