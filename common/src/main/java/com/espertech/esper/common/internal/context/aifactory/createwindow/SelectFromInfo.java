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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.client.EventType;

class SelectFromInfo {
    private final EventType eventType;
    private final String typeName;

    public SelectFromInfo(EventType eventType, String typeName) {
        this.eventType = eventType;
        this.typeName = typeName;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getTypeName() {
        return typeName;
    }
}
