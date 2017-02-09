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
package com.espertech.esper.collection;

import com.espertech.esper.client.EventBean;

/**
 * Interface that transforms one event into another event, for use with {@link TransformEventIterator}.
 */
public interface TransformEventMethod {
    /**
     * Transform event returning the transformed event.
     *
     * @param theEvent to transform
     * @return transformed event
     */
    public EventBean transform(EventBean theEvent);
}
