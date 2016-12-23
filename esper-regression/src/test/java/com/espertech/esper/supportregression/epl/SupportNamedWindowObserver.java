/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.supportregression.epl;

import com.espertech.esper.epl.named.NamedWindowLifecycleObserver;
import com.espertech.esper.epl.named.NamedWindowLifecycleEvent;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;

public class SupportNamedWindowObserver implements NamedWindowLifecycleObserver
{
    private List<NamedWindowLifecycleEvent> events = new ArrayList<NamedWindowLifecycleEvent>();

    public void observe(NamedWindowLifecycleEvent theEvent)
    {
        events.add(theEvent);
    }

    public List<NamedWindowLifecycleEvent> getEvents()
    {
        return events;
    }

    public NamedWindowLifecycleEvent getFirstAndReset()
    {
        Assert.assertEquals(1, events.size());
        NamedWindowLifecycleEvent theEvent = events.get(0);
        events.clear();
        return theEvent;
    }
}
