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
package com.espertech.esper.regressionlib.support.extend.vdw;

import com.espertech.esper.common.client.hook.vdw.VirtualDataWindow;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowContext;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactory;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryContext;

import java.util.ArrayList;
import java.util.List;

public class SupportVirtualDWFactory implements VirtualDataWindowFactory {
    private VirtualDataWindowFactoryContext initializeContext;
    private static List<SupportVirtualDW> windows = new ArrayList<SupportVirtualDW>();
    private static boolean destroyed;

    public static boolean isDestroyed() {
        return destroyed;
    }

    public static void setDestroyed(boolean destroyed) {
        SupportVirtualDWFactory.destroyed = destroyed;
    }

    public static List<SupportVirtualDW> getWindows() {
        return windows;
    }

    public void initialize(VirtualDataWindowFactoryContext initializeContext) {
        this.initializeContext = initializeContext;
    }

    public VirtualDataWindow create(VirtualDataWindowContext context) {
        SupportVirtualDW vdw = new SupportVirtualDW(context);
        windows.add(vdw);
        return vdw;
    }

    public VirtualDataWindowFactoryContext getInitializeContext() {
        return initializeContext;
    }

    public void destroy() {
        destroyed = true;
    }
}
