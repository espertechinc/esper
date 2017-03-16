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
package com.espertech.esper.example.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.*;

import java.util.Collections;
import java.util.Iterator;

public class SampleVirtualDataWindow implements VirtualDataWindow {

    private final VirtualDataWindowContext context;

    public SampleVirtualDataWindow(VirtualDataWindowContext context) {
        this.context = context;
    }

    public VirtualDataWindowLookup getLookup(VirtualDataWindowLookupContext desc) {

        // Place any code that interrogates the hash-index and btree-index fields here.

        // Return the index representation.
        return new SampleVirtualDataWindowLookup(context);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {

        // This sample simply posts into the insert and remove stream what is received.
        context.getOutputStream().update(newData, oldData);
    }

    public void destroy() {
        // Called when the named window is stopped or destroyed, for each context partition.
        // This sample does not need to clean up resources.
    }

    public void handleEvent(VirtualDataWindowEvent theEvent) {
    }

    public Iterator<EventBean> iterator() {
        return Collections.<EventBean>emptyList().iterator();
    }
}
