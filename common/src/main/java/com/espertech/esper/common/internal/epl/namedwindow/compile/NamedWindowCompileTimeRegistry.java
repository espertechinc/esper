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
package com.espertech.esper.common.internal.epl.namedwindow.compile;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.HashMap;
import java.util.Map;

public class NamedWindowCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<String, NamedWindowMetaData> namedWindows = new HashMap<>();

    public void newNamedWindow(NamedWindowMetaData detail) {
        EventType eventType = detail.getEventType();
        if (!eventType.getMetadata().getAccessModifier().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for named window");
        }
        String namedWindowName = detail.getEventType().getName();
        NamedWindowMetaData existing = namedWindows.get(namedWindowName);
        if (existing != null) {
            throw new IllegalStateException("Duplicate named window definition encountered");
        }
        namedWindows.put(namedWindowName, detail);
    }

    public boolean isNamedWindow(String namedWindowName) {
        return namedWindows.containsKey(namedWindowName);
    }

    public EventType getEventType(String namedWindowName) {
        return namedWindows.get(namedWindowName).getEventType();
    }

    public Map<String, NamedWindowMetaData> getNamedWindows() {
        return namedWindows;
    }
}
