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
package com.espertech.esper.supportregression.virtualdw;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.VirtualDataWindow;
import com.espertech.esper.client.hook.VirtualDataWindowEvent;
import com.espertech.esper.client.hook.VirtualDataWindowLookup;
import com.espertech.esper.client.hook.VirtualDataWindowLookupContext;

import java.util.Iterator;

public class SupportVirtualDWInvalid implements VirtualDataWindow {

    public VirtualDataWindowLookup getLookup(VirtualDataWindowLookupContext desc) {
        return null;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
    }

    public void destroy() {
    }

    public Iterator<EventBean> iterator() {
        return null;
    }

    public void handleEvent(VirtualDataWindowEvent theEvent) {
    }
}
