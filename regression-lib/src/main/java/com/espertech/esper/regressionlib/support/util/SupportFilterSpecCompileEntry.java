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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SupportFilterSpecCompileEntry  {
    private final EventType eventType;
    private final List<FilterSpecParamForge>[] forges;

    public SupportFilterSpecCompileEntry(EventType eventType, List<FilterSpecParamForge>[] forges) {
        this.eventType = eventType;
        this.forges = forges;
    }

    public EventType getEventType() {
        return eventType;
    }

    public List<FilterSpecParamForge>[] getForges() {
        return forges;
    }

    public FilterSpecParamForge getAssertSingle(String typeName) {
        assertEquals(typeName, eventType.getName());
        assertEquals(1, forges.length);
        List<FilterSpecParamForge> forgeList = forges[0];
        assertEquals(1, forgeList.size());
        return forgeList.get(0);
    }
}
