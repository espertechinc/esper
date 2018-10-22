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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeStateListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class SupportRuntimeStateListener implements EPRuntimeStateListener {
    private List<EPRuntime> destroyedEvents = new ArrayList<>();
    private List<EPRuntime> initializedEvents = new ArrayList<>();

    public void onEPRuntimeDestroyRequested(EPRuntime runtime) {
        destroyedEvents.add(runtime);
    }

    public void onEPRuntimeInitialized(EPRuntime runtime) {
        initializedEvents.add(runtime);
    }

    public EPRuntime assertOneGetAndResetDestroyedEvents() {
        Assert.assertEquals(1, destroyedEvents.size());
        EPRuntime item = destroyedEvents.get(0);
        destroyedEvents.clear();
        return item;
    }

    public EPRuntime assertOneGetAndResetInitializedEvents() {
        Assert.assertEquals(1, initializedEvents.size());
        EPRuntime item = initializedEvents.get(0);
        initializedEvents.clear();
        return item;
    }

    public List<EPRuntime> getDestroyedEvents() {
        return destroyedEvents;
    }

    public List<EPRuntime> getInitializedEvents() {
        return initializedEvents;
    }
}
