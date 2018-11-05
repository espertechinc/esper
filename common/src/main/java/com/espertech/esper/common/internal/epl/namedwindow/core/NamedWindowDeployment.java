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

import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import java.util.HashMap;
import java.util.Map;

public class NamedWindowDeployment {
    private final Map<String, NamedWindow> namedWindows = new HashMap<>(4);

    public void add(String windowName, NamedWindowMetaData metadata, EPStatementInitServices services) {
        NamedWindow existing = namedWindows.get(windowName);
        if (existing != null) {
            throw new IllegalStateException("Named window processor already found for name '" + windowName + "'");
        }
        NamedWindow namedWindow = services.getNamedWindowFactoryService().createNamedWindow(metadata, services);
        namedWindows.put(windowName, namedWindow);
    }

    public NamedWindow getProcessor(String namedWindowName) {
        return namedWindows.get(namedWindowName);
    }

    public void remove(String tableName) {
        namedWindows.remove(tableName);
    }

    public boolean isEmpty() {
        return namedWindows.isEmpty();
    }

    public Map<String, NamedWindow> getNamedWindows() {
        return namedWindows;
    }
}
