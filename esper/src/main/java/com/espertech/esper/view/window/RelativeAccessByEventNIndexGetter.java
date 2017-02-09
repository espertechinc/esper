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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;

/**
 * Provides random-access into window contents by event and index as a combination.
 */
public interface RelativeAccessByEventNIndexGetter {
    /**
     * Returns the access into window contents given an event.
     *
     * @param theEvent to which the method returns relative access from
     * @return buffer
     */
    public RelativeAccessByEventNIndex getAccessor(EventBean theEvent);
}
