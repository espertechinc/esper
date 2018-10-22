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
package com.espertech.esper.common.internal.epl.namedwindow.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

/**
 * The root window in a named window plays multiple roles: It holds the indexes for deleting rows, if any on-delete statement
 * requires such indexes. Such indexes are updated when events arrive, or remove from when a data window
 * or on-delete statement expires events. The view keeps track of on-delete statements their indexes used.
 */
public class NamedWindowRootView {
    private final NamedWindowMetaData namedWindowMetaData;

    public NamedWindowRootView(NamedWindowMetaData vo) {
        this.namedWindowMetaData = vo;
    }

    public boolean isChildBatching() {
        return namedWindowMetaData.isChildBatching();
    }

    public EventType getEventType() {
        return namedWindowMetaData.getEventType();
    }

    public String getContextName() {
        return namedWindowMetaData.getContextName();
    }

    public boolean isVirtualDataWindow() {
        return namedWindowMetaData.isVirtualDataWindow();
    }
}
