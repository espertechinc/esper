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
package com.espertech.esper.core.context.activator;

import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.filter.FilterServiceEntry;
import com.espertech.esper.util.StopCallback;

public class ViewableActivatorFilterProxyStopCallback implements StopCallback {

    private final ViewableActivatorFilterProxy parent;
    private EPStatementHandleCallback filterHandle;
    private FilterServiceEntry filterServiceEntry;

    public ViewableActivatorFilterProxyStopCallback(ViewableActivatorFilterProxy parent, EPStatementHandleCallback filterHandle, FilterServiceEntry filterServiceEntry) {
        this.parent = parent;
        this.filterHandle = filterHandle;
        this.filterServiceEntry = filterServiceEntry;
    }

    public synchronized void stop() {
        if (filterHandle != null) {
            parent.getServices().getFilterService().remove(filterHandle, filterServiceEntry);
        }
        filterHandle = null;
    }
}
