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
 * Interface for composite events in which a property is itself an event.
 * <p>
 * For use with patterns in which pattern tags are properties in a result event and property values
 * are the event itself that is matching in a pattern.
 */
public interface TaggedCompositeEventBean {
    /**
     * Returns the event for the tag.
     *
     * @param property is the tag name
     * @return event
     */
    public EventBean getEventBean(String property);
}
