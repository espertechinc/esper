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
package com.espertech.esper.common.internal.epl.namedwindow.path;

import java.util.Map;

public class NamedWindowCollectorImpl implements NamedWindowCollector {
    private final Map<String, NamedWindowMetaData> moduleNamedWindows;

    public NamedWindowCollectorImpl(Map<String, NamedWindowMetaData> moduleNamedWindows) {
        this.moduleNamedWindows = moduleNamedWindows;
    }

    public void registerNamedWindow(String namedWindowName, NamedWindowMetaData namedWindow) {
        moduleNamedWindows.put(namedWindowName, namedWindow);
    }
}
